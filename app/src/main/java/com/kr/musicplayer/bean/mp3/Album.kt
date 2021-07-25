package com.kr.musicplayer.bean.mp3

/**
 * Album정보를 담고 있는 Object
 */

data class Album(val albumID: Int,
                 val album: String,
                 val artistID: Int,
                 val artist: String,
                 val count: Int = 0)
