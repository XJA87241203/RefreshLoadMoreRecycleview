package com.xie.rlrecycleview.view;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.xie.rlrecycleview.LoadMoreFooter;
import com.xie.rlrecycleview.RefreshHeader;

import java.util.Objects;

/**
 * Created by iSmartGo-XIE on 2017/7/6.
 * 支持自动加载更多功能
 * 支持添加头部和尾部
 * 配合AutoLoadRecyclerView使用
 * 需要用到自动加载的话需要实现OnLoadMoreListener用于处理加载逻辑
 * 设置自动加载需要设置isAutoLoadMore为true
 */

public abstract class AutoLoadRecyclerAdapter extends RecyclerView.Adapter<BaseRecyclerViewHolder> {

    private static final int BASE_ITEM_TYPE_HEADER = 100001;
    private static final int SPECIAL_ITEM_TYPE_REFRESH_HEADER = 100000;
    private static final int BASE_ITEM_TYPE_NULL_DATA_HEADER = 200000;//空布局头部
    private static final int BASE_ITEM_TYPE_FOOTER = 200001;
    private static final int SPECIAL_ITEM_TYPE_LOAD_FOOTER = 1000000;

    protected Context context;

    //加载更多监听
    private OnLoadMoreListener onLoadMoreListener;

    //加载更多布局
    private BaseLoadMoreFooter loadMoreFooterView;
    //自动加载开关
    private boolean isAutoLoadMore = false;

    //自动加载开关
    private boolean isPullToRefresh = false;
    //剩下多少个item时才开始加载
    private int loadMoreKey = 0;

    //容器
    private SparseArrayCompat<View> mHeaderViews = new SparseArrayCompat<>();
    private SparseArrayCompat<View> mFootViews = new SparseArrayCompat<>();

    //代替onCreateViewHolder
    protected abstract BaseRecyclerViewHolder onCreateViewHolderNew(ViewGroup parent, int viewType);

    //代替getItemViewType
    protected abstract int getItemViewTypeNew(int position);

    //代替onBindViewHolder
    protected abstract void onBindViewHolderNew(BaseRecyclerViewHolder holder, int position);

    //获取内容Item数量
    protected abstract int getRealItemCount();

