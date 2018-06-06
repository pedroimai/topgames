package br.com.pedroimai.topgames.ui.main

import android.arch.lifecycle.Observer
import br.com.pedroimai.topgames.base.ServiceLocator
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.arch.paging.PagedList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import br.com.pedroimai.topgames.R
import br.com.pedroimai.topgames.base.Game
import br.com.pedroimai.topgames.base.GlideApp
import br.com.pedroimai.topgames.listing.ui.main.GameListAdapter
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: TwitchViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = getViewModel()
        initAdapter()
    }


    private fun getViewModel(): TwitchViewModel {
        return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                val repo = ServiceLocator.instance(activity!!)
                    .getRepository()
                @Suppress("UNCHECKED_CAST")
                return TwitchViewModel(repo) as T
            }
        })[TwitchViewModel::class.java]
    }

    private fun initAdapter() {
        val glide = GlideApp.with(this)
        val adapter = GameListAdapter(glide) {
            viewModel.retry()
        }
        list.adapter = adapter
        viewModel.games.observe(this, Observer<PagedList<Game>> { adapter.submitList(it) })
        viewModel.networkState.observe(this, Observer { adapter.setNetworkState(it) })
    }
}
