package br.com.pedroimai.topgames.listing.ui.main

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import br.com.pedroimai.topgames.R
import br.com.pedroimai.topgames.base.Game
import br.com.pedroimai.topgames.base.GlideRequests
import br.com.pedroimai.topgames.base.NetworkState
import br.com.pedroimai.topgames.base.Status

class GameListAdapter(
    private val glide: GlideRequests,
    private val retryCallback: () -> Unit)
    : PagedListAdapter<Game, RecyclerView.ViewHolder>(POST_COMPARATOR) {
    private var networkState: NetworkState? = null
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.game_item -> (holder as GameViewHolder).bind(getItem(position))
            R.layout.network_state_item -> (holder as NetworkStateItemViewHolder).bindTo(
                networkState)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            getItem(position)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.game_item -> createGameViewHolder(parent, glide)
            R.layout.network_state_item -> createNetworkStateItemViewHolder(parent, retryCallback)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.network_state_item
        } else {
            R.layout.game_item
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    companion object {
        private val PAYLOAD_SCORE = Any()
        val POST_COMPARATOR = object : DiffUtil.ItemCallback<Game>() {
            override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean =
                oldItem == newItem

            override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean =
                oldItem.name == newItem.name

//            override fun getChangePayload(oldItem: Game, newItem: Game): Any? {
//                return if (sameExceptScore(oldItem, newItem)) {
//                    PAYLOAD_SCORE
//                } else {
//                    null
//                }
//            }
        }
    }
}

 fun createGameViewHolder(parent: ViewGroup, glide: GlideRequests): GameViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.game_item, parent, false)
    return GameViewHolder(view, glide)
}

class GameViewHolder(view: View, private val glide: GlideRequests)
    : RecyclerView.ViewHolder(view) {
    private val name: TextView = view.findViewById(R.id.title)
    private val thumbnail : ImageView = view.findViewById(R.id.thumb)
    private var game : Game? = null
    init {
        view.setOnClickListener {
            game?.thumb?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
            }
        }
    }

    fun bind(game: Game?) {
        this.game = game
        name.text = game?.name ?: "loading"
        if (game?.thumb?.startsWith("http") == true) {
            thumbnail.visibility = View.VISIBLE
            glide.load(game.thumb)
                .centerCrop()
                //.placeholder(R.drawable.ic_insert_photo_black_48dp)
                .into(thumbnail)
        } else {
            thumbnail.visibility = View.GONE
            glide.clear(thumbnail)
        }
    }
}

fun createNetworkStateItemViewHolder(parent: ViewGroup, retryCallback: () -> Unit): NetworkStateItemViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.network_state_item, parent, false)
    return NetworkStateItemViewHolder(view, retryCallback)
}

fun toVisbility(constraint : Boolean): Int {
    return if (constraint)  View.VISIBLE
    else View.GONE
}

class NetworkStateItemViewHolder(view: View,
                                 private val retryCallback: () -> Unit)
    : RecyclerView.ViewHolder(view) {
    private val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
    private val retry = view.findViewById<Button>(R.id.retry_button)
    private val errorMsg = view.findViewById<TextView>(R.id.error_msg)
    init {
        retry.setOnClickListener {
            retryCallback()
        }
    }
    fun bindTo(networkState: NetworkState?) {
        progressBar.visibility = toVisbility(networkState?.status == Status.RUNNING)
        retry.visibility = toVisbility(networkState?.status == Status.FAILED)
        errorMsg.visibility = toVisbility(networkState?.msg != null)
        errorMsg.text = networkState?.msg
    }

    companion object {

    }
}