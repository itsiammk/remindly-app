package com.example.backup

import android.util.Base64
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class RemindBackupPayload(
  val version: Int,
  val exportedAtMillis: Long,
  val timezone: String,
  val reminders: List<Reminder>,
)

data class EncryptedRemindEnvelope(
  val version: Int,
  val exportedAtMillis: Long,
  val timezone: String,
  val kdf: String,
  val iterations: Int,
  val saltBase64: String,
  val ivBase64: String,
  val ciphertextBase64: String,
)

sealed interface RemindParseResult {
  data class Success(val payload: RemindBackupPayload, val wasEncrypted: Boolean) : RemindParseResult

  data class PasswordRequired(val envelope: EncryptedRemindEnvelope) : RemindParseResult

  data object WrongPassword : RemindParseResult

  data class InvalidFile(val message: String) : RemindParseResult
}

object RemindBackupCodec {

  const val FORMAT_ID = "com.remindly.backup"
  const val CURRENT_VERSION = 1
  const val TIMEZONE_IST = "Asia/Kolkata"

  private const val KDF_SHA256 = "PBKDF2WithHmacSHA256"
  private const val KDF_SHA1 = "PBKDF2WithHmacSHA1"
  private const val KDF_ITERATIONS = 100_000
  private const val AES_KEY_BITS = 256
  private const val GCM_TAG_BITS = 128

  fun encode(
    reminders: List<Reminder>,
    password: String? = null,
    exportedAtMillis: Long = System.currentTimeMillis(),
  ): String {
    val remindersArray = JSONArray()
    reminders.forEach { remindersArray.put(reminderToJson(it)) }

    val cleanPassword = password?.takeIf { it.isNotEmpty() }
    val root =
      JSONObject().apply {
        put("format", FORMAT_ID)
        put("version", CURRENT_VERSION)
        put("exportedAtMillis", exportedAtMillis)
        put("timezone", TIMEZONE_IST)
      }

    if (cleanPassword == null) {
      root.put("encrypted", false)
      root.put("reminders", remindersArray)
    } else {
      val innerJson = JSONObject().apply { put("reminders", remindersArray) }.toString()
      val random = SecureRandom()
      val salt = ByteArray(16).also { random.nextBytes(it) }
      val iv = ByteArray(12).also { random.nextBytes(it) }
      val (kdfUsed, secretKey) = deriveAesKey(cleanPassword, salt, KDF_ITERATIONS, preferredKdf = null)

      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
      val encryptedBytes = cipher.doFinal(innerJson.toByteArray(Charsets.UTF_8))

      root.put("encrypted", true)
      root.put("kdf", kdfUsed)
      root.put("iterations", KDF_ITERATIONS)
      root.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
      root.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
      root.put("ciphertext", Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
    }

    return root.toString(2)
  }

  fun parse(rawContent: String): RemindParseResult {
    val trimmed = rawContent.trim()
    if (trimmed.isEmpty()) {
      return RemindParseResult.InvalidFile("This file is empty.")
    }

    val root =
      runCatching { JSONObject(trimmed) }.getOrNull()
        ?: return RemindParseResult.InvalidFile("Not a valid .remind backup file.")

    val format = root.optString("format", "")
    if (format != FORMAT_ID) {
      return RemindParseResult.InvalidFile("Unrecognized file format. Choose a .remind export from Remindly.")
    }

    val version = root.optInt("version", -1)
    if (version < 1 || version > CURRENT_VERSION) {
      return RemindParseResult.InvalidFile("Backup version $version is not supported by this version of the app.")
    }

    val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())
    val timezone = root.optString("timezone", TIMEZONE_IST)
    val encrypted = root.optBoolean("encrypted", false)

    if (encrypted) {
      val kdf = root.optString("kdf", KDF_SHA256)
      val iterations = root.optInt("iterations", KDF_ITERATIONS).coerceIn(1_000, 500_000)
      val salt = root.optString("salt", "")
      val iv = root.optString("iv", "")
      val ciphertext = root.optString("ciphertext", "")
      if (salt.isBlank() || iv.isBlank() || ciphertext.isBlank()) {
        return RemindParseResult.InvalidFile("Encrypted backup metadata is incomplete or corrupted.")
      }
      return RemindParseResult.PasswordRequired(
        EncryptedRemindEnvelope(
          version = version,
          exportedAtMillis = exportedAtMillis,
          timezone = timezone,
          kdf = kdf,
          iterations = iterations,
          saltBase64 = salt,
          ivBase64 = iv,
          ciphertextBase64 = ciphertext,
        )
      )
    }

    val remindersArray =
      root.optJSONArray("reminders")
        ?: return RemindParseResult.InvalidFile("Backup does not contain any reminders list.")

    val parsedReminders = parseRemindersArray(remindersArray)
      ?: return RemindParseResult.InvalidFile("Backup contains invalid reminder entries.")

    if (parsedReminders.isEmpty()) {
      return RemindParseResult.InvalidFile("This backup contains zero valid reminders.")
    }

    return RemindParseResult.Success(
      payload =
        RemindBackupPayload(
          version = version,
          exportedAtMillis = exportedAtMillis,
          timezone = timezone,
          reminders = parsedReminders,
        ),
      wasEncrypted = false,
    )
  }

