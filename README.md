# GohPackTool

Android 游戏客户端渠道打包脚本 for GohSDK。

## Usage

根据 channels 文件夹下的脚本按顺序传入各参数执行 Jar 命令即可。

## Development

### Games

增加游戏时，创建新的游戏类继承 `Game`，并重写抽象方法：

```Kotlin
class GameXXX(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        // 根据游戏的资源位置重写替换逻辑
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelName: String): Boolean {
        // 只需修改最后一个参数为游戏拼音简写即可
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName, "xxx")
    }
}
```

`extra()` 方法可以传入额外操作。

其他方法按需重写。

### Channels

增加渠道时，如果是普通买量广告渠道，可以直接复用 CombineAdPackTool.kt 脚本，如无法复用则另外新建脚本。

联运渠道由于操作复杂，建议每个联运渠道一个单独的脚本。

调用 `Game` 的成员方法时，应当按照 `Game` 类中定义的顺序先后调用，否则可能会导致某些成员变量没有被初始化而产生异常。

### Ad

最终打投放使用的广告包的脚本，无论是普通买量广告渠道还是联运渠道，都在此打入广告参数。

为兼容百度渠道的词条需求，还可以在此脚本中再次修改 `AppName`。

### Preprocess

对游戏包的各种预处理脚本。

### Utils

工具合集，按需添加。

## Libraries

- [Dom4J](https://github.com/dom4j/dom4j)
- [Zip4J](https://github.com/srikanth-lingala/zip4j)