package dev.customglyph.data

import dev.customglyph.imaging.Adjustments
import dev.customglyph.model.*
import org.json.JSONArray
import org.json.JSONObject

internal object DesignJson {
    fun draft(value: Draft): JSONObject =
        JSONObject().apply {
            put("image", value.image)
            put("name", value.name)
            put("profile", value.profile.name)
            with(value.adjustments.normalized()) {
                put("scale", scale)
                put("x", x)
                put("y", y)
                put("rotation", rotation)
                put("brightness", brightness)
                put("contrast", contrast)
                put("silhouette", silhouette)
                put("invert", invert)
                put("dither", dither)
            }
        }

    fun draft(json: JSONObject): Draft =
        Draft(
            image = json.optString("image").takeIf { it != "null" && it.isNotEmpty() },
            name = json.optString("name", "Glyph mark"),
            profile = DeviceProfile.fromId(json.optString("profile")),
            adjustments =
                Adjustments(
                        scale = json.optDouble("scale", 1.0).toFloat(),
                        x = json.optDouble("x", 0.0).toFloat(),
                        y = json.optDouble("y", 0.0).toFloat(),
                        rotation = json.optDouble("rotation", 0.0).toFloat(),
                        brightness = json.optDouble("brightness", .75).toFloat(),
                        contrast = json.optDouble("contrast", 1.0).toFloat(),
                        silhouette = json.optBoolean("silhouette"),
                        invert = json.optBoolean("invert"),
                        dither = json.optBoolean("dither"),
                    )
                    .normalized(),
        )

    fun design(value: SavedDesign): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("name", value.name)
            .put("draft", draft(value.draft))
            .put("frames", value.frameFile)
            .put("recent", value.recent)
            .put("createdAt", value.createdAt)

    fun design(json: JSONObject) =
        SavedDesign(
            json.getString("id"),
            json.getString("name"),
            draft(json.getJSONObject("draft")),
            json.getString("frames"),
            json.optBoolean("recent"),
            json.optLong("createdAt"),
        )

    fun rule(value: ScheduleRule): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("design", value.designId)
            .put("start", value.startMinute)
            .put("end", value.endMinute)
            .put("days", JSONArray(value.days.sorted()))
            .put("enabled", value.enabled)

    fun rule(json: JSONObject): ScheduleRule {
        val days = json.getJSONArray("days")
        return ScheduleRule(
            json.getString("id"),
            json.getString("design"),
            json.getInt("start"),
            json.getInt("end"),
            (0 until days.length()).map { days.getInt(it) }.toSet(),
            json.optBoolean("enabled", true),
        )
    }
}
