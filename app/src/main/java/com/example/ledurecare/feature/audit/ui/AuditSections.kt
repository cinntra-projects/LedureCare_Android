package com.example.ledurecare.feature.audit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.ClickableField
import com.example.ledurecare.core.ui.DropdownField
import com.example.ledurecare.core.ui.FormErrorText
import com.example.ledurecare.core.ui.LabeledField
import com.example.ledurecare.core.ui.LedureDatePickerDialog
import com.example.ledurecare.core.ui.LedureTextField
import com.example.ledurecare.core.ui.numericDate
import com.example.ledurecare.feature.audit.domain.AuditField
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.AuditItem
import com.example.ledurecare.feature.audit.domain.EntryField
import com.example.ledurecare.feature.audit.domain.MfgType
import com.example.ledurecare.feature.audit.domain.WarrantyStatus
import com.example.ledurecare.feature.dealers.ui.DealerPickerSheet
import com.example.ledurecare.ui.theme.BpCodeBg
import com.example.ledurecare.ui.theme.BpCodeText
import com.example.ledurecare.ui.theme.CopiedBg
import com.example.ledurecare.ui.theme.ErrorRed
import com.example.ledurecare.ui.theme.FieldBg
import com.example.ledurecare.ui.theme.FieldBorder
import com.example.ledurecare.ui.theme.FormPanelBg
import com.example.ledurecare.ui.theme.FormPanelBorder
import com.example.ledurecare.ui.theme.Green500
import com.example.ledurecare.ui.theme.GreenMint
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PdfRedBg
import com.example.ledurecare.ui.theme.PlanGreen

private val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

@Composable
private fun required() = stringResource(R.string.audit_required)

@Composable
fun FormCard(title: String, modifier: Modifier = Modifier, hint: String? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                if (hint != null) Text(hint, fontSize = 11.sp, color = Muted)
            }
            content()
        }
    }
}

// ---------------------------------------------------------------- dealer details

