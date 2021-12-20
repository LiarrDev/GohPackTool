package goh.games

class GameFactory(private val apk: String) {
    fun getGame(gid: String): Game? {
        return when (gid) {
            "111" -> Game111(apk)
            "116" -> Game116(apk)
            "119" -> Game119(apk)
            "120" -> Game120(apk)
            "123" -> Game123(apk)
            "124" -> Game124(apk)
            "125" -> Game125(apk)
            "126" -> Game126(apk)
            "127" -> Game127(apk)
            "128" -> Game128(apk)
            "129" -> Game129(apk)
            "131" -> Game131(apk)
            "132" -> Game132(apk)
            "133" -> Game133(apk)
            "135" -> Game135(apk)
            "136" -> Game136(apk)
            "137" -> Game137(apk)
            "139" -> Game139(apk)
            "141" -> Game141(apk)
            "142" -> Game142(apk)
            "143" -> Game143(apk)
            "145" -> Game145(apk)
            "146" -> Game146(apk)
            "147" -> Game147(apk)
            "148" -> Game148(apk)
            "149" -> Game149(apk)
            "150" -> Game150(apk)
            "151" -> Game151(apk)
            "152" -> Game152(apk)
            "153" -> Game153(apk)
            else -> null
        }
    }
}