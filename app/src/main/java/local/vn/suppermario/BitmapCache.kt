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
    val marioIdle: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mario_idle)
    val marioWalkA: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mario_walk_a)
    val marioWalkB: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mario_walk_b)
    val marioJump: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mario_jump)

    val pinkIdle: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pink_idle)
    val pinkWalkA: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pink_walk_a)
    val pinkWalkB: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pink_walk_b)
    val pinkJump: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pink_jump)

    val greenIdle: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.green_idle)
    val greenWalkA: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.green_walk_a)
    val greenWalkB: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.green_walk_b)
    val greenJump: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.green_jump)

    val goombaIdle: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.goomba_idle)
    val goombaWalkA: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.goomba_walk_a)
    val goombaWalkB: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.goomba_walk_b)
    val goombaFlat: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.goomba_flat)

    val bossTurtle: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.boss_turtle)
    val fireball: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.fireball)

    val blockQuestion: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.block_question)
    val blockEmpty: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.block_empty)
    val blockBrick: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.block_brick)

    val coinGold: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.coin_gold)
    val mushroomRed: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mushroom_red)
    val star: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.star)
    val waterTop: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.water_top)
    val rock: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.rock)

    val grassTop: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.grass_top)
    val dirtCenter: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dirt_center)
    val bgClouds: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_clouds)
    val bgHills: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_hills)
    val hudHeart: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.hud_heart)
}
