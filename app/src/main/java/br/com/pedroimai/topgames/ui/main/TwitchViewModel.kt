package br.com.pedroimai.topgames.ui.main

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import br.com.pedroimai.topgames.base.Game
import br.com.pedroimai.topgames.base.GameListRepository

class TwitchViewModel(private val repository: GameListRepository) : ViewModel() {
    private val gameLiveData = MutableLiveData<Game>()
    private val repoResult = Transformations.map(gameLiveData, {
        repository.getGameList()
    })
    val games = Transformations.switchMap(repoResult, { it.pagedList })!!
    val networkState = Transformations.switchMap(repoResult, { it.networkState })!!
    val refreshState = Transformations.switchMap(repoResult, { it.refreshState })!!

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun showGame(game: Game): Boolean {
        if (gameLiveData.value == game) {
            return false
        }
        gameLiveData.value = game
        return true
    }

    fun retry() {
        val listing = repoResult?.value
        listing?.retry?.invoke()
    }

    fun currentGame(): Game? = gameLiveData.value
}
