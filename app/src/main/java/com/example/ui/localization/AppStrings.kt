package com.example.ui.localization

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.domain.model.AppLanguage
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderCategory
import com.example.domain.model.RepeatRuleType
import java.util.Locale

data class RemindlyStrings(
  val appName: String,
  val tagline: String,
  val back: String,
  val settingsTitle: String,
  val addReminder: String,
  val searchReminders: String,
  val searchPlaceholder: String,
  val clearSearch: String,
  val sortLabel: String,
  val filterAll: String,
  val fix: String,
  val alarmPermissionBanner: String,
  val emptyTitle: String,
  val emptySubtitle: String,
  val emptySearchTitle: String,
  val emptySearchSubtitle: String,
  val clearFilters: String,
  val createFirstReminder: String,
  val saveReminder: String,
  val updateReminder: String,
  val newReminder: String,
  val editReminder: String,
  val done: String,
  val pause: String,
  val resume: String,
  val edit: String,
  val delete: String,
  val undo: String,
  val cancel: String,
  val restore: String,
  val deleteForever: String,
  val skipOccurrence: String,
  val moveOccurrence: String,
  val missedBadge: String,
  val pausedBadge: String,
  val movedBadge: String,
  val alarmLabel: String,
  val notifyLabel: String,
  val sectionActivityAndRecovery: String,
  val historyTitle: String,
  val historySubtitle: String,
  val historyEmptyTitle: String,
  val historyEmptySubtitle: String,
  val clearHistory: String,
  val clearHistoryConfirmTitle: String,
  val clearHistoryConfirmBody: String,
  val recycleBinTitle: String,
  val recycleBinEmptySubtitle: String,
  val recycleBinCountSubtitle: (Int) -> String,
  val recycleBinEmptyStateTitle: String,
  val recycleBinEmptyStateBody: String,
  val emptyBinAction: String,
  val emptyBinConfirmTitle: String,
  val emptyBinConfirmBody: String,
  val permanentDeleteTitle: String,
  val permanentDeleteBody: (String) -> String,
  val deletedAtPrefix: String,
  val sectionSecurityAndLanguage: String,
  val languageLabel: String,
  val languageSubtitle: String,
  val appLockLabel: String,
  val appLockEnabledSubtitle: (Boolean) -> String,
  val appLockDisabledSubtitle: String,
  val setPinTitle: String,
  val enterNewPinHint: String,
  val confirmPinHint: String,
  val pinMismatchError: String,
  val pinTooShortError: String,
  val enableAppLock: String,
  val changePin: String,
  val disableAppLock: String,
  val useFingerprintFace: String,
  val lockScreenTitle: String,
  val lockScreenSubtitle: String,
  val lockScreenWrongPin: String,
  val unlockWithBiometric: String,
  val usePinButton: String,
  val lockTimeoutLabel: String,
  val lockTimeoutImmediate: String,
  val lockTimeout1Min: String,
  val lockTimeout5Min: String,
  val homeWidgetSectionTitle: String,
  val homeWidgetTitle: String,
  val homeWidgetSubtitle: String,
  val pinWidgetButton: String,
  val widgetPinUnsupported: String,
  val widgetPinRequested: String,
  val widgetRefreshDone: String,
  val widgetTitle: String,
  val widgetEmpty: String,
  val reliabilitySectionTitle: String,
  val reliabilityRowTitle: String,
  val backupSectionTitle: String,
  val backupRowTitle: String,
  val alarmsAndSoundsSectionTitle: String,
  val alarmRingtoneLabel: String,
  val vibrationPatternLabel: String,
  val snoozeDurationLabel: String,
  val bypassDndLabel: String,
  val themePreviewTitle: String,
  val themeModeTitle: String,
  val accentColorTitle: String,
  val defaultsSectionTitle: String,
  val replayWalkthroughTitle: String,
  val offlineBadgeText: String,
  val navHome: String,
  val navCalendar: String,
  val navSettings: String,
  val nextUpLabel: String,
  val slideToDone: String,
  val lowPowerVisualsTitle: String,
  val lowPowerVisualsSubtitle: String,
  val calendarEmptyDay: String,
  val selectedCountLabel: (Int) -> String,
  val previewTimelineTitle: String,
) {
  fun bucketTitle(bucket: ReminderBucket): String =
    when (this) {
      HindiStrings ->
        when (bucket) {
          ReminderBucket.MISSED -> "छूट गए"
          ReminderBucket.TODAY -> "आज"
          ReminderBucket.TOMORROW -> "कल"
          ReminderBucket.THIS_WEEK -> "इस सप्ताह"
          ReminderBucket.LATER -> "बाद में"
          ReminderBucket.PAUSED -> "रोके गए"
        }
      else ->
        when (bucket) {
          ReminderBucket.MISSED -> "Missed"
          ReminderBucket.TODAY -> "Today"
          ReminderBucket.TOMORROW -> "Tomorrow"
          ReminderBucket.THIS_WEEK -> "This Week"
          ReminderBucket.LATER -> "Later"
          ReminderBucket.PAUSED -> "Paused"
        }
    }

  fun categoryName(category: ReminderCategory): String =
    when (this) {
      HindiStrings ->
        when (category) {
          ReminderCategory.BIRTHDAY -> "जन्मदिन"
          ReminderCategory.ANNIVERSARY -> "सालगिरह"
          ReminderCategory.BILL -> "बिल"
          ReminderCategory.MEDICINE -> "दवाई"
          ReminderCategory.OTHER -> "अन्य"
        }
      else ->
        when (category) {
          ReminderCategory.BIRTHDAY -> "Birthday"
          ReminderCategory.ANNIVERSARY -> "Anniversary"
          ReminderCategory.BILL -> "Bill"
          ReminderCategory.MEDICINE -> "Medicine"
          ReminderCategory.OTHER -> "Other"
        }
    }

  fun repeatName(rule: RepeatRuleType): String =
    when (this) {
      HindiStrings ->
        when (rule) {
          RepeatRuleType.NONE -> "दोहराव नहीं"
          RepeatRuleType.DAILY -> "रोज़ाना"
          RepeatRuleType.WEEKLY -> "साप्ताहिक"
          RepeatRuleType.MONTHLY -> "मासिक"
          RepeatRuleType.YEARLY -> "वार्षिक"
          RepeatRuleType.EVERY_N_DAYS -> "हर N दिन में"
        }
      else ->
        when (rule) {
          RepeatRuleType.NONE -> "One-time"
          RepeatRuleType.DAILY -> "Daily"
          RepeatRuleType.WEEKLY -> "Weekly"
          RepeatRuleType.MONTHLY -> "Monthly"
          RepeatRuleType.YEARLY -> "Yearly"
          RepeatRuleType.EVERY_N_DAYS -> "Every N days"
        }
    }

  fun historyEventLabel(eventType: HistoryEventType): String =
    when (this) {
      HindiStrings ->
        when (eventType) {
          HistoryEventType.FIRED_NOTIFICATION -> "नोटिफिकेशन भेजा गया"
          HistoryEventType.FIRED_ALARM -> "अलार्म बजा"
          HistoryEventType.COMPLETED -> "पूरा हुआ"
          HistoryEventType.SNOOZED -> "स्नूज़ किया गया"
          HistoryEventType.SKIPPED -> "इस बार छोड़ा गया"
          HistoryEventType.MOVED -> "समय बदला गया"
          HistoryEventType.MISSED -> "छूट गया"
        }
      else ->
        when (eventType) {
          HistoryEventType.FIRED_NOTIFICATION -> "Notification sent"
          HistoryEventType.FIRED_ALARM -> "Alarm rang"
          HistoryEventType.COMPLETED -> "Marked done"
          HistoryEventType.SNOOZED -> "Snoozed"
          HistoryEventType.SKIPPED -> "Skipped once"
          HistoryEventType.MOVED -> "Moved once"
          HistoryEventType.MISSED -> "Missed"
        }
    }
}

