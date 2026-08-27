package com.myt.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.history.HistorySortOrder
import com.myt.domain.history.HistoryTab
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.usecase.HistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyUseCase: HistoryUseCase,
) : ViewModel() {
    private val _filter = MutableStateFlow(HistoryFilterState())
    val filter: StateFlow<HistoryFilterState> = _filter.asStateFlow()

    private val _trips = MutableStateFlow(emptyList<com.myt.domain.history.TripHistoryItem>())
    val trips = _trips.asStateFlow()

    private val _charges = MutableStateFlow(emptyList<com.myt.domain.history.ChargeHistoryItem>())
    val charges = _charges.asStateFlow()

    private val _fleetEvents = MutableStateFlow(emptyList<com.myt.domain.history.FleetApiHistoryItem>())
    val fleetEvents = _fleetEvents.asStateFlow()

    private val _chart = MutableStateFlow(emptyList<com.myt.domain.history.DailyAggregate>())
    val chart = _chart.asStateFlow()

    init {
        refresh()
    }

    fun setTab(tab: HistoryTab) {
        _filter.value = _filter.value.copy(tab = tab)
        refresh()
    }

    fun setPeriod(period: HistoryPeriodFilter) {
        _filter.value = _filter.value.copy(period = period)
        refresh()
    }

    fun setSort(sort: HistorySortOrder) {
        _filter.value = _filter.value.copy(sort = sort)
        refresh()
    }

    fun toggleFailuresOnly() {
        _filter.value = _filter.value.copy(onlyFailures = !_filter.value.onlyFailures)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val f = _filter.value
            val tripList = historyUseCase.trips(f)
            val chargeList = historyUseCase.chargeSessions(f)
            val fleetList = historyUseCase.fleetEvents(f)
            _trips.value = tripList
            _charges.value = chargeList
            _fleetEvents.value = fleetList
            _chart.value = when (f.tab) {
                HistoryTab.Driving -> historyUseCase.tripChart(tripList)
                HistoryTab.Charging -> historyUseCase.chargeChart(chargeList)
                HistoryTab.FleetApi -> historyUseCase.fleetChart(fleetList)
            }
        }
    }

    suspend fun loadTrip(id: String): TripHistoryItem? =
        _trips.value.find { it.id == id } ?: historyUseCase.tripById(id)
}
