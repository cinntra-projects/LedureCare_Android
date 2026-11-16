package com.example.ledurecare.feature.audit.ui

import kotlinx.serialization.Serializable

/** The audit form for one planned visit. [visitId] doubles as the argument key read by [AuditViewModel]. */
@Serializable
data class AuditDestination(val visitId: Long)

/** The "DLI submitted" confirmation. [dliNo] is read by [SubmittedAuditViewModel]. */
@Serializable
data class AuditSubmittedDestination(val dliNo: String)

/** The file viewer. [fileIndex] is [AUDIT_FORM_INDEX] for the audit form, otherwise an attachment's position. */
@Serializable
data class AuditFileDestination(val dliNo: String, val fileIndex: Int)
