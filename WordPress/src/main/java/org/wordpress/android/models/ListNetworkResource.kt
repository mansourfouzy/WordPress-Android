package org.wordpress.android.models

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData

class ListNetworkResource<T> constructor(private val paginationAvailable: Boolean = true) {
    enum class Status {
        READY, // Initial state or manual reset (mostly for search)

        // Success states
        SUCCESS, // All data has been successfully fetched - unless user triggers a manual fetch, it's the final state
        CAN_LOAD_MORE, // Some, but not all, data has been successfully fetched - next page can be requested

        // Error states
        FETCH_ERROR, // Initial fetch has failed
        PAGINATION_ERROR, // Initial fetch was successful, but there was a pagination error
        CONNECTION_ERROR, // Fetch action never dispatched due to no connectivity

        // Loading states
        FETCHING_FIRST_PAGE, // Fetching or refreshing first page
        LOADING_MORE // Pagination
    }
    private var _data: MutableLiveData<List<T>> = MutableLiveData()
    private var _status: MutableLiveData<Status> = MutableLiveData()
    private var errorMessage: String? = null

    val data: LiveData<List<T>>
        get() = _data

    val status: LiveData<Status>
        get() = _status

    init {
        _status.value = Status.READY
    }

    // Checking Status

    fun shouldFetch(loadMore: Boolean) = if (loadMore) shouldLoadMore() else shouldFetchFirstPage()

    private fun shouldLoadMore() = paginationAvailable && status.value == Status.CAN_LOAD_MORE

    private fun shouldFetchFirstPage() = status.value != Status.FETCHING_FIRST_PAGE

    // Updating Status

    fun fetching(loadingMore: Boolean = false) = if (loadingMore) loadingMore() else fetchingFirstPage()

    private fun fetchingFirstPage() = updateStatusIfChanged(Status.FETCHING_FIRST_PAGE)

    private fun loadingMore() = updateStatusIfChanged(Status.LOADING_MORE)

    fun connectionError() = updateStatusIfChanged(Status.CONNECTION_ERROR)

    fun fetchError(message: String?, wasLoadingMore: Boolean = false) {
        // Update the error message before the status, so the observer can use it
        errorMessage = message
        val newStatus = if (wasLoadingMore) Status.PAGINATION_ERROR else Status.FETCH_ERROR
        updateStatusIfChanged(newStatus)
    }

    fun resetStatus() {
        updateStatusIfChanged(Status.READY)
    }

    // Data Management

    fun fetchedSuccessfully(newData: List<T>, canLoadMore: Boolean = false) {
        _data.postValue(newData)
        updateStatusIfChanged(if (canLoadMore) Status.CAN_LOAD_MORE else Status.SUCCESS)
    }

    fun manuallyUpdateData(newData: List<T>) {
        _data.postValue(newData)
    }

    // Helpers

    fun isEmpty() = data.value == null || data.value!!.isEmpty()

    fun isFetchingFirstPage() = status.value == Status.FETCHING_FIRST_PAGE

    fun isError(): Boolean {
        return status.value == Status.FETCH_ERROR
                || status.value == Status.PAGINATION_ERROR
                || status.value == Status.CONNECTION_ERROR
    }

    // Utils

    private fun updateStatusIfChanged(newStatus: Status) {
        if (status.value != newStatus) {
            _status.postValue(newStatus)
        }
    }
}