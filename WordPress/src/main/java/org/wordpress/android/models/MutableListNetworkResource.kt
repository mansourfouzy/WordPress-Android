package org.wordpress.android.models

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.models.ListNetworkResource.Status
import kotlin.properties.Delegates

class MutableListNetworkResource<T>(private val paginationAvailable: Boolean = true) : ListNetworkResource<T> {
    private val _data: MutableLiveData<List<T>> = MutableLiveData()
    private val _status: MutableLiveData<Status> = MutableLiveData()
    private var errorMessage: String? = null

    override val data: LiveData<List<T>>
        get() = _data

    override val status: LiveData<Status>
        get() = _status

    private var actualData: List<T> by Delegates.observable(ArrayList()) { _, old, new ->
        if (new != old) {
            _data.postValue(new)
        }
    }

    private var actualStatus by Delegates.observable(Status.READY) { _, old, new ->
        if (new != old) {
            _status.postValue(new)
        }
    }

    // Checking Status

    override fun shouldFetch(loadMore: Boolean) = if (loadMore) shouldLoadMore() else shouldFetchFirstPage()

    private fun shouldLoadMore() = paginationAvailable && actualStatus == Status.CAN_LOAD_MORE

    private fun shouldFetchFirstPage() = actualStatus != Status.FETCHING_FIRST_PAGE

    // Updating Status

    fun fetching(loadingMore: Boolean = false) {
        actualStatus = if (loadingMore) Status.LOADING_MORE else Status.FETCHING_FIRST_PAGE
    }

    fun connectionError() {
        actualStatus = Status.CONNECTION_ERROR
    }

    fun fetchError(message: String?, wasLoadingMore: Boolean = false) {
        // Update the error message before the status, so the observer can use it
        errorMessage = message
        actualStatus = if (wasLoadingMore) Status.PAGINATION_ERROR else Status.FETCH_ERROR
    }

    fun resetStatus() {
        actualStatus = Status.READY
    }

    // Data Management

    fun fetchedSuccessfully(newData: List<T>, canLoadMore: Boolean = false) {
        actualData = newData
        actualStatus = if (canLoadMore) Status.CAN_LOAD_MORE else Status.SUCCESS
    }

    fun manuallyUpdateData(newData: List<T>) {
        actualData = newData
    }
}
