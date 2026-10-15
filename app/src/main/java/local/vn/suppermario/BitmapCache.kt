package local.vn.suppermario

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

enum class CharacterType {
    BEIGE,
    PINK,
    GREEN
}

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

class BitmapCache(context: Context) {
    private val resources = context.resources
    private val decodeOptions = BitmapFactory.Options().apply { inScaled = false }

    private fun load(resourceId: Int): Bitmap =
        BitmapFactory.decodeResource(resources, resourceId, decodeOptions).apply {
            // Game coordinates are pixels, including backgrounds drawn without a destination rect.
            density = Bitmap.DENSITY_NONE
        }

    val marioIdle: Bitmap = load(R.drawable.mario_idle)
    val marioWalkA: Bitmap = load(R.drawable.mario_walk_a)
    val marioWalkB: Bitmap = load(R.drawable.mario_walk_b)
    val marioJump: Bitmap = load(R.drawable.mario_jump)

    val pinkIdle: Bitmap = load(R.drawable.pink_idle)
    val pinkWalkA: Bitmap = load(R.drawable.pink_walk_a)
    val pinkWalkB: Bitmap = load(R.drawable.pink_walk_b)
    val pinkJump: Bitmap = load(R.drawable.pink_jump)

    val greenIdle: Bitmap = load(R.drawable.green_idle)
    val greenWalkA: Bitmap = load(R.drawable.green_walk_a)
    val greenWalkB: Bitmap = load(R.drawable.green_walk_b)
    val greenJump: Bitmap = load(R.drawable.green_jump)

    val goombaIdle: Bitmap = load(R.drawable.goomba_idle)
    val goombaWalkA: Bitmap = load(R.drawable.goomba_walk_a)
    val goombaWalkB: Bitmap = load(R.drawable.goomba_walk_b)
    val goombaFlat: Bitmap = load(R.drawable.goomba_flat)

    val bossTurtle: Bitmap = load(R.drawable.boss_turtle)
    val fireball: Bitmap = load(R.drawable.fireball)

    val blockQuestion: Bitmap = load(R.drawable.block_question)
    val blockEmpty: Bitmap = load(R.drawable.block_empty)
    val blockBrick: Bitmap = load(R.drawable.block_brick)

    val coinGold: Bitmap = load(R.drawable.coin_gold)
    val mushroomRed: Bitmap = load(R.drawable.mushroom_red)
    val star: Bitmap = load(R.drawable.star)
    val waterTop: Bitmap = load(R.drawable.water_top)
    val rock: Bitmap = load(R.drawable.rock)

    val grassTop: Bitmap = load(R.drawable.grass_top)
    val dirtCenter: Bitmap = load(R.drawable.dirt_center)
    val bgClouds: Bitmap = load(R.drawable.bg_clouds)
    val bgHills: Bitmap = load(R.drawable.bg_hills)
    val hudHeart: Bitmap = load(R.drawable.hud_heart)
}