  fun decrypt(envelope: EncryptedRemindEnvelope, password: String): RemindParseResult {
    if (password.isEmpty()) return RemindParseResult.WrongPassword

    return try {
      val salt = Base64.decode(envelope.saltBase64, Base64.DEFAULT)
      val iv = Base64.decode(envelope.ivBase64, Base64.DEFAULT)
      val ciphertext = Base64.decode(envelope.ciphertextBase64, Base64.DEFAULT)

      val (_, secretKey) =
        deriveAesKey(password, salt, envelope.iterations, preferredKdf = envelope.kdf)

      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
      val plainJson = String(cipher.doFinal(ciphertext), Charsets.UTF_8)

      val inner = JSONObject(plainJson)
      val remindersArray =
        inner.optJSONArray("reminders")
          ?: return RemindParseResult.InvalidFile("Decrypted payload has no reminders list.")
      val parsedReminders =
        parseRemindersArray(remindersArray)
          ?: return RemindParseResult.InvalidFile("Decrypted payload contains invalid entries.")

      if (parsedReminders.isEmpty()) {
        return RemindParseResult.InvalidFile("This backup contains zero valid reminders.")
      }

      RemindParseResult.Success(
        payload =
          RemindBackupPayload(
            version = envelope.version,
            exportedAtMillis = envelope.exportedAtMillis,
            timezone = envelope.timezone,
            reminders = parsedReminders,
          ),
        wasEncrypted = true,
      )
    } catch (_: javax.crypto.AEADBadTagException) {
      RemindParseResult.WrongPassword
    } catch (_: java.security.GeneralSecurityException) {
      RemindParseResult.WrongPassword
    } catch (_: Exception) {
      RemindParseResult.InvalidFile("Could not read encrypted backup.")
    }
  }

  private fun deriveAesKey(
    password: String,
    salt: ByteArray,
    iterations: Int,
    preferredKdf: String?,
  ): Pair<String, SecretKeySpec> {
    val algorithm =
      if (preferredKdf != null) {
        preferredKdf
      } else {
        try {
          SecretKeyFactory.getInstance(KDF_SHA256)
          KDF_SHA256
        } catch (_: Exception) {
          KDF_SHA1
        }
      }

    val factory =
      try {
        SecretKeyFactory.getInstance(algorithm)
      } catch (_: Exception) {
        SecretKeyFactory.getInstance(KDF_SHA1)
      }
    val spec = PBEKeySpec(password.toCharArray(), salt, iterations, AES_KEY_BITS)
    val bytes = factory.generateSecret(spec).encoded
    return algorithm to SecretKeySpec(bytes, "AES")
  }

  private fun reminderToJson(reminder: Reminder): JSONObject {
    val offsetsJson = JSONArray()
    reminder.offsets.forEach { offset ->
      offsetsJson.put(
        JSONObject().apply {
          put("offsetType", offset.offsetType.name)
          put("offsetValue", offset.offsetValue)
          if (offset.exactEpochMillis != null) {
            put("exactEpochMillis", offset.exactEpochMillis)
          }
          put("deliveryChannel", offset.deliveryChannel.name)
          put("isEnabled", offset.isEnabled)
        }
      )
    }

    return JSONObject().apply {
      put("uuid", reminder.uuid)
      put("title", reminder.title)
      put("note", reminder.note)
      put("category", reminder.category.name)
      put("anchorYear", reminder.anchorYear)
      put("anchorMonth", reminder.anchorMonth)
      put("anchorDay", reminder.anchorDay)
      put("yearKnown", reminder.yearKnown)
      put("hasExplicitTime", reminder.hasExplicitTime)
      put("hour", reminder.hour)
      put("minute", reminder.minute)
      put("repeatRule", reminder.repeatRule.name)
      put("repeatInterval", reminder.repeatInterval)
      put("repeatDaysOfWeekMask", reminder.repeatDaysOfWeekMask)
      put("isActive", reminder.isActive)
      put("isCompleted", reminder.isCompleted)
      put("nextOccurrenceEpochMillis", reminder.nextOccurrenceEpochMillis)
      if (reminder.lastCompletedOccurrenceEpochMillis != null) {
        put("lastCompletedOccurrenceEpochMillis", reminder.lastCompletedOccurrenceEpochMillis)
      }
      if (reminder.activeOverrideOriginalEpochMillis != null) {
        put("activeOverrideOriginalEpochMillis", reminder.activeOverrideOriginalEpochMillis)
      }
      put("offsets", offsetsJson)
    }
  }