val EnglishStrings =
  RemindlyStrings(
    appName = "Remindly",
    tagline = "Never miss what matters",
    back = "Back",
    settingsTitle = "Settings & Defaults",
    addReminder = "Add Reminder",
    searchReminders = "Search reminders",
    searchPlaceholder = "Search reminders or notes…",
    clearSearch = "Clear search",
    sortLabel = "Sort",
    filterAll = "All",
    fix = "Fix",
    alarmPermissionBanner = "Alarm permission needed so reminders ring on time",
    emptyTitle = "Nothing scheduled yet",
    emptySubtitle = "Tap + to add a birthday, bill, medicine, or important date in seconds.",
    emptySearchTitle = "No matching reminders",
    emptySearchSubtitle = "Try a different keyword or clear your category filter.",
    clearFilters = "Clear filters",
    createFirstReminder = "Create your first reminder",
    saveReminder = "Save Reminder",
    updateReminder = "Update Reminder",
    newReminder = "New Reminder",
    editReminder = "Edit Reminder",
    done = "Done",
    pause = "Pause",
    resume = "Resume",
    edit = "Edit",
    delete = "Delete",
    undo = "Undo",
    cancel = "Cancel",
    restore = "Restore",
    deleteForever = "Delete forever",
    skipOccurrence = "Skip this occurrence",
    moveOccurrence = "Move this occurrence",
    missedBadge = "MISSED",
    pausedBadge = "Paused",
    movedBadge = "Moved",
    alarmLabel = "Alarm",
    notifyLabel = "Notify",
    sectionActivityAndRecovery = "Activity & Recovery",
    historyTitle = "Firing & Action History",
    historySubtitle = "See when alerts rang, completed, snoozed, or were missed",
    historyEmptyTitle = "No reminder activity yet",
    historyEmptySubtitle = "Alerts, completions, snoozes, and skips will appear here automatically.",
    clearHistory = "Clear log",
    clearHistoryConfirmTitle = "Clear history log?",
    clearHistoryConfirmBody = "This removes all past firing records. Your active reminders are unaffected.",
    recycleBinTitle = "Recycle Bin",
    recycleBinEmptySubtitle = "Deleted reminders can be restored here",
    recycleBinCountSubtitle = { count -> "$count deleted ${if (count == 1) "reminder" else "reminders"} • Tap to restore" },
    recycleBinEmptyStateTitle = "Recycle Bin is empty",
    recycleBinEmptyStateBody = "When you delete a reminder, it stays here safely until you restore or remove it permanently.",
    emptyBinAction = "Empty bin",
    emptyBinConfirmTitle = "Empty Recycle Bin?",
    emptyBinConfirmBody = "All deleted reminders in the bin will be permanently removed and cannot be recovered.",
    permanentDeleteTitle = "Delete permanently?",
    permanentDeleteBody = { title -> "\"$title\" will be permanently removed and cannot be recovered." },
    deletedAtPrefix = "Deleted",
    sectionSecurityAndLanguage = "Language & Privacy Lock",
    languageLabel = "App language",
    languageSubtitle = "Switch between English and हिन्दी across screens & widget",
    appLockLabel = "App Lock (PIN / Fingerprint)",
    appLockEnabledSubtitle = { bio -> if (bio) "Locked with 4-digit PIN & Biometric" else "Locked with 4-digit PIN" },
    appLockDisabledSubtitle = "Protect your reminders when opening the app",
    setPinTitle = "Set 4-digit App PIN",
    enterNewPinHint = "Enter 4-digit PIN",
    confirmPinHint = "Confirm 4-digit PIN",
    pinMismatchError = "PINs do not match",
    pinTooShortError = "Enter a 4-digit PIN",
    enableAppLock = "Enable App Lock",
    changePin = "Change PIN",
    disableAppLock = "Turn off",
    useFingerprintFace = "Unlock with Fingerprint / Face",
    lockScreenTitle = "Remindly is Locked",
    lockScreenSubtitle = "Enter your 4-digit PIN or use fingerprint to continue",
    lockScreenWrongPin = "Incorrect PIN. Try again.",
    unlockWithBiometric = "Use Fingerprint",
    usePinButton = "Use PIN",
    lockTimeoutLabel = "Re-lock after leaving app",
    lockTimeoutImmediate = "Immediately",
    lockTimeout1Min = "1 min",
    lockTimeout5Min = "5 min",
    homeWidgetSectionTitle = "Home Screen Widget",
    homeWidgetTitle = "Remindly Upcoming Widget",
    homeWidgetSubtitle = "See Missed & Today's reminders on your phone home screen",
    pinWidgetButton = "Add to Home Screen",
    widgetPinUnsupported = "Long-press your phone home screen and add the Remindly widget.",
    widgetPinRequested = "Follow the system prompt to place the Remindly widget.",
    widgetRefreshDone = "Widget refreshed",
    widgetTitle = "Upcoming Reminders",
    widgetEmpty = "All caught up in IST",
    reliabilitySectionTitle = "Reliability & Permissions",
    reliabilityRowTitle = "Reliability Check & Phone Setup",
    backupSectionTitle = "Backup & Sharing",
    backupRowTitle = "Export, Share & Import (.remind)",
    alarmsAndSoundsSectionTitle = "Alarms & Sounds",
    alarmRingtoneLabel = "Alarm ringtone",
    vibrationPatternLabel = "Vibration pattern",
    snoozeDurationLabel = "Snooze duration",
    bypassDndLabel = "Alarms bypass Do Not Disturb",
    themePreviewTitle = "Live Preview",
    themeModeTitle = "Theme Mode",
    accentColorTitle = "Accent Palette",
    defaultsSectionTitle = "Defaults for New Reminders",
    replayWalkthroughTitle = "Replay welcome walkthrough",
    offlineBadgeText = "100% Offline • All dates in IST (Asia/Kolkata)",
    navHome = "Home",
    navCalendar = "Calendar",
    navSettings = "Settings",
    nextUpLabel = "Next up",
    slideToDone = "Slide to done",
    lowPowerVisualsTitle = "Low power visuals",
    lowPowerVisualsSubtitle = "Disable blur & ambient motion to save battery",
    calendarEmptyDay = "Nothing scheduled on this day",
    selectedCountLabel = { count -> "$count selected" },
    previewTimelineTitle = "Scheduled alert timeline",
  )

