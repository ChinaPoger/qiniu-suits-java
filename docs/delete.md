# 资源删除

# 简介
对空间中的资源进行删除。参考：[七牛空间资源删除](https://developer.qiniu.com/kodo/api/1257/delete)/[批量删除](https://developer.qiniu.com/kodo/api/1250/batch)

### 配置文件选项

#### 必须参数
|数据源方式|参数及赋值|  
|--------|-----|  
|source-type=list（空间资源列举）|[list 数据源参数](listbucket.md) <br> process=delete |  
|source-type=file（文件资源列表）|[file 数据源参数](fileinput.md) <br> process=delete <br> ak=\<ak\> <br> sk=\<sk\> <br> bucket=\<bucket\> |  

#### 可选参数
```
ak=
sk=
bucket=
```

### 参数字段说明
|参数名|参数值及类型 | 含义|  
|-----|-------|-----|  
|process=delete| 删除资源时设置为delete| 表示资源删除操作|  
|ak、sk|长度40的字符串|七牛账号的ak、sk，通过七牛控制台个人中心获取，当数据源方式为 list 时无需再设置|  
|bucket| 字符串| 操作的资源所在空间，当数据源为 list 时无需再设置|  

### 命令行方式
```
-process=delete -ak= -sk= -bucket=  
```