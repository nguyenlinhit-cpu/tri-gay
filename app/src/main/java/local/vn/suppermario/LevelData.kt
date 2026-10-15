package local.vn.suppermario

data class PipeData(val x: Float, val height: Float, val teleportTargetX: Float? = null)
data class BlockData(
    val x: Float,
    val yOffset: Float,
    val type: BlockType,
    val powerUpType: PowerUpType? = null,
    val weaponType: WeaponType? = null
)
data class CoinData(val x: Float, val yOffset: Float)
data class GoombaData(val x: Float, val minX: Float, val maxX: Float, val speed: Float)
data class BirdData(val x: Float, val minX: Float, val maxX: Float, val height: Float = 350f)
data class RiverData(val startX: Float, val endX: Float)
data class PlatformData(
    val x: Float,
    val yOffset: Float,
    val width: Float = 140f,
    val minX: Float = 0f,
    val maxX: Float = 0f,
    val speed: Float = 0f,
    val isRock: Boolean = false
)

class LevelData(
    val worldName: String,
    val skyColor: Int,
    val timeLimit: Int,
    val pipes: List<PipeData>,
    val blocks: List<BlockData>,
    val coins: List<CoinData>,
    val goombas: List<GoombaData>,
    val birds: List<BirdData> = emptyList(),
    val rivers: List<RiverData> = emptyList(),
    val platforms: List<PlatformData> = emptyList(),
    val isBossLevel: Boolean = false,
    val bossX: Float = 0f,
    val flagpoleX: Float = 5200f,
    val challenge: String = "",
    val arenaStartX: Float = 0f
) {
    companion object {
        fun getLevels(difficulty: Difficulty): List<LevelData> {
            val rules = ChallengeRules.forDifficulty(difficulty)
            fun enemy(x: Float, left: Float, right: Float, speed: Float) =
                GoombaData(x, left, right, speed * rules.enemySpeed)
            fun supply(x: Float, type: PowerUpType) =
                BlockData(x, 175f, BlockType.QUESTION, powerUpType = type)
            fun weapon(x: Float, type: WeaponType) =
                BlockData(x, 175f, BlockType.QUESTION, weaponType = type)
            fun trail(vararg points: Pair<Float, Float>) = points.map { CoinData(it.first, it.second) }
            fun time(seconds: Int) = (seconds * rules.timeScale).toInt()

            // Every level has a safe opening, separated encounters and a clear final stretch.
            val level1 = LevelData(
                worldName = "WORLD 1-1", skyColor = 0xFF6EB4FF.toInt(), timeLimit = time(230),
                challenge = "VƯỜN GAI • Canh nhịp ống, né đá rơi",
                pipes = listOf(PipeData(720f, 120f), PipeData(1280f, 160f),
                    PipeData(2480f, 140f), PipeData(3040f, 180f), PipeData(4450f, 170f)),
                blocks = listOf(supply(330f, PowerUpType.MUSHROOM),
                    BlockData(405f, 175f, BlockType.BRICK), supply(2700f, PowerUpType.COIN),
                    supply(4110f, PowerUpType.MUSHROOM)),
                rivers = listOf(RiverData(1770f, 2210f), RiverData(3540f, 4070f)),
                platforms = listOf(PlatformData(1900f, 110f, 120f, isRock = true),
                    PlatformData(3650f, 100f, 110f, isRock = true),
                    PlatformData(3870f, 145f, 110f, isRock = true)),
                birds = listOf(BirdData(1510f, 1440f, 1740f, 330f),
                    BirdData(2730f, 2650f, 2980f, 350f), BirdData(3740f, 3560f, 4050f, 370f)),
                goombas = listOf(enemy(510f, 470f, 690f, 145f), enemy(990f, 900f, 1240f, 155f),
                    enemy(2300f, 2250f, 2460f, 165f), enemy(3260f, 3200f, 3510f, 175f),
                    enemy(4750f, 4620f, 5000f, 185f)),
                coins = trail(760f to 210f, 1320f to 250f, 1935f to 195f,
                    2520f to 230f, 3080f to 270f, 3680f to 185f, 3900f to 230f, 4490f to 260f)
            )
            val level2 = LevelData(
                worldName = "WORLD 1-2", skyColor = 0xFF5AA0D8.toInt(), timeLimit = time(220),
                challenge = "ĐẢO ĐÁ • Nhảy chính xác qua ba khúc sông",
                pipes = listOf(PipeData(650f, 150f), PipeData(2290f, 180f), PipeData(4510f, 175f)),
                blocks = listOf(supply(330f, PowerUpType.MUSHROOM),
                    supply(2510f, PowerUpType.MUSHROOM), supply(4270f, PowerUpType.COIN)),
                rivers = listOf(RiverData(980f, 2050f), RiverData(2740f, 3650f), RiverData(3910f, 4400f)),
                platforms = listOf(PlatformData(1090f, 110f, 110f, isRock = true),
                    PlatformData(1330f, 160f, 100f, isRock = true),
                    PlatformData(1570f, 110f, 100f, isRock = true),
                    PlatformData(1820f, 165f, 105f, isRock = true),
                    PlatformData(2850f, 120f, 105f, isRock = true),
                    PlatformData(3090f, 175f, 100f, isRock = true),
                    PlatformData(3370f, 120f, 100f, isRock = true),
                    PlatformData(4010f, 105f, 100f, isRock = true),
                    PlatformData(4210f, 160f, 100f, isRock = true)),
                birds = listOf(BirdData(1250f, 1080f, 1580f, 380f),
                    BirdData(1810f, 1650f, 2020f, 395f), BirdData(3100f, 2850f, 3520f, 395f),
                    BirdData(4700f, 4650f, 5020f, 320f)),
                goombas = listOf(enemy(480f, 440f, 630f, 170f), enemy(2130f, 2080f, 2270f, 180f),
                    enemy(2490f, 2420f, 2700f, 190f), enemy(4750f, 4660f, 5040f, 205f)),
                coins = trail(1120f to 195f, 1360f to 245f, 1600f to 195f, 1850f to 250f,
                    2880f to 205f, 3120f to 260f, 3400f to 205f, 4040f to 190f, 4240f to 245f)
            )
            val level3 = LevelData(
                worldName = "WORLD 1-3", skyColor = 0xFF547CB6.toInt(), timeLimit = time(215),
                challenge = "CẦU CHUYỂN ĐỘNG • Chờ nền tới rồi nhảy",
                pipes = listOf(PipeData(690f, 170f), PipeData(2440f, 180f), PipeData(4620f, 185f)),
                blocks = listOf(supply(330f, PowerUpType.MUSHROOM),
                    supply(2690f, PowerUpType.MUSHROOM), BlockData(2765f, 175f, BlockType.BRICK)),
                rivers = listOf(RiverData(1050f, 2240f), RiverData(3020f, 4430f)),
                platforms = listOf(PlatformData(1150f, 105f, 125f, isRock = true),
                    PlatformData(1450f, 155f, 115f, 1360f, 1720f, 155f * rules.platformSpeed),
                    PlatformData(1850f, 110f, 115f, isRock = true),
                    PlatformData(3120f, 110f, 120f, isRock = true),
                    PlatformData(3400f, 170f, 105f, 3320f, 3680f, 175f * rules.platformSpeed),
                    PlatformData(3820f, 125f, 110f, isRock = true),
                    PlatformData(4100f, 165f, 105f, 4030f, 4380f, 190f * rules.platformSpeed)),
                birds = listOf(BirdData(1560f, 1390f, 1840f, 410f),
                    BirdData(2860f, 2800f, 3000f, 350f), BirdData(3530f, 3350f, 3800f, 420f),
                    BirdData(4200f, 4050f, 4400f, 420f)),
                goombas = listOf(enemy(500f, 440f, 670f, 185f), enemy(850f, 820f, 1020f, 195f),
                    enemy(2310f, 2260f, 2420f, 200f), enemy(2790f, 2660f, 2990f, 215f),
                    enemy(4880f, 4780f, 5080f, 225f)),
                coins = trail(1180f to 190f, 1530f to 250f, 1880f to 195f,
                    3150f to 195f, 3480f to 265f, 3850f to 210f, 4190f to 260f)
            )
            val level4 = LevelData(
                worldName = "WORLD 1-4", skyColor = 0xFF292443.toInt(), timeLimit = time(200),
                challenge = "PHÁO ĐÀI ỐNG • Đáp lên ống có mũi tên để dịch chuyển",
                pipes = listOf(PipeData(680f, 180f), PipeData(1120f, 170f, 2390f),
                    PipeData(2390f, 170f), PipeData(3020f, 185f), PipeData(3530f, 180f, 4590f),
                    PipeData(4590f, 180f)),
                blocks = listOf(supply(330f, PowerUpType.MUSHROOM),
                    weapon(2720f, WeaponType.ROCK_THROW), supply(4830f, PowerUpType.MUSHROOM)),
                rivers = listOf(RiverData(1320f, 2270f), RiverData(3740f, 4470f)),
                platforms = listOf(PlatformData(2830f, 120f, 100f, isRock = true),
                    PlatformData(3260f, 135f, 105f, isRock = true)),
                birds = listOf(BirdData(920f, 860f, 1080f, 390f),
                    BirdData(2820f, 2660f, 2980f, 405f), BirdData(3340f, 3240f, 3490f, 410f),
                    BirdData(4950f, 4840f, 5090f, 370f)),
                goombas = listOf(enemy(480f, 430f, 650f, 205f), enemy(900f, 830f, 1090f, 215f),
                    enemy(2660f, 2620f, 2980f, 225f), enemy(3290f, 3200f, 3500f, 235f),
                    enemy(4930f, 4830f, 5100f, 245f)),
                coins = trail(720f to 270f, 1160f to 260f, 2430f to 260f,
                    2860f to 205f, 3060f to 275f, 3290f to 220f, 3570f to 270f, 4630f to 270f)
            )
            val level5 = LevelData(
                worldName = "WORLD 1-5", skyColor = 0xFF461E36.toInt(), timeLimit = time(240),
                challenge = "THÀNH TRÌ CUỐI • Hạ boss để mở cổng đích",
                isBossLevel = true, bossX = 4250f, arenaStartX = 3350f,
                pipes = listOf(PipeData(810f, 170f), PipeData(2540f, 190f)),
                blocks = listOf(weapon(250f, WeaponType.GUN), weapon(410f, WeaponType.ROCK_THROW),
                    weapon(570f, WeaponType.SWORD), supply(1050f, PowerUpType.MUSHROOM),
                    supply(2940f, PowerUpType.MUSHROOM),
                    // Spare weapons before the arena prevent a mandatory weapon from being missed.
                    weapon(3100f, WeaponType.GUN), weapon(3260f, WeaponType.SWORD)),
                rivers = listOf(RiverData(1320f, 2300f)),
                platforms = listOf(PlatformData(1430f, 115f, 110f, isRock = true),
                    PlatformData(1680f, 165f, 105f, 1620f, 1970f, 190f * rules.platformSpeed),
                    PlatformData(2070f, 120f, 105f, isRock = true),
                    // Raised positions offer a way to jump over the boss volley.
                    PlatformData(3630f, 130f, 125f), PlatformData(4650f, 130f, 125f)),
                birds = listOf(BirdData(1160f, 1080f, 1290f, 360f),
                    BirdData(1800f, 1580f, 2160f, 415f), BirdData(2820f, 2730f, 2930f, 380f)),
                goombas = listOf(enemy(1170f, 1040f, 1290f, 220f),
                    enemy(2380f, 2330f, 2520f, 235f), enemy(2780f, 2700f, 2910f, 250f)),
                coins = trail(1460f to 200f, 1770f to 260f, 2100f to 205f,
                    2580f to 280f, 3660f to 215f, 4680f to 215f)
            )
            return listOf(level1, level2, level3, level4, level5)
        }
    }
}
