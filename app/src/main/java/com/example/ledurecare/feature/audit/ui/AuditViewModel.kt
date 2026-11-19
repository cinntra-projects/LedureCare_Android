package com.example.ledurecare.feature.audit.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.audit.domain.AuditDraft
import com.example.ledurecare.feature.audit.domain.AuditField
import com.example.ledurecare.feature.audit.domain.EntryField
import com.example.ledurecare.feature.audit.domain.GetAuditForEditUseCase
import com.example.ledurecare.feature.audit.domain.GetAuditOptionsUseCase
import com.example.ledurecare.feature.audit.domain.SearchProductsUseCase
import com.example.ledurecare.feature.audit.domain.UpdateAuditUseCase
import kotlinx.coroutines.delay
import com.example.ledurecare.feature.audit.domain.ItemInput
import com.example.ledurecare.feature.audit.domain.MAX_FILE_BYTES
import com.example.ledurecare.feature.audit.domain.SubmitAuditUseCase
import com.example.ledurecare.feature.audit.domain.toItem
import com.example.ledurecare.feature.audit.domain.validateAudit
import com.example.ledurecare.feature.audit.domain.validateItem
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.domain.GetDealerDetailsUseCase
import com.example.ledurecare.feature.dealers.domain.SearchDealersUseCase
import com.example.ledurecare.feature.dealers.ui.DealerSearchController
import com.example.ledurecare.feature.plan.domain.GetVisitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

private const val MAX_DIGITS = 6
private const val MIN_PRODUCT_QUERY = 2
private const val PRODUCT_SEARCH_DEBOUNCE_MS = 300L

private fun String.digits() = filter(Char::isDigit).take(MAX_DIGITS)

