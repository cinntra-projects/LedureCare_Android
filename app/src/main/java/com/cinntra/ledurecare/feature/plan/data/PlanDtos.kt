package com.cinntra.ledurecare.feature.plan.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One plan, as listed, created or updated. */
@Serializable
data class PlanDto(
    val id: Long,
    @SerialName("dealer_code") val dealerCode: String? = null,
    @SerialName("dealer_name") val dealerName: String? = null,
    val city: String? = null,
    @SerialName("plan_date") val planDate: String? = null,
    /** "planned" or "completed". */
    val status: String? = null,
    /** The submitted DLI of a completed plan, when the server gives it. */
    @SerialName("return_id") val returnId: JsonPrimitive? = null,
) {
    /** `return_id` as a number, whether the server sent it as 41 or "41"; null when absent. */
    val returnIdValue: Int?
        get() = returnId?.contentOrNull?.let { text ->
            text.trim().toIntOrNull() ?: Regex("\\d+").findAll(text).lastOrNull()?.value?.toIntOrNull()
        }
}

/** `dealers[]` of `POST api/plans/create/`: the dealer as bp-lookup gave it. */
@Serializable
data class PlanDealerDto(
    @SerialName("dealer_code") val dealerCode: String,
    @SerialName("dealer_name") val dealerName: String,
    val city: String? = null,
)

/** `POST api/plans/create/`: one to 100 dealers for a single date. */
@Serializable
data class CreatePlansRequestDto(
    @SerialName("plan_date") val planDate: String,
    val dealers: List<PlanDealerDto>,
)

/** `data.skipped[]` of the create response. */
@Serializable
data class SkippedPlanDto(
    @SerialName("dealer_code") val dealerCode: String? = null,
    val reason: String? = null,
)

/**
 * `POST api/plans/{id}/update/`. Only fields that are set are sent (the JSON config omits nulls).
 * `dealer_code` and `dealer_name` must travel together.
 */
@Serializable
data class UpdatePlanRequestDto(
    @SerialName("plan_date") val planDate: String? = null,
    @SerialName("dealer_code") val dealerCode: String? = null,
    @SerialName("dealer_name") val dealerName: String? = null,
    val city: String? = null,
)
