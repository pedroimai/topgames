package br.com.pedroimai.topgames.base

import android.arch.paging.DataSource
import android.arch.persistence.room.*
import android.content.Context

@Database(
    entities = [Game::class],
    version = 1,
    exportSchema = false
)
abstract class TwitchDB : RoomDatabase() {
    abstract fun games(): GameDao
}

fun createTwitchDB(context: Context, useInMemory : Boolean): TwitchDB {
    val databaseBuilder = if(useInMemory) {
        Room.inMemoryDatabaseBuilder(context, TwitchDB::class.java)
    } else {
        Room.databaseBuilder(context, TwitchDB::class.java, "twitch.db")
    }
    return databaseBuilder
        .fallbackToDestructiveMigration()
        .build()
}


@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(games: List<Game>)

    @Query("SELECT * FROM game")
    fun getGameList() : DataSource.Factory<Int, Game>


//    @Query("SELECT MAX(indexInResponse) + 1 FROM getGameList WHERE game = :game.")
//    fun getNextIndexInSubreddit(subreddit: String) : Int

}