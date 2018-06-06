package br.com.pedroimai.topgames.base

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class Game(@PrimaryKey val id: String, val name: String, val image: String, val thumb: String)

