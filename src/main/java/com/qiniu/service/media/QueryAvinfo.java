package com.qiniu.service.media;

import com.qiniu.persistence.FileMap;
import com.qiniu.common.QiniuException;
import com.qiniu.service.interfaces.ILineProcess;
import com.qiniu.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryAvinfo implements ILineProcess<Map<String, String>>, Cloneable {

    private String domain;
    private String protocol;
    private String urlIndex;
    private Auth auth;
    private MediaManager mediaManager;
    private String processName;
    private int retryCount;
    protected String resultPath;
    private int resultIndex;
    private FileMap fileMap;

    public QueryAvinfo(String domain, String protocol, String urlIndex, Auth auth, String resultPath, int resultIndex)
            throws IOException {
        this.processName = "avinfo";
        if (domain == null || "".equals(domain)) this.domain = null;
        else {
            RequestUtils.checkHost(domain);
            this.domain = domain;
        }
        this.protocol = protocol;
        this.urlIndex = urlIndex;
        this.auth = auth;
        this.mediaManager = new MediaManager(protocol, auth);
        this.resultPath = resultPath;
        this.resultIndex = resultIndex;
        this.fileMap = new FileMap();
        this.fileMap.initWriter(resultPath, processName, resultIndex);
    }

    public QueryAvinfo(String domain, String protocol, String urlIndex, Auth auth, String resultPath) throws IOException {
        this(domain, protocol, urlIndex, auth, resultPath, 0);
    }

    public QueryAvinfo clone() throws CloneNotSupportedException {
        QueryAvinfo queryAvinfo = (QueryAvinfo)super.clone();
        queryAvinfo.mediaManager = new MediaManager(protocol, auth);
        queryAvinfo.fileMap = new FileMap();
        try {
            queryAvinfo.fileMap.initWriter(resultPath, processName, resultIndex++);
        } catch (IOException e) {
            throw new CloneNotSupportedException("init writer failed.");
        }
        return queryAvinfo;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getProcessName() {
        return this.processName;
    }

    public String singleWithRetry(String url, int retryCount) throws QiniuException {

        String avinfo = null;
        try {
            avinfo = mediaManager.getAvinfoBody(url);
        } catch (QiniuException e1) {
            HttpResponseUtils.checkRetryCount(e1, retryCount);
            while (retryCount > 0) {
                try {
                    avinfo = mediaManager.getAvinfoBody(url);
                    retryCount = 0;
                } catch (QiniuException e2) {
                    retryCount = HttpResponseUtils.getNextRetryCount(e2, retryCount);
                }
            }
        }

        return avinfo;
    }

    public void processLine(List<Map<String, String>> lineList) throws QiniuException {
        List<String> urlList;
        if (domain != null) {
            List<String> keyList = lineList.stream().map(line -> line.get("key"))
                    .filter(pid -> pid != null && !"".equals(pid)).collect(Collectors.toList());
            if (keyList.size() == 0) throw new QiniuException(null, "there is no key in line.");
            urlList = keyList.stream().map(key -> protocol + "://" + domain + "/" + key).collect(Collectors.toList());
        } else {
            urlList = lineList.stream().map(line -> line.get(urlIndex)).collect(Collectors.toList());
        }
        List<String> resultList = new ArrayList<>();
        for (String url : urlList) {
            try {
                String avinfo = singleWithRetry(url, retryCount);
                if (avinfo != null) resultList.add(url + "\t" + avinfo);
                else throw new QiniuException(null, "empty avinfo");
            } catch (QiniuException e) {
                HttpResponseUtils.processException(e, fileMap, url);
            }
        }
        if (resultList.size() > 0) fileMap.writeSuccess(String.join("\n", resultList));
    }

    public void closeResource() {
        fileMap.closeWriter();
    }
}
