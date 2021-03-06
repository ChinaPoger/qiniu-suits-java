package com.qiniu.service.datasource;

import com.qiniu.common.QiniuException;
import com.qiniu.persistence.FileMap;
import com.qiniu.service.convert.MapToString;
import com.qiniu.service.convert.LineToMap;
import com.qiniu.service.interfaces.ILineProcess;
import com.qiniu.service.interfaces.ITypeConvert;
import com.qiniu.util.HttpResponseUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileInput implements IDataSource {

    final private String filePath;
    final private String parseType;
    final private String separator;
    final private Map<String, String> indexMap;
    final private int unitLen;
    final private String savePath;
    private boolean saveTotal;
    private String saveFormat;
    private String saveSeparator;
    private List<String> rmFields;

    public FileInput(String filePath, String parseType, String separator, Map<String, String> indexMap, int unitLen,
                     String savePath) {
        this.filePath = filePath;
        this.parseType = parseType;
        this.separator = separator;
        this.indexMap = indexMap;
        this.unitLen = unitLen;
        this.savePath = savePath;
        this.saveTotal = false;
    }

    public void setResultOptions(boolean saveTotal, String format, String separator, List<String> rmFields) {
        this.saveTotal = saveTotal;
        this.saveFormat = format;
        this.saveSeparator = separator;
        this.rmFields = rmFields;
    }

    private void traverseByReader(BufferedReader reader, FileMap fileMap, ILineProcess<Map<String, String>> processor)
            throws IOException {
        ITypeConvert<String, Map<String, String>> typeConverter = new LineToMap(parseType, separator, indexMap);
        ITypeConvert<Map<String, String>, String> writeTypeConverter = new MapToString(saveFormat, saveSeparator, rmFields);
        List<String> srcList = new ArrayList<>();
        List<Map<String, String>> infoMapList;
        List<String> writeList;
        String line = null;
        boolean goon = true;
        while (goon) {
            // 避免文件过大，行数过多，使用 lines() 的 stream 方式直接转换可能会导致内存泄漏，故使用 readLine() 的方式
            try { line = reader.readLine(); } catch (IOException e) { e.printStackTrace(); }
            if (line == null) goon = false;
            else srcList.add(line);
            if (srcList.size() >= unitLen || line == null) {
                infoMapList = typeConverter.convertToVList(srcList);
                if (typeConverter.getErrorList().size() > 0)
                    fileMap.writeError(String.join("\n", typeConverter.consumeErrorList()), false);
                if (saveTotal) {
                    writeList = writeTypeConverter.convertToVList(infoMapList);
                    if (writeList.size() > 0) fileMap.writeSuccess(String.join("\n", writeList), false);
                }
                // 如果抛出异常需要检测下异常是否是可继续的异常，如果是程序可继续的异常，忽略当前异常保持数据源读取过程继续进行
                try {
                    if (processor != null) processor.processLine(infoMapList);
                } catch (QiniuException e) {
                    HttpResponseUtils.processException(e, 1, null, null);
                }
                srcList = new ArrayList<>();
            }
        }
    }

    private void export(FileMap recordFileMap, String identifier, BufferedReader reader,
                        ILineProcess<Map<String, String>> processor) throws Exception {
        FileMap fileMap = new FileMap(savePath, "fileinput", identifier.split(": ")[0]);
        fileMap.initDefaultWriters();
//        if (processor != null) processor.setSaveTag(identifier);
        ILineProcess<Map<String, String>> lineProcessor = processor == null ? null : processor.clone();
        String record = "order " + identifier;
        String next;
        try {
            recordFileMap.writeKeyFile("result", record + "\treading...", true);
            traverseByReader(reader, fileMap, lineProcessor);
            next = reader.readLine();
            if (next == null) record += "\tsuccessfully done";
            else record += "\tnextLine:" + next;
            System.out.println(record);
        } catch (IOException e) {
            try { next = reader.readLine(); } catch (IOException ex) { next = ex.getMessage(); }
            record += "\tnextLine:" + next + "\t" + e.getMessage().replaceAll("\n", "\t");
            e.printStackTrace();
            throw e;
        } finally {
            recordFileMap.writeKeyFile("result", record, true);
            fileMap.closeWriters();
            if (lineProcessor != null) lineProcessor.closeResource();
        }
    }

    synchronized private void exit(AtomicBoolean exit, Exception e) {
        if (!exit.get()) e.printStackTrace();
        exit.set(true);
        System.exit(-1);
    }

    public void export(int threads, ILineProcess<Map<String, String>> processor) throws Exception {
        FileMap inputFiles = new FileMap(savePath);
        File sourceFile = new File(filePath);
        if (sourceFile.isDirectory()) {
            inputFiles.initReaders(filePath);
        } else {
            inputFiles.initReader(filePath);
        }

        HashMap<String, BufferedReader> readers = inputFiles.getReaderMap();
        int listSize = readers.size();
        int runningThreads = listSize < threads ? listSize : threads;
        String info = "read files" + (processor == null ? "" : " and " + processor.getProcessName());
        System.out.println(info + " running...");
        ExecutorService executorPool = Executors.newFixedThreadPool(runningThreads);
        AtomicBoolean exit = new AtomicBoolean(false);
        List<String> keys = new ArrayList<>(readers.keySet());
        for (int i = 0; i < keys.size(); i++) {
            int fi = i;
            executorPool.execute(() -> {
                try {
                    export(inputFiles, fi + ": " + keys.get(fi), readers.get(keys.get(fi)), processor);
                } catch (Exception e) {
                    exit(exit, e);
                }
            });
        }
        executorPool.shutdown();
        while (!executorPool.isTerminated()) Thread.sleep(1000);
        inputFiles.closeReaders();
        System.out.println(info + " finished");
    }
}
