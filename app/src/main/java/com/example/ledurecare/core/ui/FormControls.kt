package com.example.ledurecare.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ledurecare.R
import com.example.ledurecare.ui.theme.ErrorRed
import com.example.ledurecare.ui.theme.FieldBg
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.Placeholder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val ControlShape = RoundedCornerShape(12.dp)

/** A label above a control, with a red asterisk when required and an optional error underneath. */
@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = buildAnnotatedString {
                append(label)
                if (required) withStyle(SpanStyle(color = ErrorRed)) { append(" *") }
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
        )
        content()
        if (error != null) FormErrorText(error)
    }
}

@Composable
fun FormErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 11.sp, color = ErrorRed)
}

/** Looks like a text field but only reacts to taps; used for pickers. */
@Composable
fun ClickableField(
    text: String?,
    placeholder: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = ControlShape,
        color = FieldBg,
        border = BorderStroke(if (isError) 1.5.dp else 1.dp, if (isError) ErrorRed else FieldBorder),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text ?: placeholder,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = if (text != null) Ink else Placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Muted)
        }
    }
}

@Composable
fun <T> DropdownField(
    selected: T?,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    placeholder: String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: ImageVector = Icons.Filled.KeyboardArrowDown,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        ClickableField(
            text = selected?.let { optionLabel(it) },
            placeholder = placeholder,
            trailingIcon = trailingIcon,
            onClick = { open = true },
            isError = isError,
            enabled = enabled,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedureDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.sheet_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.sheet_cancel)) } },
    ) { DatePicker(state = state) }
}
