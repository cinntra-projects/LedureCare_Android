package com.cinntra.ledurecare.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.core.common.AppError

/** The text to show the user for [this] error. Use when there is a [Context] but no composition. */
fun AppError.message(context: Context): String = when (this) {
    is AppError.Validation -> context.getString(
        when (field) {
            AppError.Validation.Field.Credentials -> R.string.error_validation_credentials
            AppError.Validation.Field.Mobile -> R.string.error_validation_mobile
            AppError.Validation.Field.Otp -> R.string.error_validation_otp
            AppError.Validation.Field.Email -> R.string.error_validation_email
            AppError.Validation.Field.PasswordTooShort -> R.string.error_validation_password_short
            AppError.Validation.Field.PasswordMismatch -> R.string.error_validation_password_mismatch
        }
    )
    AppError.Network -> context.getString(R.string.error_network)
    is AppError.Http -> message ?: context.getString(R.string.error_http, code)
    is AppError.Unknown -> context.getString(R.string.error_unknown)
}

@Composable
fun AppError.asMessage(): String = message(LocalContext.current)
