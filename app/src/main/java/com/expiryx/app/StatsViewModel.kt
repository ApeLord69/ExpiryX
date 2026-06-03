package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel(private val repository: ProductRepository) : ViewModel() {

    private val timeRange = MutableLiveData(TimeRange.DAYS_30)

    private val _statsState = MediatorLiveData<StatsUiState>().apply {
        value = StatsUiState(isLoading = true)
    }
    val statsState: LiveData<StatsUiState> = _statsState

    private var latestProducts: List<Product> = emptyList()
    private var latestHistory: List<History> = emptyList()

    init {
        _statsState.addSource(repository.allProducts) { products ->
            latestProducts = products ?: emptyList()
            recompute()
        }
        _statsState.addSource(repository.allHistory) { history ->
            latestHistory = history ?: emptyList()
            recompute()
        }
        _statsState.addSource(timeRange) {
            recompute()
        }
    }

    fun setTimeRange(range: TimeRange) {
        if (timeRange.value != range) {
            timeRange.value = range
        }
    }

    private fun recompute() {
        val range = timeRange.value ?: TimeRange.DAYS_30
        _statsState.value = _statsState.value?.copy(isLoading = true, timeRange = range)
            ?: StatsUiState(isLoading = true, timeRange = range)

        viewModelScope.launch {
            val state = withContext(Dispatchers.Default) {
                StatsCalculator.compute(latestProducts, latestHistory, range)
            }
            _statsState.value = state
        }
    }
}

class StatsViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
