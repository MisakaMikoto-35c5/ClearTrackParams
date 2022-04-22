# ClearTrackParams

清理 URL 中的跟踪参数，
程序提供了两种方法清理 URL。

通过“分享接口”清理时：需要找到先分享给这个程序，
程序清理完成以后会再次弹出分享界面，此时正常选择要分享给的 App 即可。

通过“剪切板”清理：有一些 App 不使用系统分享接口，
此时只能将链接复制到剪切板，然后打开这个程序，
按下“粘贴并清理”按钮，之后选择是要复制还是再次分享即可。

## 规则接口说明

基础规则使用 toml 格式编写。

### 规则字段说明

#### 文件主体

|       字段名称       |  字段类型  |      说明      |
| ------------------- | -------- | -------------- |
| rule_configuration  |   dict   | 规则文档全局配置 |
| rules               |  array   | 规则详情        |

#### 规则文档全局配置

|       字段名称       |  字段类型  |                         说明                         |
| ------------------- | -------- | ---------------------------------------------------- |
| webview_pre_execute |  string  | WebView 运行前所执行的脚本（适合放一些规则全局需要的支持库） |

#### 规则详情

|       字段名称       |  字段类型  |                                 说明                                           |
| ------------------- | -------- | ------------------------------------------------------------------------------ |
| regex               |  string  | 用于匹配 URL 的正则表达式                                                         |
| action              |  string  | 匹配到的 URL 需要执行的操作（详情见下）                                             |
| params              | string[] | 当 action 为 PARAM_BLACKLIST 或 PARAM_WHITELIST 时，需要排除或加入的参数（详情见下） |
| javascript          |  string  | 当 action 为 WEB_VIEW 时，在浏览器中执行的脚本（详情见下）                           |

##### 支持的 action

|       操作       |                              说明                             |
| --------------- | ------------------------------------------------------------- |
| CLEAR_ALL_PARAM | 清除所有参数（即问号后面全部字符）                                  |
| PARAM_BLACKLIST | 清除部分参数，传入的 params 列表将作为黑名单清理                     ｜
| PARAM_WHITELIST | 清除部分参数，传入的 params 列表将作为白名单保留，其余的将清理         ｜
| WEB_VIEW        | URL 将使用 WebView 和指定的 Javascript 处理（Javascript 接口见下） ｜

## WebView 操作

WebView 提供了一个可被控制的浏览器，能够在页面当中插入自定义脚本。
因此，WebView 适用于那些使用 JS 进行层层跳转的短链接。

在该程序中，WebView 提供了如下的 Hooks：

|           Hook           |                                                          说明                                                        |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| document.onUrlPreLoad    | 在 WebView 收到访问 URL 请求时调用，传入需要被访问的 URL。每次页面更换 URL 都会调用该方法，需要开发者在该方法内决定继续访问哪个 URL。 |
| document.onUrlLoad       | 页面加载时调用。                                                                                                       ｜
| document.onUrlLoadFinish | 页面加载完成时调用。                                                                                                   ｜

### Javascript API

**注意：所有 API 都只能在脚本作用域内调用！**

#### control.changeAllowLoadUrl()

- 传入参数：无
- 返回值类型：undefined

该方法将告诉 Webview 接下来访问的一个链接需要被正常引导，
该操作仅能由 `document.onUrlPreLoad` 这个钩子发出。

- 示例代码：
见 control.completion(result) 的示例代码

#### control.completion(result)

- 传入参数：result: String
- 返回值类型：undefined

当脚本获取到干净的 URL 时，需要调用该方法回调，
result 传入的即处理完成的干净 URL。

- 示例代码：
```
document.onUrlPreLoad = (url) => {
  if (url.indexOf('www.example.com/redirect?key=') !== -1) { // 判断是否为需要处理的链接
     control.changeAllowLoadUrl(); // 告知程序接下来的 URL 需要正常跳转
     location.href = url; // 跳转至指定 URL
     return;
  }
  control.completion(url); // 链接被转换为真实链接后，告知程序转换完成。
}
```

#### control.error(errorMessage)

- 传入参数：errorMessage: String
- 返回值类型：undefined

该方法将告诉程序转换失败，并且提供失败信息。

- 示例代码：
```

document.onUrlPreLoad = (url) => {
  if (url.indexOf('www.example.com/redirect?key=') !== -1) { // 判断是否为需要处理的链接
     control.changeAllowLoadUrl(); // 告知程序接下来的 URL 需要正常跳转
     location.href = url; // 跳转至指定 URL
     return;
  }
  if (document.body.innerHTML.indexOf('403 Forbidden') !== -1) { // 在页面中查找是否包含对应字符串
    control.error('IP 被封禁'); // 将错误报告给程序
    return;
  }
  control.completion(url);
```

**注意：当 JS 脚本触发异常时（不包含脚本格式不正确），程序也会调用该方法报告错误。**

#### control.cleanUrlParams(url, keywords, isBlacklist)

- 传入参数：url: String, keywords: String[], isBlacklist: Boolean
- 返回值类型：String（清理完成的 URL）

该方法为程序内部提供的简单清理的方法，
开放出来作为工具提供。

对应 PARAM_BLACKLIST 和 PARAM_WHITELIST 的 Action。

#### control.getVersion()

- 传入参数：无
- 返回值类型：Int

获取程序内部版本号
