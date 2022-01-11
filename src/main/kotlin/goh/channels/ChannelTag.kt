package goh.channels

enum class ChannelTag(val tag: String) {
    NO_SDK("0"),        // 无 SDK，如原包、搜狗搜索等
    TOUTIAO("1"),       // 头条
    UC("2"),            // 汇川 UC
    KWAI("3"),          // 快手
    IQIYI("4"),         // 爱奇艺
    XINGTU("5"),        // 星图
    BAIDU("6"),         // 百度
    GDT("7"),           // 广点通
    MI("8"),            // 小米联运
    OPPO("9"),          // OPPO 联运
    VIVO("10"),         // ViVO 联运
    YSDK("11"),         // 应用宝
    DALAN("12"),        // 大蓝
    HUAWEI("13")        // 华为
}