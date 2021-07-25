package com.kr.musicplayer.ui.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;

/**
 * 기초 adapter
 */
public abstract class BaseAdapter<D, T extends BaseViewHolder> extends ListAdapter<D, T> {

  protected OnItemClickListener mOnItemClickListener;
  private int mLayoutId; // 표시할 개별적인 항목의 layout Id
  private Constructor mConstructor;


  public BaseAdapter(int layoutId, DiffUtil.ItemCallback<D> diff_callback) {
    super(diff_callback);
    this.mLayoutId = layoutId;
    try {
      this.mConstructor = getGenericClass().getDeclaredConstructor(View.class);
      this.mConstructor.setAccessible(true);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.toString());
    }
  }

  @Override
  public void onBindViewHolder(T holder, int position) {
    convert(holder, getItem(position), position);
  }

  @Override
  public D getItem(int position) {
    return super.getItem(position);
  }

  protected abstract void convert(final T holder, D d, final int position);

  @SuppressWarnings("unchecked")
  @Override
  public T onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    try {
      View itemView = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
      return (T) mConstructor.newInstance(itemView);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.toString());
    }
  }

  public void setOnItemClickListener(OnItemClickListener l) {
    this.mOnItemClickListener = l;
  }

  @SuppressWarnings("unchecked")
  protected final Class<T> getGenericClass() {
    Type genType = getClass().getGenericSuperclass();
    Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

    if (params != null && params.length > 1 && params[1] instanceof Class<?>) {
      return (Class<T>) params[1];
    } else {
      throw new IllegalArgumentException("Generic error");
    }
  }
}