@Composable
fun DealerDetailsCard(state: AuditUiState, onEvent: (AuditEvent) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDealerPicker by remember { mutableStateOf(false) }
    FormCard(
        title = stringResource(R.string.audit_dealer_details),
        hint = stringResource(R.string.audit_dealer_hint),
    ) {
        LabeledField(
            stringResource(R.string.audit_dealer),
            required = true,
            error = if (AuditField.Dealer in state.errors) required() else null,
        ) {
            // A tap opens a bottom sheet with search, not a dropdown: the dealer list can be long.
            ClickableField(
                text = state.dealer?.let { stringResource(R.string.audit_dealer_option, it.name, it.bpCode) },
                placeholder = stringResource(R.string.audit_select_dealer),
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onClick = {
                    showDealerPicker = true
                    onEvent(AuditEvent.OpenDealerPicker)
                },
                isError = AuditField.Dealer in state.errors,
            )
        }
        LabeledField(
            stringResource(R.string.audit_address),
            required = true,
            error = if (AuditField.Address in state.errors) required() else null,
        ) {
            LedureTextField(
                value = state.address,
                onValueChange = { onEvent(AuditEvent.AddressChanged(it)) },
                placeholder = "",
                isError = AuditField.Address in state.errors,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(
                stringResource(R.string.audit_customer_code),
                Modifier.weight(1f),
                required = true,
                error = if (AuditField.CustomerCode in state.errors) required() else null,
            ) {
                LedureTextField(
                    value = state.customerCode,
                    onValueChange = { onEvent(AuditEvent.CustomerCodeChanged(it)) },
                    placeholder = "",
                    isError = AuditField.CustomerCode in state.errors,
                )
            }
            LabeledField(
                stringResource(R.string.audit_sales_person),
                Modifier.weight(1f),
                required = true,
                error = if (AuditField.SalesPerson in state.errors) required() else null,
            ) {
                LedureTextField(
                    value = state.salesPerson,
                    onValueChange = { onEvent(AuditEvent.SalesPersonChanged(it)) },
                    placeholder = "",
                    isError = AuditField.SalesPerson in state.errors,
                )
            }
        }
        LabeledField(
            stringResource(R.string.audit_inspection_date),
            required = true,
            error = if (AuditField.InspectionDate in state.errors) required() else null,
        ) {
            ClickableField(
                text = state.inspectionDate.numericDate(),
                placeholder = stringResource(R.string.audit_pick_date),
                trailingIcon = Icons.Outlined.CalendarMonth,
                onClick = { showDatePicker = true },
                isError = AuditField.InspectionDate in state.errors,
            )
        }
    }

    if (showDealerPicker) {
        DealerPickerSheet(
            search = state.dealerSearch,
            selectedId = state.dealer?.id,
            onQueryChange = { onEvent(AuditEvent.DealerQueryChanged(it)) },
            onLoadMore = { onEvent(AuditEvent.LoadMoreDealers) },
            onRetry = { onEvent(AuditEvent.RetryDealers) },
            onSelect = {
                showDealerPicker = false
                onEvent(AuditEvent.DealerSelected(it))
            },
            onDismiss = { showDealerPicker = false },
        )
    }

    if (showDatePicker) {
        LedureDatePickerDialog(
            initial = state.inspectionDate,
            onDismiss = { showDatePicker = false },
            onPick = {
                showDatePicker = false
                onEvent(AuditEvent.InspectionDateChanged(it))
            },
        )
    }
}

// ---------------------------------------------------------------- audited items

@Composable
fun AuditedItemsCard(state: AuditUiState, onEvent: (AuditEvent) -> Unit) {
    FormCard(
        title = stringResource(R.string.audit_items_title),
        hint = stringResource(R.string.audit_items_hint),
    ) {
        ItemEntryPanel(state, onEvent)

        if (AuditField.Items in state.errors) FormErrorText(stringResource(R.string.audit_error_items))

        Text(
            stringResource(R.string.audit_added_items, state.items.size),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Muted,
        )
        if (state.items.isEmpty()) {
            Text(stringResource(R.string.audit_no_items), fontSize = 11.sp, color = Muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.items.forEach { item ->
                    AddedItemCard(
                        item = item,
                        editing = item.id == state.editingId,
                        onRepeat = { onEvent(AuditEvent.RepeatItem(item.id)) },
                        onEdit = { onEvent(AuditEvent.EditItem(item.id)) },
                        onDelete = { onEvent(AuditEvent.DeleteItem(item.id)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemEntryPanel(state: AuditUiState, onEvent: (AuditEvent) -> Unit) {
    val entry = state.entry
    val errors = state.entryErrors
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(state.editingId) {
        if (state.editingId != null) requester.bringIntoView()
    }

    Surface(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester),
        shape = RoundedCornerShape(14.dp),
        color = FormPanelBg,
        border = BorderStroke(1.dp, FormPanelBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledField(
                stringResource(R.string.audit_product),
                required = true,
                error = if (EntryField.Product in errors) required() else null,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LedureTextField(
                        value = entry.productQuery,
                        onValueChange = { onEvent(AuditEvent.ProductQueryChanged(it)) },
                        placeholder = stringResource(R.string.audit_product_hint),
                        leadingIcon = Icons.Filled.Search,
                        isError = EntryField.Product in errors,
                    )
                    if (state.suggestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, FieldBorder),
                        ) {
                            Column {
                                state.suggestions.forEach { product ->
                                    Text(
                                        "${product.code} · ${product.name}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEvent(AuditEvent.ProductSelected(product)) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        fontSize = 13.sp,
                                        color = Ink,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField(stringResource(R.string.audit_qty), Modifier.weight(1f), required = true) {
                    LedureTextField(
                        value = entry.qty,
                        onValueChange = { onEvent(AuditEvent.QtyChanged(it)) },
                        placeholder = "",
                        keyboardOptions = NumberKeyboard,
                        isError = EntryField.Qty in errors,
                    )
                }
                LabeledField(stringResource(R.string.audit_approved), Modifier.weight(1f)) {
                    LedureTextField(
                        value = entry.approved,
                        onValueChange = { onEvent(AuditEvent.ApprovedChanged(it)) },
                        placeholder = "",
                        keyboardOptions = NumberKeyboard,
                        isError = EntryField.Approved in errors,
                    )
                }
                LabeledField(stringResource(R.string.audit_rejected), Modifier.weight(1f)) {
                    LedureTextField(
                        value = entry.rejected,
                        onValueChange = { onEvent(AuditEvent.RejectedChanged(it)) },
                        placeholder = "",
                        keyboardOptions = NumberKeyboard,
                        isError = EntryField.Approved in errors,
                    )
                }
            }
            if (EntryField.Approved in errors) FormErrorText(stringResource(R.string.audit_error_approved))

            LabeledField(stringResource(R.string.audit_total_approved)) {
                LedureTextField(
                    value = entry.totalApproved,
                    onValueChange = { onEvent(AuditEvent.TotalApprovedChanged(it)) },
                    placeholder = "",
                    keyboardOptions = NumberKeyboard,
                    isError = EntryField.Approved in errors,
                )
            }

            MfgDateField(entry, state.today.year, errors, onEvent)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField(
                    stringResource(R.string.audit_condition),
                    Modifier.weight(1f),
                    required = true,
                    error = if (EntryField.Condition in errors) required() else null,
                ) {
                    DropdownField(
                        selected = entry.condition,
                        options = state.options.conditions,
                        optionLabel = { it },
                        placeholder = stringResource(R.string.audit_select),
                        onSelect = { onEvent(AuditEvent.ConditionChanged(it)) },
                        isError = EntryField.Condition in errors,
                    )
                }
                LabeledField(
                    stringResource(R.string.audit_warranty),
                    Modifier.weight(1f),
                    required = true,
                    error = if (EntryField.Warranty in errors) required() else null,
                ) {
                    DropdownField(
                        selected = entry.warranty,
                        options = WarrantyStatus.entries,
                        optionLabel = { warrantyLabel(it) },
                        placeholder = stringResource(R.string.audit_select),
                        onSelect = { onEvent(AuditEvent.WarrantyChanged(it)) },
                        isError = EntryField.Warranty in errors,
                    )
                }
            }

            val special = entry.warranty == WarrantyStatus.OutOfWarranty
            val remarkLabel = stringResource(if (special) R.string.audit_special_remark else R.string.audit_remark)
            LabeledField(
                remarkLabel,
                required = special,
                error = if (EntryField.Remark in errors) required() else null,
            ) {
                LedureTextField(
                    value = entry.remark,
                    onValueChange = { onEvent(AuditEvent.RemarkChanged(it)) },
                    placeholder = remarkLabel,
                    isError = EntryField.Remark in errors,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.editingId != null) {
                    OutlineButton(
                        text = stringResource(R.string.audit_cancel_edit),
                        onClick = { onEvent(AuditEvent.CancelEdit) },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlineButton(
                    text = stringResource(if (state.editingId != null) R.string.audit_update_item else R.string.audit_add_item),
                    onClick = { onEvent(AuditEvent.AddItem) },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Add.takeIf { state.editingId == null },
                )
            }
        }
    }
}

@Composable
private fun warrantyLabel(status: WarrantyStatus): String = stringResource(
    if (status == WarrantyStatus.InWarranty) R.string.audit_warranty_in else R.string.audit_warranty_out
)

@Composable
private fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, PlanGreen),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = PlanGreen)
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
        }
    }
}

@Composable
private fun AddedItemCard(
    item: AuditItem,
    editing: Boolean,
    onRepeat: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val highlighted = item.copied || editing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) CopiedBg else Color.White,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) Green500 else FieldBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${item.product.code} · ${item.product.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                if (item.copied) {
                    stringResource(R.string.audit_item_copied, item.qty)
                } else {
                    stringResource(R.string.audit_item_qty_line, item.qty, item.approved, item.rejected)
                },
                fontSize = 11.sp,
                color = Ink,
            )
            Text(
                stringResource(R.string.audit_item_meta, item.mfgLabel(), item.condition, warrantyLabel(item.warranty)),
                fontSize = 10.sp,
                color = Muted,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onRepeat,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PlanGreen),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Repeat, contentDescription = null, modifier = Modifier.size(12.dp), tint = PlanGreen)
                        Text(stringResource(R.string.audit_repeat), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.audit_edit_item), modifier = Modifier.size(18.dp), tint = Muted)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.audit_delete_item), modifier = Modifier.size(18.dp), tint = ErrorRed)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- warehouse, notes, attachments

@Composable
fun WarehouseCard(state: AuditUiState, onEvent: (AuditEvent) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GreenMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Warehouse, contentDescription = null, modifier = Modifier.size(18.dp), tint = PlanGreen)
                }
                Column {
                    Text(stringResource(R.string.audit_warehouse_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(stringResource(R.string.audit_warehouse_hint), fontSize = 11.sp, color = Muted)
                }
            }
            if (state.zone.isNotBlank()) {
                Text(
                    stringResource(R.string.audit_zone, state.zone),
                    modifier = Modifier.clip(CircleShape).background(BpCodeBg).padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BpCodeText,
                )
            }
            LabeledField(
                stringResource(R.string.audit_warehouse),
                required = true,
                error = if (AuditField.Warehouse in state.errors) required() else null,
            ) {
                DropdownField(
                    selected = state.warehouse,
                    options = state.options.warehouses,
                    optionLabel = { it.label },
                    placeholder = stringResource(R.string.audit_select_warehouse),
                    onSelect = { onEvent(AuditEvent.WarehouseSelected(it.id)) },
                    isError = AuditField.Warehouse in state.errors,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    stringResource(R.string.audit_total_box),
                    Modifier.weight(1f),
                    required = true,
                    error = if (AuditField.TotalBox in state.errors) required() else null,
                ) {
                    LedureTextField(
                        value = state.totalBox,
                        onValueChange = { onEvent(AuditEvent.TotalBoxChanged(it)) },
                        placeholder = stringResource(R.string.audit_total_box_hint),
                        keyboardOptions = NumberKeyboard,
                        isError = AuditField.TotalBox in state.errors,
                    )
                }
                // Not typed: it is the sum of "total approved" over the added items.
                LabeledField(stringResource(R.string.audit_total_qty), Modifier.weight(1f)) {
                    LedureTextField(
                        value = state.totalQty.toString(),
                        onValueChange = {},
                        placeholder = "",
                        readOnly = true,
                    )
                }
            }
        }
    }
}

