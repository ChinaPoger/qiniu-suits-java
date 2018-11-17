package com.qiniu.service.oss;

import com.qiniu.common.FileReaderAndWriterMap;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.service.interfaces.IOssFileProcess;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.HttpResponseUtils;
import com.qiniu.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CopyFile extends OperationBase implements IOssFileProcess, Cloneable {

    private String resultFileDir;
    private String processName;
    private FileReaderAndWriterMap fileReaderAndWriterMap = new FileReaderAndWriterMap();
    private String srcBucket;
    private String tarBucket;
    private boolean keepKey;
    private String keyPrefix;

    public CopyFile(Auth auth, Configuration configuration, String srcBucket, String tarBucket,
                    boolean keepKey, String keyPrefix, String resultFileDir, String processName,
                    int resultFileIndex) throws IOException {
        super(auth, configuration);
        this.resultFileDir = resultFileDir;
        this.processName = processName;
        this.fileReaderAndWriterMap.initWriter(resultFileDir, processName, resultFileIndex);
        this.srcBucket = srcBucket;
        this.tarBucket = tarBucket;
        this.keepKey = keepKey;
        this.keyPrefix = StringUtils.isNullOrEmpty(keyPrefix) ? "" : keyPrefix;
    }

    public CopyFile(Auth auth, Configuration configuration, String srcBucket, String tarBucket,
                    boolean keepKey, String keyPrefix, String resultFileDir, String processName)
            throws IOException {
        this(auth, configuration, srcBucket, tarBucket, keepKey, keyPrefix, resultFileDir, processName, 0);
    }

    public CopyFile getNewInstance(int resultFileIndex) throws CloneNotSupportedException {
        CopyFile copyFile = (CopyFile)super.clone();
        copyFile.fileReaderAndWriterMap = new FileReaderAndWriterMap();
        try {
            copyFile.fileReaderAndWriterMap.initWriter(resultFileDir, processName, resultFileIndex);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CloneNotSupportedException();
        }
        return copyFile;
    }

    public String getProcessName() {
        return this.processName;
    }

    public String run(String fromBucket, String srcKey, String toBucket, String tarKey, String keyPrefix, boolean force,
                      int retryCount) throws QiniuException {

        Response response = copyWithRetry(fromBucket, srcKey, toBucket, tarKey, keyPrefix, force, retryCount);
        if (response == null) return null;
        String responseBody = response.bodyString();
        int statusCode = response.statusCode;
        String reqId = response.reqId;
        response.close();

        return statusCode + "\t" + reqId + "\t" + responseBody;
    }

    public Response copyWithRetry(String fromBucket, String srcKey, String toBucket, String prefix, String tarKey,
                                  boolean force, int retryCount) throws QiniuException {

        Response response = null;
        try {
            response = bucketManager.copy(fromBucket, srcKey, toBucket, prefix + tarKey, force);
        } catch (QiniuException e1) {
            HttpResponseUtils.checkRetryCount(e1, retryCount);
            while (retryCount > 0) {
                try {
                    response = bucketManager.copy(fromBucket, srcKey, toBucket, prefix + tarKey, false);
                    retryCount = 0;
                } catch (QiniuException e2) {
                    retryCount = HttpResponseUtils.getNextRetryCount(e2, retryCount);
                }
            }
        }

        return response;
    }

    synchronized public String batchRun(String fromBucket, String toBucket, String keyPrefix, boolean keepKey,
                                        List<String> keys, int retryCount) throws QiniuException {

        if (keepKey) {
            keys.forEach(fileKey -> batchOperations.addCopyOp(fromBucket, fileKey, toBucket,
                    keyPrefix + fileKey));
        } else {
            keys.forEach(fileKey -> batchOperations.addCopyOp(fromBucket, fileKey, toBucket, null));
        }
        Response response = batchWithRetry(retryCount);
        if (response == null) return null;
        String responseBody = response.bodyString();
        int statusCode = response.statusCode;
        String reqId = response.reqId;
        batchOperations.clearOps();
        return statusCode + "\t" + reqId + "\t" + responseBody;
    }

    public void processFile(List<FileInfo> fileInfoList, boolean batch, int retryCount) throws QiniuException {

        if (fileInfoList == null || fileInfoList.size() == 0) return;
        List<String> keyList = fileInfoList.stream().map(fileInfo -> fileInfo.key).collect(Collectors.toList());

        if (batch) {
            List<String> resultList = new ArrayList<>();
            for (String key : keyList) {
                try {
                    String result = run(srcBucket, key, tarBucket, keepKey ? key : null, keyPrefix,
                            false, retryCount);
                    if (!StringUtils.isNullOrEmpty(result)) resultList.add(result);
                } catch (QiniuException e) {
                    System.out.println("type failed. " + e.error());
                    fileReaderAndWriterMap.writeErrorOrNull(srcBucket + "\t" + tarBucket + "\t" + keyPrefix + "\t"
                            + key + "\t" + "\t" + e.error());
                    if (!e.response.needRetry()) throw e;
                    else e.response.close();
                }
            }
            if (resultList.size() > 0) fileReaderAndWriterMap.writeSuccess(String.join("\n", resultList));
            return;
        }

        int times = fileInfoList.size()/1000 + 1;
        for (int i = 0; i < times; i++) {
            List<String> processList = keyList.subList(1000 * i, i == times - 1 ? keyList.size() : 1000 * (i + 1));
            if (processList.size() > 0) {
                try {
                    String result = batchRun(srcBucket, tarBucket, keyPrefix, keepKey, processList,
                            retryCount);
                    if (!StringUtils.isNullOrEmpty(result)) fileReaderAndWriterMap.writeSuccess(result);
                } catch (QiniuException e) {
                    System.out.println("copy failed. " + e.error());
                    fileReaderAndWriterMap.writeErrorOrNull(srcBucket + "\t" + tarBucket + "\t" + keyPrefix + "\t"
                            + processList + "\t" + "\t" + e.error());
                    if (!e.response.needRetry()) throw e;
                    else e.response.close();
                }
            }
        }
    }

    public void closeResource() {
        fileReaderAndWriterMap.closeWriter();
    }
}
