package local.vn.suppermario

/** Timing is independent of rendering and runs only while the level is active. */
class CactusCycle(private val tempo: Float, phase: Float = 0f) {
    private var clock = phase
    val extension: Float
        get() = when {
            clock < 1.6f -> 0f
            clock < 2.4f -> (clock - 1.6f) / 0.8f
            clock < 4f -> 1f
            clock < 4.8f -> 1f - (clock - 4f) / 0.8f
            else -> 0f
        }

    fun update(dt: Float, playerNear: Boolean) {
        clock = if (clock < 1.6f && playerNear) 0f else (clock + dt * tempo) % 5.2f
    }
}
