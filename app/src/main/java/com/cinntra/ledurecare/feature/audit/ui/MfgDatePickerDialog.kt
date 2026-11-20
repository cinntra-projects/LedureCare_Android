package com.cinntra.ledurecare.feature.audit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.feature.audit.domain.MfgType
import com.cinntra.ledurecare.ui.theme.FieldBorder
import com.cinntra.ledurecare.ui.theme.Green600
import com.cinntra.ledurecare.ui.theme.GreenMint
import com.cinntra.ledurecare.ui.theme.Ink
import com.cinntra.ledurecare.ui.theme.Muted
import com.cinntra.ledurecare.ui.theme.Placeholder
import com.cinntra.ledurecare.ui.theme.TabTrack
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** The picked manufacturing date: a month (1-12) or a quarter (1-4) of [year]. */
data class MfgPick(val type: MfgType, val period: Int, val year: Int)

/** Text for a pick, such as "Q1 2026" or "Mar 2026". */
fun MfgPick.label(): String = "${periodLabel(type, period)} $year"

/**
 * A small calendar-style dialog that replaces the three separate dropdowns: Quarter / Month tabs, a year
 * with arrows (never later than [maxYear] or earlier than [minYear]), and a grid of periods.
 * Nothing changes until Apply; Clear empties the field.
 */
@Composable
fun MfgDatePickerDialog(
    current: MfgPick?,
    maxYear: Int,
    minYear: Int,
    onApply: (MfgPick) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(current?.type ?: MfgType.Quarter) }
    var year by remember { mutableStateOf((current?.year ?: maxYear).coerceIn(minYear, maxYear)) }
    var period by remember { mutableStateOf(current?.period) }
    val picked = period?.let { MfgPick(type, it, year) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeTabs(type) {
                    if (it != type) {
                        type = it
                        period = null // a month number and a quarter number mean different things
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YearArrow(left = true, enabled = year > minYear) { year-- }
                    Text(
                        year.toString(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                    )
                    YearArrow(left = false, enabled = year < maxYear) { year++ }
                }
                if (type == MfgType.Quarter) {
                    PeriodGrid(
                        periods = (1..4).toList(),
                        columns = 2,
                        selected = period,
                        title = { "Q$it" },
                        subtitle = { quarterRange(it, year) },
                        onSelect = { period = it },
                    )
                } else {
                    PeriodGrid(
                        periods = (1..12).toList(),
                        columns = 3,
                        selected = period,
                        title = { Month.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                        subtitle = null,
                        onSelect = { period = it },
                    )
                }
                HorizontalDivider(color = FieldBorder)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        picked?.label().orEmpty(),
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = Muted,
                    )
                    DialogButton(stringResource(R.string.audit_mfg_clear), filled = false, enabled = true, onClick = onClear)
                    DialogButton(stringResource(R.string.audit_mfg_apply), filled = true, enabled = picked != null) {
                        picked?.let(onApply)
                    }
                }
            }
        }
    }
}

private fun quarterRange(quarter: Int, year: Int): String {
    val first = Month.of((quarter - 1) * 3 + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val last = Month.of(quarter * 3).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$first - $last $year"
}

@Composable
private fun TypeTabs(selected: MfgType, onSelect: (MfgType) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(TabTrack, RoundedCornerShape(10.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(MfgType.Quarter to R.string.audit_mfg_quarter, MfgType.Month to R.string.audit_mfg_month).forEach { (t, label) ->
            val on = t == selected
            Surface(
                onClick = { onSelect(t) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = if (on) Color.White else Color.Transparent,
                shadowElevation = if (on) 1.dp else 0.dp,
            ) {
                Text(
                    stringResource(label),
                    modifier = Modifier.padding(vertical = 9.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (on) Green600 else Muted,
                )
            }
        }
    }
}

@Composable
private fun YearArrow(left: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Icon(
            if (left) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.padding(6.dp).size(20.dp),
            tint = if (enabled) Ink else Placeholder,
        )
    }
}

@Composable
private fun PeriodGrid(
    periods: List<Int>,
    columns: Int,
    selected: Int?,
    title: (Int) -> String,
    subtitle: ((Int) -> String)?,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        periods.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    val on = p == selected
                    Surface(
                        onClick = { onSelect(p) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (on) GreenMint else Color.White,
                        border = BorderStroke(1.dp, if (on) Green600 else FieldBorder),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(title(p), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Green600 else Ink)
                            subtitle?.let { Text(it(p), fontSize = 12.sp, color = Muted) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) (if (enabled) Green600 else Green600.copy(alpha = 0.4f)) else Color.White,
        border = if (filled) null else BorderStroke(1.dp, FieldBorder),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) Color.White else Ink,
        )
    }
}
