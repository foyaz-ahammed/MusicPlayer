package com.kr.musicplayer.ui.adapter.holder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.kr.musicplayer.ui.adapter.SongAdapter;

import java.util.List;

import timber.log.Timber;

/**
 * RecyclerView 의 항목이동을 위한 class
 */
public class ItemMoveCallback extends ItemTouchHelper.Callback {
    ItemTouchHelpers mAdapter;

    public ItemMoveCallback(ItemTouchHelpers adapter) {
        mAdapter = adapter;
    }

    /**
     * 어떤 방향으로 drag 하는지 얻는 함수
     * @param recyclerView ItemTouchHelper 가 련결된 RecyclerView
     * @param viewHolder 이동정보가 필요한 viewHolder
     * @return 주어진 viewHolder 가 어느방향으로 이동을 지정하였는지 하는 flag 값
     */
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    /**
     * Animation 시간설정 함수
     * @param recyclerView ItemTouchHelper 가 련결된 RecyclerView
     * @param animationType 진행할려는 animation 형태
     * @param animateDx animation 을 진행할 수평거리
     * @param animateDy animation 을 진행할 수직거리
     * @return 설정된 Animation 시간
     */
    @Override
    public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
        return 100;
    }

    /**
     * 이전 위치로부터 새로운 위치로 끌기한 항목을 이동하였을때 호출되는 함수
     * @param recyclerView ItemTouchHelper 가 련결된 RecyclerView
     * @param viewHolder 사용자에 의해 이동할 viewHolder
     * @param target 현재 이동하는 항목과 교체하려는 viewHolder
     * @return 교체하려는 viewHolder 의 자리로 이동하였으면 true
     */
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        mAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    /**
     * ItemTouchHelper 에 의해 swipe 되거나 Drag 된 viewHolder 가 변경될때 호출되는 함수
     * @param viewHolder Swipe 혹은 Drag 되는 viewHolder
     * @param actionState ItemTouchHelper 의 action 상태중 하나
     */
    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof SongAdapter.SongViewHolder) {
                SongAdapter.SongViewHolder myViewHolder = (SongAdapter.SongViewHolder) viewHolder;
                mAdapter.onRowSelected(myViewHolder);
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    /**
     * Animation 이 끝났을때 호출되는 callback
     * @param recyclerView ItemTouchHelper 에 의해 조종되는 recyclerView
     * @param viewHolder 사용자에 의해 조종된 viewHolder
     */
    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (viewHolder instanceof SongAdapter.SongViewHolder) {
            SongAdapter.SongViewHolder myViewHolder = (SongAdapter.SongViewHolder) viewHolder;
            mAdapter.onRowClear(myViewHolder);
        }
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    /**
     * 끌기한 viewHolder 의 밑에 있는 ViewHolder목록에서 놓기 대상을 선택하기 위해 호출되는 함수
     * @param selected 사용자에 의해 끌기되는 viewHolder
     * @param dropTargets 끌기되는 viewHolder 와 교체될수 있는 viewHolder 목록
     * @param curX 끌기변환이 적용된후 끌기된 viewHolder 의 갱신된 왼쪽값이다.
     * @param curY 끌기변환이 적용된후 끌기된 viewHolder 의 갱신된 꼭대기값이다.
     * @return 끌기한 viewHolder 와 교체해야 하는 viewHolder
     */
    @Override
    public RecyclerView.ViewHolder chooseDropTarget(@NonNull RecyclerView.ViewHolder selected, @NonNull List<RecyclerView.ViewHolder> dropTargets, int curX, int curY) {
        int right = curX + selected.itemView.getWidth();
        int bottom = curY + selected.itemView.getHeight();
        RecyclerView.ViewHolder winner = null;
        int winnerScore = -1;
        final int dx = curX - selected.itemView.getLeft();
        final int dy = curY - selected.itemView.getTop();
        final int targetsSize = dropTargets.size();
        for (int i = 0; i < targetsSize; i++) {
            final RecyclerView.ViewHolder target = dropTargets.get(i);
            if (dx > 0) {
                int diff = target.itemView.getRight() - right;
                if (diff < 0 && target.itemView.getRight() > selected.itemView.getRight()) {
                    final int score = Math.abs(diff);
                    if (score > winnerScore) {
                        winnerScore = score;
                        winner = target;
                    }
                }
            }
            if (dx < 0) {
                int diff = target.itemView.getLeft() - curX;
                if (diff > 0 && target.itemView.getLeft() < selected.itemView.getLeft()) {
                    final int score = Math.abs(diff);
                    if (score > winnerScore) {
                        winnerScore = score;
                        winner = target;
                    }
                }
            }
            if (dy < 0) {
                int diff = target.itemView.getTop() - curY + (int) (target.itemView.getHeight() / 3);
                if (diff > 0 && target.itemView.getTop() < selected.itemView.getTop()) {
                    final int score = Math.abs(diff);
                    if (score > winnerScore) {
                        winnerScore = score;
                        winner = target;
                    }
                }
            }

            if (dy > 0) {
                int diff = target.itemView.getBottom() - bottom - (int) (target.itemView.getHeight() / 3);
                if (diff < 0 && target.itemView.getBottom() > selected.itemView.getBottom()) {
                    final int score = Math.abs(diff);
                    if (score > winnerScore) {
                        winnerScore = score;
                        winner = target;
                    }
                }
            }
        }
        return winner;
    }

    public interface ItemTouchHelpers {
        void onRowMoved(int fromPosition, int toPosition);
        void onRowSelected(SongAdapter.SongViewHolder myViewHolder);
        void onRowClear(SongAdapter.SongViewHolder myViewHolder);
    }
}