@HiltViewModel
class AuditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getVisit: GetVisitUseCase,
    private val searchDealers: SearchDealersUseCase,
    private val getDealerDetails: GetDealerDetailsUseCase,
    private val getOptions: GetAuditOptionsUseCase,
    private val searchItems: SearchProductsUseCase,
    private val submitAudit: SubmitAuditUseCase,
    private val getAuditForEdit: GetAuditForEditUseCase,
    private val updateAudit: UpdateAuditUseCase,
    clock: Clock,
) : ViewModel() {

    // Navigation arguments, stored under their property names (see AuditDestination).
    private val visitId: Long = savedStateHandle.get<Long>("visitId") ?: 0L

    /** Set when an existing DLI is being edited instead of a new audit started from a plan. */
    private val returnId: Int? = savedStateHandle.get<Int>("returnId")

    private val today = LocalDate.now(clock)
    private val _state = MutableStateFlow(AuditUiState(today = today, inspectionDate = today))
    val state: StateFlow<AuditUiState> = _state.asStateFlow()

    private val _effects = Channel<AuditEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var nextItemId = 1L

    // The dealer picker searches and pages on the server; its state is mirrored into the form state.
    private val dealerSearch = DealerSearchController(searchDealers, viewModelScope)

    init {
        viewModelScope.launch { dealerSearch.state.collect { search -> _state.update { it.copy(dealerSearch = search) } } }
        load()
    }

    fun onEvent(event: AuditEvent) {
        when (event) {
            AuditEvent.Retry -> load()

            AuditEvent.OpenDealerPicker -> dealerSearch.reset()
            is AuditEvent.DealerQueryChanged -> dealerSearch.setQuery(event.query)
            AuditEvent.LoadMoreDealers -> dealerSearch.loadMore()
            AuditEvent.RetryDealers -> dealerSearch.retry()
            is AuditEvent.DealerSelected -> selectDealer(event.dealer)
            is AuditEvent.AddressChanged -> field(AuditField.Address) { it.copy(address = event.value) }
            is AuditEvent.CustomerCodeChanged -> field(AuditField.CustomerCode) { it.copy(customerCode = event.value) }
            is AuditEvent.SalesPersonChanged -> field(AuditField.SalesPerson) { it.copy(salesPerson = event.value) }
            is AuditEvent.InspectionDateChanged -> field(AuditField.InspectionDate) { it.copy(inspectionDate = event.date) }
            is AuditEvent.TotalBoxChanged -> field(AuditField.TotalBox) { it.copy(totalBox = event.value.digits()) }

            is AuditEvent.ProductQueryChanged -> {
                entry(EntryField.Product) { it.copy(productQuery = event.value, product = null) }
                runProductSearch(event.value)
            }
            is AuditEvent.ProductSelected -> {
                entry(EntryField.Product) {
                    it.copy(productQuery = "${event.product.code} · ${event.product.name}", product = event.product)
                }
                runProductSearch("")
            }
            is AuditEvent.QtyChanged -> entry(EntryField.Qty, EntryField.Approved) { it.copy(qty = event.value.digits()) }
            is AuditEvent.MfgTypeChanged -> entry(EntryField.MfgType, EntryField.MfgPeriod) {
                // Month and quarter numbers mean different things, so a new type clears the old pick.
                if (it.mfgType == event.type) it else it.copy(mfgType = event.type, mfgPeriod = null)
            }
            is AuditEvent.MfgPeriodChanged -> entry(EntryField.MfgPeriod) { it.copy(mfgPeriod = event.period) }
            is AuditEvent.MfgYearChanged -> entry(EntryField.MfgYear) { it.copy(mfgYear = event.year) }
            is AuditEvent.MfgPicked -> entry(EntryField.MfgType, EntryField.MfgPeriod, EntryField.MfgYear) {
                it.copy(mfgType = event.type, mfgPeriod = event.period, mfgYear = event.year)
            }
            AuditEvent.MfgCleared -> entry { it.copy(mfgType = null, mfgPeriod = null, mfgYear = null) }
            is AuditEvent.ConditionChanged -> entry(EntryField.Condition) { it.copy(condition = event.condition) }
            is AuditEvent.WarrantyChanged -> entry(EntryField.Warranty, EntryField.Remark) { it.copy(warranty = event.warranty) }
            is AuditEvent.RemarkChanged -> entry(EntryField.Remark) { it.copy(remark = event.value) }
            AuditEvent.AddItem -> addItem()
            AuditEvent.CancelEdit -> _state.update { it.copy(entry = ItemEntry(), editingId = null, entryErrors = emptySet()) }

            is AuditEvent.RepeatItem -> repeatItem(event.id)
            is AuditEvent.EditItem -> editItem(event.id)
            is AuditEvent.DeleteItem -> deleteItem(event.id)

            is AuditEvent.WarehouseSelected -> field(AuditField.Warehouse) { s ->
                s.copy(warehouse = s.options.warehouses.firstOrNull { it.id == event.warehouseId })
            }
            is AuditEvent.NotesChanged -> _state.update { it.copy(notes = event.value) }
            is AuditEvent.FilesPicked -> addFiles(event.files)
            is AuditEvent.RemoveFile -> _state.update { s -> s.copy(files = s.files.filterNot { it.uri == event.uri }) }
            AuditEvent.OpenPreview -> openPreview()
            AuditEvent.ClosePreview -> _state.update { it.copy(showPreview = false, submitError = null) }
            AuditEvent.Submit -> submit()
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            if (returnId != null) return@launch loadForEdit(returnId)
            val visit = getVisit(visitId)
            val options = getOptions()
            val failure = (options as? AppResult.Failure)?.error
            if (failure != null || visit == null || options !is AppResult.Success) {
                _state.update {
                    it.copy(isLoading = false, error = failure ?: AppError.Http(404, "Planned visit not found."))
                }
                return@launch
            }
            // The plan only carries the dealer's name and code; the address etc. are fetched below.
            val dealer = Dealer(visit.dealerId, visit.dealerName, visit.bpCode, visit.city)
            _state.update {
                it.copy(
                    isLoading = false,
                    startedFrom = visit.date,
                    options = options.data,
                    // With one warehouse there is nothing to choose. With several, and no zone to go by,
                    // the engineer picks: a wrong default could send goods to the wrong place unnoticed.
                    warehouse = it.warehouse ?: options.data.warehouses.singleOrNull(),
                    dealer = dealer,
                    address = dealer.address,
                    customerCode = dealer.bpCode,
                    salesPerson = dealer.salesPerson,
                )
            }
            fillDealerDetails(dealer)
        }
    }

    private var productJob: Job? = null

    /**
     * Looks the typed text up on the server, among the items this dealer can order. It waits a moment
     * after the last key, so typing does not send a request per letter, and a newer search replaces an
     * older one. Without a dealer, or with less than two letters, there is nothing to search.
     */
    private fun runProductSearch(text: String) {
        productJob?.cancel()
        val dealer = _state.value.dealer
        val query = text.trim()
        if (dealer == null || query.length < MIN_PRODUCT_QUERY) {
            _state.update { it.copy(productResults = emptyList(), isSearchingProducts = false) }
            return
        }
        productJob = viewModelScope.launch {
            delay(PRODUCT_SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(isSearchingProducts = true) }
            val found = (searchItems(dealer.bpCode.ifBlank { dealer.id }, query) as? AppResult.Success)?.data.orEmpty()
            _state.update { it.copy(productResults = found, isSearchingProducts = false) }
        }
    }

    /** Fills the form with the DLI being edited. */
    private suspend fun loadForEdit(id: Int) {
        val options = getOptions()
        val edit = getAuditForEdit(id)
        val failure = (options as? AppResult.Failure)?.error ?: (edit as? AppResult.Failure)?.error
        if (failure != null || options !is AppResult.Success || edit !is AppResult.Success) {
            _state.update { it.copy(isLoading = false, error = failure ?: AppError.Http(404, "DLI not found.")) }
            return
        }
        val dli = edit.data
        nextItemId = (dli.items.maxOfOrNull { it.id } ?: 0L) + 1
        _state.update {
            it.copy(
                isLoading = false,
                isEditing = true,
                dliNo = dli.dliNo,
                options = options.data,
                dealer = dli.dealer,
                address = dli.address,
                customerCode = dli.customerCode,
                salesPerson = dli.salesPerson,
                inspectionDate = dli.inspectionDate ?: it.inspectionDate,
                totalBox = dli.totalBox?.toString().orEmpty(),
                // The form no longer has approved / rejected fields: every item counts as fully approved.
                items = dli.items.map { item -> item.copy(approved = item.qty, rejected = 0, totalApproved = item.qty) },
                warehouse = options.data.warehouses.firstOrNull { w -> w.id == dli.warehouseId }
                    ?: options.data.warehouses.singleOrNull(),
                notes = dli.notes,
            )
        }
    }

    private fun selectDealer(dealer: Dealer) {
        _state.update { s ->
            s.copy(
                dealer = dealer,
                address = dealer.address,
                customerCode = dealer.bpCode,
                salesPerson = dealer.salesPerson,
                errors = s.errors - setOf(AuditField.Dealer, AuditField.Address, AuditField.CustomerCode, AuditField.SalesPerson),
            )
        }
        fillDealerDetails(dealer)
    }

    private var detailsJob: Job? = null

    /**
     * Asks the business-partner server for [dealer]'s address, sales person and zone, and fills them in.
     * Only blank fields are filled, so something the engineer already typed is kept, and an answer for a
     * dealer that is no longer chosen is dropped. A failed lookup leaves the fields to be typed by hand.
     */
    private fun fillDealerDetails(dealer: Dealer) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            val details = (getDealerDetails(dealer) as? AppResult.Success)?.data ?: return@launch
            _state.update { s ->
                if (s.dealer?.id != dealer.id) s
                else s.copy(
                    dealer = details,
                    address = s.address.ifBlank { details.address },
                    salesPerson = s.salesPerson.ifBlank { details.salesPerson },
                )
            }
        }
    }

    /** Applies [change] and clears the error for [field]. */
    private fun field(field: AuditField, change: (AuditUiState) -> AuditUiState) =
        _state.update { change(it).copy(errors = it.errors - field) }

    /** Applies [change] to the item panel and clears the errors for [clears]. */
    private fun entry(vararg clears: EntryField, change: (ItemEntry) -> ItemEntry) =
        _state.update { it.copy(entry = change(it.entry), entryErrors = it.entryErrors - clears.toSet()) }

    private fun ItemEntry.toInput() = ItemInput(
        product = product,
        qty = qty.toIntOrNull(),
        // The form no longer asks for these: every item counts as fully approved.
        approved = qty.toIntOrNull(),
        rejected = 0,
        totalApproved = qty.toIntOrNull(),
        mfgType = mfgType,
        mfgPeriod = mfgPeriod,
        mfgYear = mfgYear,
        condition = condition,
        warranty = warranty,
        remark = remark,
    )

    private fun addItem() {
        val s = _state.value
        val input = s.entry.toInput()
        val problems = validateItem(input)
        if (problems.isNotEmpty()) {
            _state.update { it.copy(entryErrors = problems) }
            return
        }
        val editing = s.editingId
        val item = input.toItem(editing ?: nextItemId++) ?: return
        _state.update {
            it.copy(
                items = if (editing == null) it.items + item else it.items.map { old -> if (old.id == editing) item else old },
                entry = ItemEntry(),
                editingId = null,
                entryErrors = emptySet(),
                errors = it.errors - AuditField.Items,
                warehouse = it.warehouse ?: it.options.warehouses.singleOrNull(),
            )
        }
    }

    private fun repeatItem(id: Long) = _state.update { s ->
        val index = s.items.indexOfFirst { it.id == id }
        if (index < 0) return@update s
        val copy = s.items[index].copy(
            id = nextItemId++,
            qty = 1,
            approved = 1,
            rejected = 0,
            totalApproved = 1,
            copied = true,
        )
        s.copy(items = s.items.toMutableList().apply { add(index + 1, copy) })
    }

    private fun editItem(id: Long) = _state.update { s ->
        val item = s.items.firstOrNull { it.id == id } ?: return@update s
        s.copy(
            editingId = id,
            entryErrors = emptySet(),
            entry = ItemEntry(
                productQuery = "${item.product.code} · ${item.product.name}",
                product = item.product,
                qty = item.qty.toString(),
                mfgType = item.mfgType,
                mfgPeriod = item.mfgPeriod,
                mfgYear = item.mfgYear,
                condition = item.condition,
                warranty = item.warranty,
                remark = item.remark,
            ),
        )
    }

    private fun deleteItem(id: Long) = _state.update { s ->
        val leavingEdit = s.editingId == id
        s.copy(
            items = s.items.filterNot { it.id == id },
            editingId = if (leavingEdit) null else s.editingId,
            entry = if (leavingEdit) ItemEntry() else s.entry,
        )
    }

    private fun addFiles(picked: List<com.example.ledurecare.feature.audit.domain.AuditFile>) {
        val (allowed, tooLarge) = picked.partition { it.sizeBytes <= MAX_FILE_BYTES }
        _state.update { s ->
            val known = s.files.map { it.uri }.toSet()
            s.copy(files = s.files + allowed.filter { it.uri !in known })
        }
        if (tooLarge.isNotEmpty()) {
            viewModelScope.launch { _effects.send(AuditEffect.FilesRejected(tooLarge.map { it.name })) }
        }
    }

    private fun draft(s: AuditUiState) = AuditDraft(
        dealer = s.dealer,
        address = s.address.trim(),
        customerCode = s.customerCode.trim(),
        salesPerson = s.salesPerson.trim(),
        inspectionDate = s.inspectionDate,
        totalBox = s.totalBox.toIntOrNull(),
        totalQty = s.totalQty,
        items = s.items,
        warehouse = s.warehouse,
        notes = s.notes.trim(),
        files = s.files,
    )

    /** Same checks as a submit; when the form is fine, the preview page opens instead of anything being sent. */
    private fun openPreview() {
        val problems = validateAudit(draft(_state.value))
        if (problems.isNotEmpty()) {
            _state.update { it.copy(errors = problems) }
            viewModelScope.launch { _effects.send(AuditEffect.FixErrors) }
            return
        }
        _state.update { it.copy(errors = emptySet(), showPreview = true, submitError = null) }
    }

    private fun submit() {
        val s = _state.value
        if (s.isSubmitting) return
        val draft = draft(s)
        val problems = validateAudit(draft)
        if (problems.isNotEmpty()) {
            _state.update { it.copy(errors = problems) }
            viewModelScope.launch { _effects.send(AuditEffect.FixErrors) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val outcome = if (returnId != null) updateAudit(returnId, draft) else submitAudit(visitId, draft)
            when (val result = outcome) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _effects.send(AuditEffect.Submitted(result.data.dliNo))
                }
                is AppResult.Failure -> _state.update { it.copy(isSubmitting = false, submitError = result.error) }
            }
        }
    }
}
