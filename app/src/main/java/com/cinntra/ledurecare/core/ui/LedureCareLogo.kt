package com.cinntra.ledurecare.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinntra.ledurecare.R

/** The bundled variable font; weight 600 is set through its `wght` axis (Android 8+), older phones show the regular weight. */
@OptIn(ExperimentalTextApi::class)
private val PlayfairItalic = FontFamily(
    Font(
        R.font.playfair_display_italic,
        weight = FontWeight.SemiBold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

/**
 * The Ledure wordmark followed by "CARE", sized to the same cap height so the two read as one logo.
 * [height] is the height of the wordmark image; the logo is white, for dark or green backgrounds.
 */
@Composable
fun LedureCareLogo(height: Dp, modifier: Modifier = Modifier, color: Color = Color.White) {
    // Playfair Display capitals are about 0.71 of the font size tall; Dp.toSp keeps it exact whatever the user's font scale.
    val fontSize = with(LocalDensity.current) { (height / 0.71f).toSp() }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(height * 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ledure_logo),
            contentDescription = null,
            modifier = Modifier.height(height),
            contentScale = ContentScale.FillHeight,
            alignment = Alignment.CenterStart,
        )
        Text(
            text = stringResource(R.string.logo_care),
            color = color,
            style = TextStyle(
                // Same as the dashboard: Playfair Display, italic, weight 600, line-height 1, tracking 0.02em.
                fontFamily = PlayfairItalic,
                fontStyle = FontStyle.Italic,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = fontSize * 0.02f,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}
