package com.example.ledurecare.feature.reports.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ledurecare.R
import com.example.ledurecare.feature.reports.domain.ReturnStatus

@get:StringRes
val ReturnStatus.titleRes: Int
    get() = when (this) {
        ReturnStatus.Coord -> R.string.stage_coord_title
        ReturnStatus.Transit -> R.string.stage_transit_title
        ReturnStatus.Wh -> R.string.stage_wh_title
        ReturnStatus.AdminApproval -> R.string.stage_admin_title
        ReturnStatus.Acc -> R.string.stage_acc_title
        ReturnStatus.Sap -> R.string.stage_sap_title
        ReturnStatus.Cn -> R.string.stage_cn_title
        ReturnStatus.Completed -> R.string.stage_completed_title
    }

@get:StringRes
val ReturnStatus.ownerRes: Int
    get() = when (this) {
        ReturnStatus.Coord -> R.string.stage_coord_owner
        ReturnStatus.Transit -> R.string.stage_transit_owner
        ReturnStatus.Wh -> R.string.stage_wh_owner
        ReturnStatus.AdminApproval -> R.string.stage_admin_owner
        ReturnStatus.Acc -> R.string.stage_acc_owner
        ReturnStatus.Sap -> R.string.stage_sap_owner
        ReturnStatus.Cn -> R.string.stage_cn_owner
        ReturnStatus.Completed -> R.string.stage_completed_owner
    }

/** Row label in the tracker; `null` is the "Submitted by Service Engineer" row. */
@StringRes
fun trackerLabelRes(status: ReturnStatus?): Int = when (status) {
    null -> R.string.tracker_submitted
    ReturnStatus.Coord -> R.string.tracker_coord
    ReturnStatus.Transit -> R.string.tracker_transit
    ReturnStatus.Wh -> R.string.tracker_wh
    ReturnStatus.AdminApproval -> R.string.tracker_admin
    ReturnStatus.Acc -> R.string.tracker_acc
    ReturnStatus.Sap -> R.string.tracker_sap
    ReturnStatus.Cn -> R.string.tracker_cn
    ReturnStatus.Completed -> R.string.tracker_completed
}

val ReturnStatus.icon: ImageVector
    get() = when (this) {
        ReturnStatus.Coord -> Icons.Outlined.Groups
        ReturnStatus.Transit -> Icons.Outlined.LocalShipping
        ReturnStatus.Wh -> Icons.Outlined.Inventory2
        ReturnStatus.AdminApproval -> Icons.Outlined.TaskAlt
        ReturnStatus.Acc -> Icons.Outlined.CurrencyRupee
        ReturnStatus.Sap -> Icons.Outlined.Bolt
        ReturnStatus.Cn -> Icons.Outlined.Receipt
        ReturnStatus.Completed -> Icons.Outlined.EmojiEvents
    }