val HindiStrings =
  RemindlyStrings(
    appName = "Remindly",
    tagline = "जो ज़रूरी है, उसे कभी न भूलें",
    back = "वापस",
    settingsTitle = "सेटिंग्स और डिफ़ॉल्ट",
    addReminder = "रिमाइंडर जोड़ें",
    searchReminders = "रिमाइंडर खोजें",
    searchPlaceholder = "रिमाइंडर या नोट्स खोजें…",
    clearSearch = "खोज साफ़ करें",
    sortLabel = "क्रम",
    filterAll = "सभी",
    fix = "ठीक करें",
    alarmPermissionBanner = "समय पर अलार्म बजने के लिए अनुमति ज़रूरी है",
    emptyTitle = "अभी कोई रिमाइंडर नहीं",
    emptySubtitle = "जन्मदिन, बिल, दवाई या खास तारीख सेकंडों में जोड़ने के लिए + दबाएं।",
    emptySearchTitle = "कोई मिलता रिमाइंडर नहीं",
    emptySearchSubtitle = "दूसरा शब्द आज़माएं या श्रेणी फ़िल्टर हटाएं।",
    clearFilters = "फ़िल्टर हटाएं",
    createFirstReminder = "पहला रिमाइंडर बनाएं",
    saveReminder = "रिमाइंडर सहेजें",
    updateReminder = "रिमाइंडर अपडेट करें",
    newReminder = "नया रिमाइंडर",
    editReminder = "रिमाइंडर बदलें",
    done = "पूरा हुआ",
    pause = "रोकें",
    resume = "फिर शुरू करें",
    edit = "बदलें",
    delete = "हटाएं",
    undo = "वापस लाएं",
    cancel = "रद्द करें",
    restore = "वापस लाएं",
    deleteForever = "हमेशा के लिए हटाएं",
    skipOccurrence = "सिर्फ़ इस बार छोड़ें",
    moveOccurrence = "सिर्फ़ इस बार समय बदलें",
    missedBadge = "छूट गया",
    pausedBadge = "रोका गया",
    movedBadge = "बदला गया",
    alarmLabel = "अलार्म",
    notifyLabel = "सूचना",
    sectionActivityAndRecovery = "गतिविधि और रीसायकल बिन",
    historyTitle = "अलर्ट और गतिविधि लॉग",
    historySubtitle = "जानें कि कब अलार्म बजा, पूरा हुआ, स्नूज़ हुआ या छूटा",
    historyEmptyTitle = "अभी कोई गतिविधि नहीं",
    historyEmptySubtitle = "बजे हुए अलर्ट, पूरे किए गए कार्य और स्नूज़ यहाँ दिखेंगे।",
    clearHistory = "लॉग साफ़ करें",
    clearHistoryConfirmTitle = "इतिहास लॉग साफ़ करें?",
    clearHistoryConfirmBody = "इससे पुराने रिकॉर्ड हट जाएंगे। आपके सक्रिय रिमाइंडर सुरक्षित रहेंगे।",
    recycleBinTitle = "रीसायकल बिन",
    recycleBinEmptySubtitle = "हटाए गए रिमाइंडर यहाँ से वापस लाएं",
    recycleBinCountSubtitle = { count -> "$count हटाए गए रिमाइंडर • वापस लाने के लिए टैप करें" },
    recycleBinEmptyStateTitle = "रीसायकल बिन खाली है",
    recycleBinEmptyStateBody = "हटाए गए रिमाइंडर यहाँ सुरक्षित रहते हैं जब तक आप उन्हें वापस न लाएं या हमेशा के लिए न हटाएं।",
    emptyBinAction = "बिन खाली करें",
    emptyBinConfirmTitle = "रीसायकल बिन खाली करें?",
    emptyBinConfirmBody = "बिन के सभी रिमाइंडर हमेशा के लिए हट जाएंगे और वापस नहीं आएंगे।",
    permanentDeleteTitle = "हमेशा के लिए हटाएं?",
    permanentDeleteBody = { title -> "\"$title\" हमेशा के लिए हट जाएगा और वापस नहीं लाया जा सकेगा।" },
    deletedAtPrefix = "हटाया गया",
    sectionSecurityAndLanguage = "भाषा और ऐप लॉक",
    languageLabel = "ऐप की भाषा",
    languageSubtitle = "English और हिन्दी के बीच तुरंत बदलें",
    appLockLabel = "ऐप लॉक (PIN / फ़िंगरप्रिंट)",
    appLockEnabledSubtitle = { bio -> if (bio) "4-अंक PIN और फ़िंगरप्रिंट से सुरक्षित" else "4-अंक PIN से सुरक्षित" },
    appLockDisabledSubtitle = "ऐप खोलते समय अपने रिमाइंडर सुरक्षित रखें",
    setPinTitle = "4-अंकों का ऐप PIN सेट करें",
    enterNewPinHint = "4-अंक का PIN डालें",
    confirmPinHint = "PIN की पुष्टि करें",
    pinMismatchError = "दोनों PIN मेल नहीं खाते",
    pinTooShortError = "4-अंक का PIN दर्ज करें",
    enableAppLock = "ऐप लॉक चालू करें",
    changePin = "PIN बदलें",
    disableAppLock = "बंद करें",
    useFingerprintFace = "फ़िंगरप्रिंट / फेस से अनलॉक करें",
    lockScreenTitle = "Remindly लॉक है",
    lockScreenSubtitle = "आगे बढ़ने के लिए 4-अंक PIN डालें या फ़िंगरप्रिंट उपयोग करें",
    lockScreenWrongPin = "गलत PIN, फिर से प्रयास करें।",
    unlockWithBiometric = "फ़िंगरप्रिंट से खोलें",
    usePinButton = "PIN डालें",
    lockTimeoutLabel = "ऐप छोड़ने के बाद लॉक समय",
    lockTimeoutImmediate = "तुरंत",
    lockTimeout1Min = "1 मिनट",
    lockTimeout5Min = "5 मिनट",
    homeWidgetSectionTitle = "होम स्क्रीन विजेट",
    homeWidgetTitle = "Remindly आगामी विजेट",
    homeWidgetSubtitle = "फ़ोन की होम स्क्रीन पर आज और छूटे हुए रिमाइंडर देखें",
    pinWidgetButton = "होम स्क्रीन पर जोड़ें",
    widgetPinUnsupported = "अपने फ़ोन की होम स्क्रीन पर देर तक दबाएं और Remindly विजेट जोड़ें।",
    widgetPinRequested = "विजेट जोड़ने के लिए स्क्रीन पर दिए निर्देश का पालन करें।",
    widgetRefreshDone = "विजेट अपडेट किया गया",
    widgetTitle = "आगामी रिमाइंडर",
    widgetEmpty = "कोई बाकी रिमाइंडर नहीं (IST)",
    reliabilitySectionTitle = "भरोसेमंद अलार्म और अनुमतियाँ",
    reliabilityRowTitle = "रिलायबिलिटी जांच और फ़ोन सेटअप",
    backupSectionTitle = "बैकअप और शेयरिंग",
    backupRowTitle = "एक्सपोर्ट, शेयर और इम्पोर्ट (.remind)",
    alarmsAndSoundsSectionTitle = "अलार्म और आवाज़",
    alarmRingtoneLabel = "अलार्म रिंगटोन",
    vibrationPatternLabel = "वाइब्रेशन पैटर्न",
    snoozeDurationLabel = "स्नूज़ अवधि",
    bypassDndLabel = "Do Not Disturb में भी अलार्म बजे",
    themePreviewTitle = "लाइव झलक",
    themeModeTitle = "थीम मोड",
    accentColorTitle = "रंग पैलेट",
    defaultsSectionTitle = "नए रिमाइंडर के डिफ़ॉल्ट",
    replayWalkthroughTitle = "स्वागत परिचय फिर देखें",
    offlineBadgeText = "100% ऑफ़लाइन • सभी समय IST (Asia/Kolkata) में",
    navHome = "होम",
    navCalendar = "कैलेंडर",
    navSettings = "सेटिंग्स",
    nextUpLabel = "अगला रिमाइंडर",
    slideToDone = "पूरा करने के लिए स्लाइड करें",
    lowPowerVisualsTitle = "कम बैटरी विज़ुअल मोड",
    lowPowerVisualsSubtitle = "बैटरी बचाने के लिए ब्लर और एनिमेशन बंद करें",
    calendarEmptyDay = "इस दिन कोई रिमाइंडर नहीं है",
    selectedCountLabel = { count -> "$count चुने गए" },
    previewTimelineTitle = "अलर्ट टाइमलाइन झलक",
  )

fun stringsForLanguage(language: AppLanguage): RemindlyStrings =
  when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.HINDI -> HindiStrings
  }

val LocalStrings = compositionLocalOf { EnglishStrings }

@Composable
fun ProvideAppLanguage(
  language: AppLanguage,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalStrings provides stringsForLanguage(language)) {
    content()
  }
}

fun Context.localizedContext(language: AppLanguage): Context {
  val locale = Locale(language.tag)
  val config = Configuration(resources.configuration)
  config.setLocale(locale)
  return createConfigurationContext(config)
}
