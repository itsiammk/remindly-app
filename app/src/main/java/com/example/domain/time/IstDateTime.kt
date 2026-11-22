package com.example.domain.time

import com.example.domain.model.Weekday
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Pure Indian Standard Time (Asia/Kolkata, UTC+05:30) date/time helpers.
 * Compatible with API 24+ without desugaring overhead and completely independent of device timezone.
 */
object IstTimeZone {
  val zone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

  fun calendar(epochMillis: Long? = null): GregorianCalendar =
    GregorianCalendar(zone).apply {
      isLenient = false
      if (epochMillis != null) {
        timeInMillis = epochMillis
      }
    }
}

data class IstDate(
  val year: Int,
  val month: Int, // 1..12
  val day: Int, // 1..31
) : Comparable<IstDate> {

  init {
    require(month in 1..12) { "Invalid month: $month" }
    val maxDay = lastDayOfMonth(year, month)
    require(day in 1..maxDay) { "Invalid day $day for $year-$month" }
  }

  override fun compareTo(other: IstDate): Int {
    if (year != other.year) return year.compareTo(other.year)
    if (month != other.month) return month.compareTo(other.month)
    return day.compareTo(other.day)
  }

  fun weekday(): Weekday {
    val cal = toCalendar(0, 0)
    return Weekday.fromCalendarDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
  }

  fun plusDays(days: Int): IstDate {
    val cal = toCalendar(12, 0)
    cal.add(Calendar.DAY_OF_MONTH, days)
    return IstDate(
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun plusMonthsClamped(months: Int): IstDate {
    val totalMonths = (year * 12 + (month - 1)) + months
    val targetYear = Math.floorDiv(totalMonths, 12)
    val targetMonth = Math.floorMod(totalMonths, 12) + 1
    val clampedDay = day.coerceAtMost(lastDayOfMonth(targetYear, targetMonth))
    return IstDate(targetYear, targetMonth, clampedDay)
  }

  fun plusYearsClamped(years: Int): IstDate {
    val targetYear = year + years
    val clampedDay = day.coerceAtMost(lastDayOfMonth(targetYear, month))
    return IstDate(targetYear, month, clampedDay)
  }

  fun daysUntil(other: IstDate): Int {
    val startCal = toCalendar(0, 0)
    val endCal = other.toCalendar(0, 0)
    val diffMillis = endCal.timeInMillis - startCal.timeInMillis
    // IST has no DST, so every calendar day is exactly 86,400,000 ms
    return (diffMillis / MILLIS_PER_DAY).toInt()
  }

  fun formatShort(): String = "$day ${MONTH_NAMES[month - 1]} $year"

  fun formatWithWeekday(): String = "${weekday().shortLabel}, ${formatShort()}"

  fun formatDayMonth(): String = "$day ${MONTH_NAMES[month - 1]}"

  fun toEpochMillis(hour: Int, minute: Int): Long = toCalendar(hour, minute).timeInMillis

  private fun toCalendar(hour: Int, minute: Int): GregorianCalendar =
    GregorianCalendar(IstTimeZone.zone).apply {
      clear()
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

  companion object {
    private const val MILLIS_PER_DAY = 86_400_000L
    private val MONTH_NAMES =
      listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun today(nowMillis: Long = System.currentTimeMillis()): IstDate = fromEpochMillis(nowMillis)

    fun fromEpochMillis(epochMillis: Long): IstDate {
      val cal = IstTimeZone.calendar(epochMillis)
      return IstDate(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH),
      )
    }

    fun isLeapYear(year: Int): Boolean =
      (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    fun lastDayOfMonth(year: Int, month: Int): Int =
      when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> error("Invalid month: $month")
      }
  }
}

data class IstTime(
  val hour: Int, // 0..23
  val minute: Int, // 0..59
) : Comparable<IstTime> {

  init {
    require(hour in 0..23) { "Invalid hour: $hour" }
    require(minute in 0..59) { "Invalid minute: $minute" }
  }

  override fun compareTo(other: IstTime): Int {
    if (hour != other.hour) return hour.compareTo(other.hour)
    return minute.compareTo(other.minute)
  }

  fun format12Hour(): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour =
      when (val h = hour % 12) {
        0 -> 12
        else -> h
      }
    return "%d:%02d %s".format(displayHour, minute, amPm)
  }

  companion object {
    fun fromEpochMillis(epochMillis: Long): IstTime {
      val cal = IstTimeZone.calendar(epochMillis)
      return IstTime(
        hour = cal.get(Calendar.HOUR_OF_DAY),
        minute = cal.get(Calendar.MINUTE),
      )
    }
  }
}

data class IstDateTime(
  val date: IstDate,
  val time: IstTime,
) : Comparable<IstDateTime> {

  fun toEpochMillis(): Long = date.toEpochMillis(time.hour, time.minute)

  override fun compareTo(other: IstDateTime): Int {
    val dateCmp = date.compareTo(other.date)
    if (dateCmp != 0) return dateCmp
    return time.compareTo(other.time)
  }

  companion object {
    fun fromEpochMillis(epochMillis: Long): IstDateTime =
      IstDateTime(
        date = IstDate.fromEpochMillis(epochMillis),
        time = IstTime.fromEpochMillis(epochMillis),
      )
  }
}