@Composable
fun NotesCard(state: AuditUiState, onEvent: (AuditEvent) -> Unit) {
    FormCard(title = stringResource(R.string.audit_notes_title)) {
        LedureTextField(
            value = state.notes,
            onValueChange = { onEvent(AuditEvent.NotesChanged(it)) },
            placeholder = stringResource(R.string.audit_notes_hint),
            singleLine = false,
        )
    }
}

@Composable
fun AttachmentsCard(state: AuditUiState, onAdd: () -> Unit, onEvent: (AuditEvent) -> Unit) {
    FormCard(
        title = stringResource(R.string.audit_attachments_title),
        hint = stringResource(R.string.audit_attachments_hint),
    ) {
        val dash = Green500
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CopiedBg)
                .drawBehind {
                    drawRoundRect(
                        color = dash,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                    )
                }
                .clickable(onClick = onAdd),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(24.dp), tint = PlanGreen)
            Text(stringResource(R.string.audit_attach_add), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlanGreen)
        }
        state.files.forEach { file -> FileRow(file) { onEvent(AuditEvent.RemoveFile(file.uri)) } }
    }
}

@Composable
private fun FileRow(file: AuditFile, onRemove: () -> Unit) {
    val pdf = file.isPdf()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FieldBg,
        border = BorderStroke(1.dp, FieldBorder),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (pdf) PdfRedBg else BpCodeBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (pdf) Icons.Outlined.Description else Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (pdf) ErrorRed else BpCodeText,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.audit_attached, formatFileSize(file.sizeBytes)),
                    fontSize = 10.sp,
                    color = Green500,
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.audit_remove_file), modifier = Modifier.size(16.dp), tint = Muted)
            }
        }
    }
}

/** One field for the whole manufacturing date; tapping it opens the quarter/month dialog. */
@Composable
private fun MfgDateField(entry: ItemEntry, thisYear: Int, errors: Set<EntryField>, onEvent: (AuditEvent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val type = entry.mfgType
    val period = entry.mfgPeriod
    val year = entry.mfgYear
    val current = if (type != null && period != null && year != null) MfgPick(type, period, year) else null
    val failed = EntryField.MfgType in errors || EntryField.MfgPeriod in errors || EntryField.MfgYear in errors
    LabeledField(
        stringResource(R.string.audit_mfg_date),
        required = true,
        error = if (failed) required() else null,
    ) {
        ClickableField(
            text = current?.label(),
            placeholder = stringResource(R.string.audit_mfg_date_pick),
            trailingIcon = Icons.Outlined.CalendarMonth,
            onClick = { open = true },
            isError = failed,
        )
    }
    if (open) {
        MfgDatePickerDialog(
            current = current,
            maxYear = thisYear,
            minYear = thisYear - 15,
            onApply = { open = false; onEvent(AuditEvent.MfgPicked(it.type, it.period, it.year)) },
            onClear = { open = false; onEvent(AuditEvent.MfgCleared) },
            onDismiss = { open = false },
        )
    }
}
