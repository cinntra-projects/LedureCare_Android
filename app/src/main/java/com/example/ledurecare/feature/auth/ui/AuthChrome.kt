package com.example.ledurecare.feature.auth.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.LedureCareLogo
import com.example.ledurecare.core.ui.VideoBackground
import com.example.ledurecare.ui.theme.GreenAccentText
import com.example.ledurecare.ui.theme.GreenDeepBottom
import com.example.ledurecare.ui.theme.GreenDeepTop

/**
 * The looping video behind every signed-out screen. It is shown once, by the navigation host, so it keeps
 * playing without a restart while the screens in front of it change.
 */
@Composable
fun AuthBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GreenDeepTop, GreenDeepBottom))),
    ) {
        VideoBackground(rawRes = R.raw.login_bg, modifier = Modifier.fillMaxSize())
    }
}

/**
 * The look shared by every signed-out screen: the logo and tagline, and a white card holding [content].
 * The card sits at the bottom and scrolls when the keyboard is up. The video comes from [AuthBackdrop].
 */
@Composable
fun AuthScreenFrame(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 28.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Brand(Modifier.padding(horizontal = 8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 12.dp,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PortalBadge(stringResource(R.string.login_badge))
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LedureCareLogo(height = 20.dp)
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.login_tagline_prefix))
                withStyle(SpanStyle(color = GreenAccentText)) {
                    append(stringResource(R.string.login_tagline_accent))
                }
            },
            color = Color.White,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
