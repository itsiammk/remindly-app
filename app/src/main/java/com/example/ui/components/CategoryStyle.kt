package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.domain.model.ReminderCategory
import com.example.ui.theme.LocalRemindlyPalette

data class CategoryVisualStyle(
  val label: String,
  val icon: ImageVector,
  val containerColor: Color,
  val contentColor: Color,
)

@Composable
fun categoryStyle(category: ReminderCategory): CategoryVisualStyle {
  val palette = LocalRemindlyPalette.current
  val pair = palette.categoryTints[category] ?: (palette.primarySoft to palette.primaryAccent)
  return CategoryVisualStyle(
    label = categoryLabel(category),
    icon = categoryIcon(category),
    containerColor = pair.first,
    contentColor = pair.second,
  )
}

fun categoryLabel(category: ReminderCategory): String =
  when (category) {
    ReminderCategory.BIRTHDAY -> "Birthday"
    ReminderCategory.ANNIVERSARY -> "Anniversary"
    ReminderCategory.BILL -> "Bill"
    ReminderCategory.MEDICINE -> "Medicine"
    ReminderCategory.OTHER -> "Other"
  }

fun categoryIcon(category: ReminderCategory): ImageVector =
  when (category) {
    ReminderCategory.BIRTHDAY -> Icons.Outlined.Cake
    ReminderCategory.ANNIVERSARY -> Icons.Outlined.FavoriteBorder
    ReminderCategory.BILL -> Icons.Outlined.ReceiptLong
    ReminderCategory.MEDICINE -> Icons.Outlined.Medication
    ReminderCategory.OTHER -> Icons.Outlined.EventNote
  }
