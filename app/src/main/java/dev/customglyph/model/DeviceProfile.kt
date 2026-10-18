package dev.customglyph.model

/** LED masks and aperture ratios from Nothing's official hardware SVG specifications. */
enum class DeviceProfile(
    val title: String,
    val size: Int,
    val aperture: Float,
    val aodOnly: Boolean,
    private val rows: IntArray,
) {
    PHONE_3(
        "Phone (3)",
        25,
        6.93301f / 9.467f,
        false,
        intArrayOf(
            7,
            11,
            15,
            17,
            19,
            21,
            21,
            23,
            23,
            25,
            25,
            25,
            25,
            25,
            25,
            25,
            23,
            23,
            21,
            21,
            19,
            17,
            15,
            11,
            7,
        ),
    ),
    PHONE_4A_PRO(
        "Phone (4a) Pro",
        13,
        15.585f / 18.109f,
        true,
        intArrayOf(5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5),
    );

    val ledCount: Int
        get() = rows.sum()

    fun contains(x: Int, y: Int): Boolean =
        y in 0 until size && x in ((size - rows[y]) / 2) until ((size + rows[y]) / 2)

    companion object {
        fun fromId(id: String?): DeviceProfile = entries.firstOrNull { it.name == id } ?: PHONE_3
    }
}
