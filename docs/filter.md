# 资源过滤

## 简介
对资源信息进行过滤，接受 source 数据源输入的信息字段按条件过滤后输出符合条件的记录，目前支持七牛存储资源的元信息字段。  

## 配置文件选项

### 配置参数
```
f-prefix=
f-suffix=
f-regex=
f-mime=image
f-type=
f-status=
f-date-scale=
f-anti-prefix=
f-anti-suffix=
f-anti-regex=
f-anti-mime=
f-check=
f-check-config=
f-check-rewrite=
```  
|参数名|参数值及类型 | 含义|  
|-----|-------|-----|  
|f-prefix| ","分隔的字符串列表| 表示**选择**文件名符合该前缀的文件|  
|f-suffix| ","分隔的字符串列表| 表示**选择**文件名符合该后缀的文件|  
|f-inner| ","分隔的字符串列表| 表示**选择**文件名包含该部分字符的文件|  
|f-regex| ","分隔的字符串列表| 表示**选择**文件名符合该正则表达式的文件，所填内容必须为正则表达式|  
|f-mime| ","分隔的字符串列表| 表示**选择**符合该 mime 类型的文件|  
|f-type| 0/1| 表示**选择**符合该存储类型的文件, 为 0（标准存储）或 1（低频存储）|  
|f-status| 0/1| 表示**选择**符合该存储状态的文件, 为 0（启用）或 1（禁用）|  
|f-date-scale| 字符串| 设置过滤的时间范围，格式为 [\<date1\>,\<date2\>]，\<date\> 格式为："2018-08-01 00:00:00"|  
|f-anti-prefix| ","分隔的字符串列表| 表示**排除**文件名符合该前缀的文件|  
|f-anti-suffix| ","分隔的字符串列表| 表示**排除**文件名符合该后缀的文件|  
|f-anti-inner| ","分隔的字符串列表| 表示**排除**文件名包含该部分字符的文件|  
|f-anti-regex| ","分隔的字符串列表| 表示**排除**文件名符合该正则表达式的文件，所填内容必须为正则表达式|  
|f-anti-mime| ","分隔的字符串列表| 表示**排除**该 mime 类型的文件|  
|f-check|字符串| 是否进行**后缀名**和**mimeType**（即 content-type）匹配性检查，不符合规范的疑似异常文件将被筛选出来|  
|f-check-config|配置文件路径字符串|自定义资源字段规范对应关系列表的配置文件，格式为 json，配置举例：[check-config 配置](../resources/check-config.json)|  
|f-check-rewrite|true/false|是否完全使用自定义的规范列表进行检查，默认为 false，程序包含的默认字段规范对应关系配置见：[check 默认配置](../resources/check.json)|  

**备注：**  
过滤条件中，f-prefix,f-suffix,f-inner,f-regex,f-mime 可以为列表形式，用逗号分割，如 param1,param2,param3。
f-prefix,f-suffix,f-inner,f-regex 四个均为针对文件名 key 的过滤条件，多个过滤条件时使用 &&（与）的关系得到最终过滤结果。f-anti-xx 的参数
表示反向过滤条件，即排除符合该特征的记录。  

**关于 f-check[-x]**:  
*f-check* 目前支持检查**后缀名**和**mimeType**的对应关系
*f-check-config* 自定义规范列表配置 "ext-mime" 字段必填，切元素类型为列表 [], 否则无效，后缀名和 mimeType 用 ":" 组合成字符串成为一组对应
关系写法如下：  
```
{
  "ext-mime": [
    "mp5:video/mp5"
  ]
}
```  
*f-check-rewrite* 如果为 true 的话，则程序内置的规范对应关系将失效，只检查自定义的规范列表。  

## 命令行方式
```
-f-prefix= -f-suffix= -f-inner= -f-regex= -f-mime= -f-type= -f-status= -f-date-scale= -f-anti-prefix= -f-anti-suffix= -f-anti-inner= -f-anti-regex= -f-anti-mime=
```
