package com.carpet.app.data

import android.content.Context
import com.carpet.app.domain.DriveLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class PetProfile(
    /** Species key drawn from the egg; null until the egg hatches. */
    val speciesKey: String? = null,
    val name: String? = null,
    val hatchedAtMillis: Long? = null,
    /** Odometer when the egg hatched. A friend hatched in a car with km shows the road it came. */
    val hatchedAtKm: Float? = null,
    /** Last age (years) and stage already celebrated, so each is celebrated once. */
    val celebratedAge: Int = 0,
    val celebratedStage: Int = 0,
)

/** A friend from an earlier body, resting in the friend box with the km it reached. */
data class RestingFriend(
    val speciesKey: String,
    val name: String,
    val hatchedAtMillis: Long,
    val finalKm: Float,
    val restedAtMillis: Long,
)

/** Keeps the friends on the device only. */
class PetProfileStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("pet_profile", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(readProfile())
    val profile: StateFlow<PetProfile> = _profile.asStateFlow()

    private val _resting = MutableStateFlow(readResting())
    val resting: StateFlow<List<RestingFriend>> = _resting.asStateFlow()

    private val _drive = MutableStateFlow(readDrive())
    val drive: StateFlow<DriveLog> = _drive.asStateFlow()

    /** The egg hatches: the drawn species and the name it was given, saved together. */
    fun hatch(speciesKey: String, name: String, nowMillis: Long, odometerKm: Float?, ageYears: Int, stage: Int) {
        prefs.edit()
            .putString(KEY_SPECIES, speciesKey)
            .putString(KEY_NAME, name)
            .putLong(KEY_HATCHED_AT, nowMillis)
            .apply { if (odometerKm != null) putFloat(KEY_HATCHED_KM, odometerKm) else remove(KEY_HATCHED_KM) }
            .putInt(KEY_CELEBRATED_AGE, ageYears)
            .putInt(KEY_CELEBRATED_STAGE, stage)
            .apply()
        _profile.value = readProfile()
    }

    fun setSpecies(key: String) {
        prefs.edit().putString(KEY_SPECIES, key).apply()
        _profile.value = readProfile()
    }

    fun markCelebrated(ageYears: Int, stage: Int) {
        prefs.edit().putInt(KEY_CELEBRATED_AGE, ageYears).putInt(KEY_CELEBRATED_STAGE, stage).apply()
        _profile.value = readProfile()
    }

    /** The friend moves to the friend box (a new body is coming); the current slot empties. */
    fun rest(finalKm: Float, nowMillis: Long) {
        val p = _profile.value
        val species = p.speciesKey
        val name = p.name
        if (species != null && name != null) {
            val friend = RestingFriend(species, name, p.hatchedAtMillis ?: nowMillis, finalKm, nowMillis)
            writeResting(_resting.value + friend)
        }
        clearCurrent()
    }

    fun recordDrive(log: DriveLog) {
        prefs.edit()
            .apply { log.dayEpoch?.let { putLong(KEY_DAY, it) } }
            .apply { log.dayStartKm?.let { putFloat(KEY_DAY_START_KM, it) } }
            .apply { log.lastDriveEpoch?.let { putLong(KEY_LAST_DRIVE, it) } }
            .apply { log.lastOdometerKm?.let { putFloat(KEY_LAST_ODO, it) } }
            .apply()
        _drive.value = readDrive()
    }

    /** Demo: start over from the egg, keeping nobody. */
    fun clearAll() {
        prefs.edit().clear().apply()
        _profile.value = readProfile()
        _resting.value = readResting()
        _drive.value = readDrive()
    }

    private fun clearCurrent() {
        prefs.edit()
            .remove(KEY_SPECIES).remove(KEY_NAME).remove(KEY_HATCHED_AT).remove(KEY_HATCHED_KM)
            .remove(KEY_CELEBRATED_AGE).remove(KEY_CELEBRATED_STAGE)
            .apply()
        _profile.value = readProfile()
    }

    private fun readProfile() = PetProfile(
        // Profiles from before the draw existed had only the fox.
        speciesKey = prefs.getString(KEY_SPECIES, null) ?: prefs.getString(KEY_NAME, null)?.let { "fox" },
        name = prefs.getString(KEY_NAME, null),
        hatchedAtMillis = prefs.getLong(KEY_HATCHED_AT, -1L).takeIf { it >= 0 },
        hatchedAtKm = if (prefs.contains(KEY_HATCHED_KM)) prefs.getFloat(KEY_HATCHED_KM, 0f) else null,
        celebratedAge = prefs.getInt(KEY_CELEBRATED_AGE, 0),
        celebratedStage = prefs.getInt(KEY_CELEBRATED_STAGE, 0),
    )

    private fun readDrive() = DriveLog(
        dayEpoch = prefs.getLong(KEY_DAY, -1L).takeIf { it >= 0 },
        dayStartKm = if (prefs.contains(KEY_DAY_START_KM)) prefs.getFloat(KEY_DAY_START_KM, 0f) else null,
        lastDriveEpoch = prefs.getLong(KEY_LAST_DRIVE, -1L).takeIf { it >= 0 },
        lastOdometerKm = if (prefs.contains(KEY_LAST_ODO)) prefs.getFloat(KEY_LAST_ODO, 0f) else null,
    )

    private fun readResting(): List<RestingFriend> {
        val raw = prefs.getString(KEY_RESTING, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RestingFriend(
                    speciesKey = o.getString("species"),
                    name = o.getString("name"),
                    hatchedAtMillis = o.getLong("hatchedAt"),
                    finalKm = o.getDouble("finalKm").toFloat(),
                    restedAtMillis = o.getLong("restedAt"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeResting(list: List<RestingFriend>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(
                JSONObject()
                    .put("species", f.speciesKey)
                    .put("name", f.name)
                    .put("hatchedAt", f.hatchedAtMillis)
                    .put("finalKm", f.finalKm.toDouble())
                    .put("restedAt", f.restedAtMillis),
            )
        }
        prefs.edit().putString(KEY_RESTING, arr.toString()).apply()
        _resting.value = list
    }

    private companion object {
        const val KEY_SPECIES = "species"
        const val KEY_NAME = "name"
        const val KEY_HATCHED_AT = "hatched_at"
        const val KEY_HATCHED_KM = "hatched_km"
        const val KEY_CELEBRATED_AGE = "celebrated_age"
        const val KEY_CELEBRATED_STAGE = "celebrated_stage"
        const val KEY_RESTING = "resting"
        const val KEY_DAY = "day_epoch"
        const val KEY_DAY_START_KM = "day_start_km"
        const val KEY_LAST_DRIVE = "last_drive_epoch"
        const val KEY_LAST_ODO = "last_odometer_km"
    }
}
