package com.cinntra.ledurecare.feature.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinntra.ledurecare.ui.theme.FieldBg
import com.cinntra.ledurecare.ui.theme.FieldBorder
import com.cinntra.ledurecare.ui.theme.Green500
import com.cinntra.ledurecare.ui.theme.GreenMint
import com.cinntra.ledurecare.ui.theme.GreenMintBorder
import com.cinntra.ledurecare.ui.theme.Ink
import com.cinntra.ledurecare.ui.theme.Muted
import com.cinntra.ledurecare.ui.theme.Placeholder
import com.cinntra.ledurecare.ui.theme.TabTrack

@Composable
fun PortalBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFE7F8EE))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(Green500))
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Ink)
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
}

@Composable
fun ModeTabs(
    selected: LoginMode,
    onSelect: (LoginMode) -> Unit,
    userIdLabel: String,
    userIdIcon: ImageVector,
    mobileLabel: String,
    mobileIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TabTrack)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeTab(userIdLabel, userIdIcon, selected == LoginMode.UserId, { onSelect(LoginMode.UserId) }, Modifier.weight(1f))
        ModeTab(mobileLabel, mobileIcon, selected == LoginMode.Mobile, { onSelect(LoginMode.Mobile) }, Modifier.weight(1f))
    }
}

@Composable
private fun ModeTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (selected) Ink else Muted)
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Ink else Muted,
            )
        }
    }
}

/** Six-box OTP entry. A hidden text field captures input; the boxes only render it. */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    length: Int = OTP_LENGTH,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= length && it.all(Char::isDigit)) onValueChange(it) },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = { inner ->
            Box {
                Box(Modifier.size(1.dp).alpha(0f)) { inner() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = null, indication = null) {
                            focusRequester.requestFocus()
                            keyboard?.show()
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val activeIndex = value.length.coerceAtMost(length - 1)
                    repeat(length) { index ->
                        OtpCell(
                            char = value.getOrNull(index),
                            active = index == activeIndex,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun OtpCell(char: Char?, active: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val border = when {
        active -> BorderStroke(1.5.dp, Green500)
        char != null -> BorderStroke(1.dp, GreenMintBorder)
        else -> BorderStroke(1.dp, FieldBorder)
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (char != null) GreenMint else Color.White)
            .border(border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) {
            Text(char.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        } else if (active) {
            Box(Modifier.width(1.5.dp).height(20.dp).background(Green500))
        }
    }
}