  private fun parseRemindersArray(array: JSONArray): List<Reminder>? {
    val list = mutableListOf<Reminder>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: return null
      val title = obj.optString("title", "").trim()
      if (title.isEmpty()) continue

      val uuid = obj.optString("uuid", "").trim().ifEmpty { UUID.randomUUID().toString() }
      val note = obj.optString("note", "")
      val category =
        runCatching { ReminderCategory.valueOf(obj.optString("category", "OTHER")) }
          .getOrDefault(ReminderCategory.OTHER)
      val anchorYear = obj.optInt("anchorYear", 2026).coerceIn(1900, 2200)
      val anchorMonth = obj.optInt("anchorMonth", 1).coerceIn(1, 12)
      val maxDay = com.example.domain.time.IstDate.lastDayOfMonth(anchorYear, anchorMonth)
      val anchorDay = obj.optInt("anchorDay", 1).coerceIn(1, maxDay)
      val yearKnown = obj.optBoolean("yearKnown", true)
      val hasExplicitTime = obj.optBoolean("hasExplicitTime", false)
      val hour = obj.optInt("hour", 0).coerceIn(0, 23)
      val minute = obj.optInt("minute", 0).coerceIn(0, 59)
      val repeatRule =
        runCatching { RepeatRuleType.valueOf(obj.optString("repeatRule", "NONE")) }
          .getOrDefault(RepeatRuleType.NONE)
      val repeatInterval = obj.optInt("repeatInterval", 1).coerceIn(1, 365)
      val repeatDaysMask = obj.optInt("repeatDaysOfWeekMask", 0)
      val isActive = obj.optBoolean("isActive", true)
      val isCompleted = obj.optBoolean("isCompleted", false)
      val nextOccurrence = obj.optLong("nextOccurrenceEpochMillis", 0L)
      val lastCompleted =
        if (obj.has("lastCompletedOccurrenceEpochMillis") && !obj.isNull("lastCompletedOccurrenceEpochMillis")) {
          obj.optLong("lastCompletedOccurrenceEpochMillis")
        } else {
          null
        }
      val activeOverride =
        if (obj.has("activeOverrideOriginalEpochMillis") && !obj.isNull("activeOverrideOriginalEpochMillis")) {
          obj.optLong("activeOverrideOriginalEpochMillis")
        } else {
          null
        }

      val offsetsArr = obj.optJSONArray("offsets")
      val parsedOffsets = mutableListOf<ReminderOffset>()
      if (offsetsArr != null) {
        for (j in 0 until offsetsArr.length()) {
          val offObj = offsetsArr.optJSONObject(j) ?: continue
          val offType =
            runCatching { OffsetType.valueOf(offObj.optString("offsetType", "")) }.getOrNull()
              ?: continue
          val offVal = offObj.optInt("offsetValue", 0).coerceAtLeast(0)
          val exactMs =
            if (offObj.has("exactEpochMillis") && !offObj.isNull("exactEpochMillis")) {
              offObj.optLong("exactEpochMillis")
            } else {
              null
            }
          val channel =
            runCatching { DeliveryChannel.valueOf(offObj.optString("deliveryChannel", "NOTIFICATION")) }
              .getOrDefault(DeliveryChannel.NOTIFICATION)
          val enabled = offObj.optBoolean("isEnabled", true)
          parsedOffsets +=
            ReminderOffset(
              offsetType = offType,
              offsetValue = offVal,
              exactEpochMillis = exactMs,
              deliveryChannel = channel,
              isEnabled = enabled,
            )
        }
      }

      list +=
        Reminder(
          id = 0L,
          uuid = uuid,
          title = title,
          note = note,
          category = category,
          anchorYear = anchorYear,
          anchorMonth = anchorMonth,
          anchorDay = anchorDay,
          yearKnown = yearKnown,
          hasExplicitTime = hasExplicitTime,
          hour = hour,
          minute = minute,
          repeatRule = repeatRule,
          repeatInterval = repeatInterval,
          repeatDaysOfWeekMask = repeatDaysMask,
          isActive = isActive,
          isCompleted = isCompleted,
          nextOccurrenceEpochMillis = nextOccurrence,
          lastCompletedOccurrenceEpochMillis = lastCompleted,
          activeOverrideOriginalEpochMillis = activeOverride,
          offsets = parsedOffsets,
        )
    }
    return list
  }
}
