package local.vn.suppermario

import android.graphics.Color

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
    val rivers: List<RiverData> = emptyList(),
    val platforms: List<PlatformData> = emptyList(),
    val isBossLevel: Boolean = false,
    val bossX: Float = 0f,
    val flagpoleX: Float = 5100f
) {
    companion object {
        fun getLevels(difficulty: Difficulty): List<LevelData> {
            val speedMult = when (difficulty) {
                Difficulty.EASY -> 0.8f
                Difficulty.MEDIUM -> 1.0f
                Difficulty.HARD -> 1.4f
            }

            val bH1 = 175f // Độ cao gạch tầng 1 (vừa tầm Mario nhảy húc nhẹ)
            val bH2 = 325f // Độ cao gạch tầng 2

            // MÀN 1: WORLD 1-1 (Nhập Môn Classic - Bố cục Cân Bằng)
            val level1 = LevelData(
                worldName = "WORLD 1-1",
                skyColor = Color.rgb(110, 180, 255),
                timeLimit = 400,
                pipes = listOf(
                    PipeData(850f, 110f),
                    PipeData(1400f, 150f),
                    PipeData(2400f, 180f),
                    PipeData(4300f, 140f)
                ),
                blocks = listOf(
                    // Cụm gạch nhập môn
                    BlockData(300f, bH1, BlockType.BRICK),
                    BlockData(375f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.MUSHROOM),
                    BlockData(450f, bH1, BlockType.BRICK),
                    
                    // Cụm gạch giữa ống nước
                    BlockData(1100f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.COIN),
                    
                    // Dãy gạch treo thưởng
                    BlockData(1800f, bH1, BlockType.BRICK),
                    BlockData(1875f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.MUSHROOM),
                    BlockData(1950f, bH1, BlockType.BRICK),
                    BlockData(2025f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.STAR),
                    BlockData(2100f, bH1, BlockType.BRICK),

                    // Cầu gạch cao
                    BlockData(3600f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.COIN),
                    BlockData(3675f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.COIN),
                    BlockData(3750f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.COIN)
                ),
                coins = listOf(
                    CoinData(375f, bH1 + 75f),
                    CoinData(1100f, bH1 + 75f),
                    CoinData(1875f, bH1 + 75f),
                    CoinData(2025f, bH1 + 75f),
                    CoinData(3600f, bH1 + 75f),
                    CoinData(3675f, bH1 + 75f),
                    CoinData(3750f, bH1 + 75f)
                ),
                goombas = listOf(
                    GoombaData(650f, 550f, 800f, 100f * speedMult),
                    GoombaData(1200f, 1000f, 1350f, 110f * speedMult),
                    GoombaData(2100f, 1850f, 2350f, 120f * speedMult),
                    GoombaData(3500f, 3300f, 3700f, 130f * speedMult)
                )
            )

            // MÀN 2: WORLD 1-2 (Vượt Sông & Cục Đá Nổi)
            val level2 = LevelData(
                worldName = "WORLD 1-2",
                skyColor = Color.rgb(90, 160, 230),
                timeLimit = 360,
                rivers = listOf(
                    RiverData(1050f, 1750f),
                    RiverData(2850f, 3650f)
                ),
                platforms = listOf(
                    PlatformData(1150f, 130f, 120f, isRock = true),
                    PlatformData(1400f, 170f, 120f, isRock = true),
                    PlatformData(1620f, 130f, 120f, isRock = true),

                    PlatformData(3000f, 140f, 120f, isRock = true),
                    PlatformData(3250f, 180f, 120f, isRock = true),
                    PlatformData(3480f, 130f, 120f, isRock = true)
                ),
                pipes = listOf(
                    PipeData(650f, 140f),
                    PipeData(2150f, 160f),
                    PipeData(4150f, 150f)
                ),
                blocks = listOf(
                    BlockData(350f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.MUSHROOM),
                    BlockData(2350f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.STAR),
                    BlockData(2425f, bH1, BlockType.BRICK)
                ),
                coins = listOf(
                    CoinData(1150f, 200f),
                    CoinData(1400f, 240f),
                    CoinData(1620f, 200f),
                    CoinData(3000f, 210f),
                    CoinData(3250f, 250f)
                ),
                goombas = listOf(
                    GoombaData(500f, 400f, 620f, 120f * speedMult),
                    GoombaData(1950f, 1820f, 2100f, 130f * speedMult),
                    GoombaData(2550f, 2400f, 2800f, 140f * speedMult),
                    GoombaData(3950f, 3750f, 4100f, 150f * speedMult)
                )
            )

            // MÀN 3: WORLD 1-3 (Nền Di Chuyển Qua Lại)
            val level3 = LevelData(
                worldName = "WORLD 1-3",
                skyColor = Color.rgb(100, 160, 240),
                timeLimit = 330,
                rivers = listOf(
                    RiverData(1350f, 2050f),
                    RiverData(3150f, 3850f)
                ),
                platforms = listOf(
                    PlatformData(1400f, 180f, 150f, minX = 1380f, maxX = 2000f, speed = 140f),
                    PlatformData(3200f, 200f, 150f, minX = 3180f, maxX = 3800f, speed = 160f)
                ),
                pipes = listOf(
                    PipeData(750f, 150f),
                    PipeData(2450f, 170f),
                    PipeData(4250f, 180f)
                ),
                blocks = listOf(
                    BlockData(350f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.MUSHROOM),
                    BlockData(950f, bH1, BlockType.BRICK),
                    BlockData(1025f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.STAR),
                    BlockData(2650f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.MUSHROOM)
                ),
                coins = listOf(
                    CoinData(1025f, bH1 + 75f),
                    CoinData(2650f, bH1 + 75f)
                ),
                goombas = listOf(
                    GoombaData(550f, 400f, 720f, 140f * speedMult),
                    GoombaData(1050f, 900f, 1250f, 150f * speedMult),
                    GoombaData(2250f, 2100f, 2400f, 160f * speedMult),
                    GoombaData(4050f, 3900f, 4200f, 170f * speedMult)
                )
            )

            // MÀN 4: WORLD 1-4 (Ống Nước Dịch Chuyển Teleport)
            val level4 = LevelData(
                worldName = "WORLD 1-4",
                skyColor = Color.rgb(40, 20, 50),
                timeLimit = 320,
                rivers = listOf(
                    RiverData(1150f, 2150f),
                    RiverData(3150f, 3950f)
                ),
                pipes = listOf(
                    PipeData(550f, 160f),
                    PipeData(950f, 180f, teleportTargetX = 2250f),
                    PipeData(2250f, 180f),
                    PipeData(2950f, 180f, teleportTargetX = 4050f),
                    PipeData(4050f, 180f)
                ),
                blocks = listOf(
                    BlockData(350f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.MUSHROOM),
                    BlockData(2450f, bH1, BlockType.QUESTION, powerUpType = PowerUpType.STAR),
                    BlockData(2525f, bH1, BlockType.BRICK)
                ),
                coins = listOf(
                    CoinData(350f, bH1 + 75f),
                    CoinData(2450f, bH1 + 75f)
                ),
                goombas = listOf(
                    GoombaData(750f, 630f, 900f, 160f * speedMult),
                    GoombaData(2550f, 2350f, 2850f, 170f * speedMult),
                    GoombaData(4250f, 4100f, 4550f, 180f * speedMult)
                )
            )

            // MÀN 5: WORLD 1-5 (TRƯỜNG ĐẤU BOSS RÙA HỒNG & SÓNG KIẾM)
            val level5 = LevelData(
                worldName = "WORLD 1-5 (BOSS BATTLE)",
                skyColor = Color.rgb(60, 15, 30),
                timeLimit = 300,
                isBossLevel = true,
                bossX = 3600f,
                flagpoleX = 4000f,
                pipes = listOf(
                    PipeData(950f, 160f)
                ),
                blocks = listOf(
                    // 3 Ô Chọn Vũ Khí
                    BlockData(250f, bH1, BlockType.QUESTION, weaponType = WeaponType.GUN),
                    BlockData(375f, bH1, BlockType.QUESTION, weaponType = WeaponType.ROCK_THROW),
                    BlockData(500f, bH1, BlockType.QUESTION, weaponType = WeaponType.SWORD),

                    // Tường gạch chỗ nấp (Cao 2 khối vừa tầm nhảy qua: 150px)
                    BlockData(1700f, 75f, BlockType.BRICK),
                    BlockData(1700f, 150f, BlockType.BRICK),

                    BlockData(2700f, 75f, BlockType.BRICK),
                    BlockData(2700f, 150f, BlockType.BRICK)
                ),
                coins = listOf(
                    CoinData(250f, bH1 + 75f),
                    CoinData(375f, bH1 + 75f),
                    CoinData(500f, bH1 + 75f)
                ),
                goombas = listOf(
                    GoombaData(1350f, 1050f, 1650f, 170f * speedMult),
                    GoombaData(2250f, 1950f, 2550f, 180f * speedMult)
                )
            )

            return listOf(level1, level2, level3, level4, level5)
        }
    }
}
