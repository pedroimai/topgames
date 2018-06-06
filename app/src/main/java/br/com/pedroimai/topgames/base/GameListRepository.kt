package br.com.pedroimai.topgames.base

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.support.annotation.MainThread
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

interface GameListRepository {
    fun getGameList(): Listing<Game>
}

data class Listing<T>(
    // the LiveData of paged lists for the UI to observe
    val pagedList: LiveData<PagedList<T>>,
    // represents the network request status to show to the user
    val networkState: LiveData<NetworkState>,
    // represents the refresh status to show to the user. Separate from networkState, this
    // value is importantly only when refresh is requested.
    val refreshState: LiveData<NetworkState>,
    // refreshes the whole data and fetches it from scratch.
    val refresh: () -> Unit,
    // retries any failed requests.
    val retry: () -> Unit
)


enum class Status {
    RUNNING,
    SUCCESS,
    FAILED
}

@Suppress("DataClassPrivateConstructor")
data class NetworkState private constructor(
    val status: Status,
    val msg: String? = null
) {
    companion object {
        val LOADED = NetworkState(Status.SUCCESS)
        val LOADING = NetworkState(Status.RUNNING)
        fun error(msg: String?) = NetworkState(Status.FAILED, msg)
    }
}

class DbRedditPostRepository(
    val db: TwitchDB,
    private val twitchApi: TwitchApi,
    private val ioExecutor: Executor,
    private val networkPageSize: Int = DEFAULT_NETWORK_PAGE_SIZE
) : GameListRepository {
    companion object {
        private const val DEFAULT_NETWORK_PAGE_SIZE = 10
    }

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    private fun insertResultIntoDb(game: Game, body: GameListResponse?) {
        body?.let { response: GameListResponse ->
            db.runInTransaction {

                val items: List<Game> = response.games.map {
                    Game(id = game.id, image = game.image, name = game.name, thumb = game.thumb)
                }
                db.games().insert(items)
            }
        }
    }

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.LOADING
        twitchApi.getTopGames().enqueue(
            object : Callback<GameListResponse> {
                override fun onFailure(call: Call<GameListResponse>, t: Throwable) {
                    // retrofit calls this on main thread so safe to call set value
                    networkState.value = NetworkState.error(t.message)
                }

                override fun onResponse(
                    call: Call<GameListResponse>,
                    response: Response<GameListResponse>
                ) {
                    ioExecutor.execute {
                        db.runInTransaction {
                            //                            db.getGameList().deleteBySubreddit(subredditName)
//                            insertResultIntoDb(subredditName, response.body())
                        }
                        // since we are in bg thread now, post the result.
                        networkState.postValue(NetworkState.LOADED)
                    }
                }
            }
        )
        return networkState
    }

    /**
     * Returns a Listing for the given subreddit.
     */
    @MainThread
    override fun getGameList(): Listing<Game> {
        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        val boundaryCallback = TwitchBoundaryCallback(
            webservice = twitchApi,
            handleResponse = this::insertResultIntoDb,
            ioExecutor = ioExecutor,
            networkPageSize = networkPageSize
        )
        // create a data source factory from Room
        val dataSourceFactory = db.games().getGameList()
        val builder = LivePagedListBuilder(dataSourceFactory, 20)
            .setBoundaryCallback(boundaryCallback)

        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger, {
            refresh()
        })

        return Listing(
            pagedList = builder.build(),
            networkState = boundaryCallback.networkState,
            retry = {
                boundaryCallback.helper.retryAllFailed()
            },
            refresh = {
                refreshTrigger.value = null
            },
            refreshState = refreshState
        )
    }
}
