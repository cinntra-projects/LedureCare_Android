package com.cinntra.ledurecare.feature.audit.ui

import kotlinx.serialization.Serializable

/**
 * The audit form: a new audit for the planned visit [visitId], or, when [returnId] is set, the existing DLI
 * with that id opened for editing. The property names double as the argument keys read by [AuditViewModel].
 */
@Serializable
data class AuditDestination(val visitId: Long = 0L, val returnId: Int? = null)

/** The "DLI submitted" confirmation. [dliNo] is read by [SubmittedAuditViewModel]. */
@Serializable
data class AuditSubmittedDestination(val dliNo: String)

/** The file viewer. [fileIndex] is [AUDIT_FORM_INDEX] for the audit form, otherwise an attachment's position. */
@Serializable
data class AuditFileDestination(val dliNo: String, val fileIndex: Int)
