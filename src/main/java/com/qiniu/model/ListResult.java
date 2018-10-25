package com.qiniu.model;

import com.qiniu.storage.model.FileInfo;

import java.util.List;

public class ListResult {

    public String commonPrefix = "";

    public List<FileInfo> fileInfoList;

    public String nextMarker = "";

    public boolean isValid() {
        return fileInfoList != null || !"".equals(nextMarker);
    }
}