    public AutoLoadRecyclerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mHeaderViews.get(viewType) != null) {
            //头部
            return BaseRecyclerViewHolder.createViewHolder(mHeaderViews.get(viewType));
        } else if (mFootViews.get(viewType) != null) {
            //尾部
            return BaseRecyclerViewHolder.createViewHolder(mFootViews.get(viewType));
        }
        //内容部分
        return onCreateViewHolderNew(parent, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderViewPos(position)) {
            return mHeaderViews.keyAt(position);
        } else if (isFooterViewPos(position)) {
            return mFootViews.keyAt(position - getHeadersCount() - getRealItemCount());
        }
        return getItemViewTypeNew(position - getHeadersCount());
    }

    @Override
    public void onBindViewHolder(@NonNull BaseRecyclerViewHolder holder, int position) {
        if (isHeaderViewPos(position)) {
            return;
        }
        if (isFooterViewPos(position)) {
            return;
        }
        onBindViewHolderNew(holder, position - getHeadersCount());
    }

    @Override
    public int getItemCount() {
        return getHeadersCount() + getFootersCount() + getRealItemCount();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull BaseRecyclerViewHolder holder) {
        //处理StaggeredGridLayout类型
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            position = holder.getLayoutPosition();
        }
        if (isHeaderViewPos(position) || isFooterViewPos(position)) {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();

            if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        //处理gridLayout类型

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            final GridLayoutManager.SpanSizeLookup spanSizeLookup = gridLayoutManager.getSpanSizeLookup();

            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int viewType = getItemViewType(position);
                    if (mHeaderViews.get(viewType) != null) {
                        return gridLayoutManager.getSpanCount();
                    } else if (mFootViews.get(viewType) != null) {
                        return gridLayoutManager.getSpanCount();
                    }
                    if (spanSizeLookup != null)
                        return spanSizeLookup.getSpanSize(position);
                    return 1;
                }
            });
            gridLayoutManager.setSpanCount(gridLayoutManager.getSpanCount());
        }
    }

    /**
     * 判断是不是Header
     *
     * @param position position
     * @return boolean
     */
    private boolean isHeaderViewPos(int position) {
        return position < getHeadersCount();
    }

    /**
     * 判断是不是Footer
     *
     * @param position position
     * @return boolean
     */
    private boolean isFooterViewPos(int position) {
        return position >= getHeadersCount() + getRealItemCount();
    }

    /**
     * 添加Header
     *
     * @param view view
     */
    public void addHeaderView(View view) {
        mHeaderViews.put(mHeaderViews.size() + BASE_ITEM_TYPE_HEADER, view);
    }

    /**
     * 添加并隐藏空布局
     *
     * @return 空布局View
     */
    public View addNullDataUIHeaderView(@LayoutRes int id) {
        //在外面套布局，防止GONE时显示异常
        LinearLayout linearLayout = new LinearLayout(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(lp);
        LayoutInflater.from(context).inflate(id, linearLayout);
        mHeaderViews.put(BASE_ITEM_TYPE_NULL_DATA_HEADER, linearLayout);
        setNullDataUIHeaderVisibility(false);
        return linearLayout;
    }

    /**
     * 移除空布局
     */
    public void removeNullDataUIHeaderView() {
        int index = mHeaderViews.indexOfKey(BASE_ITEM_TYPE_NULL_DATA_HEADER);
        if (index != -1) {
            mHeaderViews.remove(BASE_ITEM_TYPE_NULL_DATA_HEADER);
        }
    }

    /**
     * 设置空布局隐藏或显示
     *
     * @param isVisible isVisible
     */
    public void setNullDataUIHeaderVisibility(boolean isVisible) {
        if (mHeaderViews.containsKey(BASE_ITEM_TYPE_NULL_DATA_HEADER)) {
            if (isVisible) {
                Objects.requireNonNull(mHeaderViews.get(BASE_ITEM_TYPE_NULL_DATA_HEADER)).setVisibility(View.VISIBLE);
            } else {
                Objects.requireNonNull(mHeaderViews.get(BASE_ITEM_TYPE_NULL_DATA_HEADER)).setVisibility(View.GONE);
            }
        }
    }

    /**
     * 删除Header
     *
     * @param view view
     */
    public void removeHeaderView(View view) {
        int index = mHeaderViews.indexOfValue(view);
        if (index != -1) {
            mHeaderViews.removeAt(index);
        }
    }


    /**
     * 添加Footer
     *
     * @param view view
     */
    public void addFooterView(View view) {
        mFootViews.put(mFootViews.size() + BASE_ITEM_TYPE_FOOTER, view);
    }

    /**
     * 删除Footer
     *
     * @param view view
     */
    public void removeFooterView(View view) {
        int index = mFootViews.indexOfValue(view);
        if (index != -1) {
            mFootViews.removeAt(index);
        }
    }

    /**
     * 设置刷新footer
     *
     * @param view view
     */
    private void setLoadMoreFooter(View view) {
        mFootViews.put(SPECIAL_ITEM_TYPE_LOAD_FOOTER + BASE_ITEM_TYPE_FOOTER, view);
    }

    /**
     * 获取Header Item数量
     *
     * @return int
     */
    public int getHeadersCount() {
        return mHeaderViews.size();
    }

    /**
     * 获取HeaderViews
     *
     * @return SparseArrayCompat
     */
    public SparseArrayCompat<View> getHeaderViews() {
        return mHeaderViews;
    }

    /**
     * 获取HeaderViews
     *
     * @return SparseArrayCompat
     */
    public SparseArrayCompat<View> getFootViews() {
        return mFootViews;
    }

    /**
     * 获取Footer Item数量
     *
     * @return int
     */
    public int getFootersCount() {
        return mFootViews.size();
    }


    /**
     * 开始加载
     */
    public void startLoadMore() {
        //过滤同一页面重复请求
        if (onLoadMoreListener == null || loadMoreFooterView == null || loadMoreFooterView.getState() != BaseLoadMoreFooter.STATE_LOAD_FINISH)
            return;
        loadMoreFooterView.setLoadMoreState(BaseLoadMoreFooter.STATE_LOADING);
        onLoadMoreListener.onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
        if (onLoadMoreListener != null) initLoadMoreView();

    }

    private void initLoadMoreView() {
        if (loadMoreFooterView == null) {
            loadMoreFooterView = new LoadMoreFooter(context);
            loadMoreFooterView.setLoadMoreState(BaseLoadMoreFooter.STATE_LOAD_FINISH);
            setLoadMoreFooter(loadMoreFooterView);
        }
    }

    /**
     * 加载完成
     */
    public void finishLoadMore() {
        if (loadMoreFooterView == null) return;
        loadMoreFooterView.setLoadMoreState(BaseLoadMoreFooter.STATE_LOAD_FINISH);
    }

    /**
     * 停止加载,显示“没有更多了”
     */
    public void showNoMoreHint() {
        if (loadMoreFooterView == null) return;
        loadMoreFooterView.setLoadMoreState(BaseLoadMoreFooter.STATE_NO_MORE);
    }

    /**
     * 重设自动加载状态
     */
    public void resetLoadMoreState() {
        if (loadMoreFooterView == null) return;
        loadMoreFooterView.setLoadMoreState(BaseLoadMoreFooter.STATE_LOAD_FINISH);
    }

    boolean isPullLoading() {
        return loadMoreFooterView.getState() != BaseLoadMoreFooter.STATE_LOAD_FINISH;
    }

    public boolean isLoading() {
        return loadMoreFooterView.getState() == BaseLoadMoreFooter.STATE_LOADING;
    }

    public boolean isNoMore() {
        return loadMoreFooterView.getState() == BaseLoadMoreFooter.STATE_NO_MORE;
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    /**
     * 自动加载开关
     *
     * @return isAutoLoadMore
     */
    public boolean isAutoLoadMore() {
        return isAutoLoadMore;
    }

    /**
     * 设置自动加载
     *
     * @param autoLoadMore boolean 是否可自动刷新
     * @param loadMoreKey  剩下多少个item没划的时候开始加载，默认是0
     */
    public void setAutoLoadEnable(boolean autoLoadMore, int loadMoreKey) {
        isAutoLoadMore = autoLoadMore;
        this.loadMoreKey = loadMoreKey;
    }

    /**
     * 获取自动加载关键值
     *
     * @return 剩下多少个item没划的时候开始加载，默认是0
     */
    public int getLoadMoreKey() {
        return loadMoreKey;
    }

    //--------------------------------下拉刷新部分--------------------------------//
    private BaseRefreshHeader refreshHeader;
    private float startY = -1;

    public interface OnRefreshListener {
        void onRefresh();
    }

    void onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = e.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = (e.getY() - startY)/2.5F;//为了防止滑动幅度过大，将实际手指滑动的距离除以2.5
                //防止异常回弹(需要根据屏幕密度判断)
//                if(Math.abs(deltaY)<100){
                    refreshHeader.onMove(deltaY);
//                }
                startY = e.getY();
                break;
            case MotionEvent.ACTION_UP:
                refreshHeader.onRelease();
                break;
        }
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        //下拉刷新监听
        if (onRefreshListener != null && refreshHeader == null) {
            refreshHeader = new RefreshHeader(context);
            setRefreshHeader(refreshHeader);
            refreshHeader.setOnRefreshListener(onRefreshListener);
            refreshHeader.setVisibleHeight(1);
        }
    }

    public void finishRefresh(){
        refreshHeader.onRefreshFinish();
    }

    /**
     * 设置下拉刷新
     *
     * @param pullToRefresh 是否开启下拉刷新
     */
    public void setPullToRefresh(boolean pullToRefresh) {
        isPullToRefresh = pullToRefresh;
    }

    /**
     * 获取下拉刷新开关状态
     *
     * @return 下拉刷新开关状态
     */
    public boolean isPullToRefresh() {
        return isPullToRefresh;
    }

    /**
     * 设置刷新头部
     *
     * @param refreshHeader refreshHeader
     */
    private void setRefreshHeader(BaseRefreshHeader refreshHeader) {
        mHeaderViews.put(SPECIAL_ITEM_TYPE_REFRESH_HEADER, refreshHeader);
    }
}