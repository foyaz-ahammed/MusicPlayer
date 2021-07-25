package com.kr.musicplayer.bean.misc;

import android.content.Context;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.ui.fragment.ArtistFragment;
import com.kr.musicplayer.ui.fragment.FavouriteFragment;
import com.kr.musicplayer.ui.fragment.FolderFragment;
import com.kr.musicplayer.ui.fragment.SongFragment;

/**
 * 화면종류에 관한 object
 */

public class Category implements Serializable {

  private static final long serialVersionUID = -6799150022891213071L;
  private String mClassName;
  private int mOrder;
  private int mTag;

  public Category(int tag) {
    mTag = tag;
    mClassName = getClassName();
    mOrder = mTag;
  }

  public String getClassName() {
    return mTag == TAG_SONG ? SongFragment.class.getName() :
            mTag == TAG_ARTIST ? ArtistFragment.class.getName() :
              mTag == TAG_FAVOURITE ? FavouriteFragment.class.getName(): FolderFragment.class.getName();
  }

  public int getTag() {
    return mTag;
  }

  public void setTag(int tag) {
    this.mTag = tag;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || !(o == null || getClass() != o.getClass());
  }

  @Override
  public String toString() {
    return "Category{" +
        "mClassName='" + mClassName + '\'' +
        ", mOrder=" + mOrder +
        ", mTag=" + mTag +
        '}';
  }

  public static final int TAG_SONG = 0;
  public static final int TAG_ALBUM = 1;
  public static final int TAG_ARTIST = 2;
  public static final int TAG_PLAYLIST = 3;
  public static final int TAG_FOLDER = 4;
  public static final int TAG_FAVOURITE = 5;

  public static List<Category> getDefaultLibrary(Context context){
    return Arrays.asList(
        new Category(TAG_SONG),
        new Category(TAG_FOLDER),
        new Category(TAG_FAVOURITE),
        new Category(TAG_ARTIST)
    );
  }

  public CharSequence getTitle() {
    return App.getContext().getString(mTag == TAG_SONG ? R.string.tab_song :
        mTag == TAG_ALBUM ? R.string.tab_album :
        mTag == TAG_ARTIST ? R.string.tab_artist :
        mTag == TAG_PLAYLIST ? R.string.tab_playlist :
        mTag == TAG_FAVOURITE ? R.string.my_favorite :
                R.string.tab_folder);
  }
}
