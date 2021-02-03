# GohPackTool

Android 游戏客户端渠道打包脚本

## Usage

根据 channels 文件夹下的脚本按顺序传入各参数执行 Jar 命令即可

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

### Utils

工具合集，按需添加。用好 Kotlin 扩展函数。