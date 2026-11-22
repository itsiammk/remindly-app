package com.example

import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderCategory
import com.example.security.AppLockManager
import com.example.ui.localization.stringsForLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5FeaturesTest {

  @Test
  fun `app lock pin hashing uses unique salt and verifies accurately`() {
    val hash1 = AppLockManager.hashPin("4829")
    val hash2 = AppLockManager.hashPin("4829")

    assertNotEquals(hash1, hash2)
    assertTrue(AppLockManager.verifyPin("4829", hash1))
    assertTrue(AppLockManager.verifyPin("4829", hash2))
    assertFalse(AppLockManager.verifyPin("1234", hash1))
    assertFalse(AppLockManager.verifyPin("482", hash1))
  }

  @Test
  fun `english and hindi strings provide distinct translations for all buckets categories and history events`() {
    val en = stringsForLanguage(AppLanguage.ENGLISH)
    val hi = stringsForLanguage(AppLanguage.HINDI)

    assertEquals("Today", en.bucketTitle(ReminderBucket.TODAY))
    assertEquals("आज", hi.bucketTitle(ReminderBucket.TODAY))

    ReminderBucket.entries.forEach { bucket ->
      assertTrue(en.bucketTitle(bucket).isNotBlank())
      assertTrue(hi.bucketTitle(bucket).isNotBlank())
      assertNotEquals(en.bucketTitle(bucket), hi.bucketTitle(bucket))
    }

    ReminderCategory.entries.forEach { cat ->
      assertTrue(en.categoryName(cat).isNotBlank())
      assertTrue(hi.categoryName(cat).isNotBlank())
      assertNotEquals(en.categoryName(cat), hi.categoryName(cat))
    }

    HistoryEventType.entries.forEach { evt ->
      assertTrue(en.historyEventLabel(evt).isNotBlank())
      assertTrue(hi.historyEventLabel(evt).isNotBlank())
      assertNotEquals(en.historyEventLabel(evt), hi.historyEventLabel(evt))
    }
  }

  @Test
  fun `theme mode fromPersisted migrates legacy values and preserves all redesigned themes`() {
    assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromPersisted(null))
    assertEquals(AppThemeMode.MONOCHROME_LIGHT, AppThemeMode.fromPersisted("LIGHT"))
    assertEquals(AppThemeMode.AURORA, AppThemeMode.fromPersisted("DARK"))
    assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromPersisted("NON_EXISTENT_THEME"))

    AppThemeMode.entries.forEach { mode ->
      assertEquals(mode, AppThemeMode.fromPersisted(mode.name))
    }
  }
}
