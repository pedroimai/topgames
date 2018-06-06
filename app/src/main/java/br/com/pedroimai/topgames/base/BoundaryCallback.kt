package br.com.pedroimai.topgames.base

import android.arch.paging.PagedList
import android.support.annotation.MainThread
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor
import kotlin.reflect.KFunction2

class TwitchBoundaryCallback(
    private val webservice: TwitchApi,
    private val handleResponse: (GameListResponse?) -> Unit,
    private val ioExecutor: Executor,
    private val networkPageSize: Int
) : PagedList.BoundaryCallback<Game>() {

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            webservice.getTopGames()
                .enqueue(createWebserviceCallback(it))
        }
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: Game) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            webservice.getTopGames()
                .enqueue(createWebserviceCallback(it))
        }
    }

    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
        response: Response<GameListResponse>,
        it: PagingRequestHelper.Request.Callback
    ) {
        ioExecutor.execute {
            handleResponse(response.body())
            it.recordSuccess()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Game) {
        // ignored, since we only ever append to what's in the DB
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback): Callback<GameListResponse> {
        return object : Callback<GameListResponse> {
            override fun onFailure(
                call: Call<GameListResponse>,
                t: Throwable
            ) {
                it.recordFailure(t)
            }

            override fun onResponse(
                call: Call<GameListResponse>,
                response: Response<GameListResponse>
            ) {
                insertItemsIntoDb(response, it)
            }
        }
    }
}