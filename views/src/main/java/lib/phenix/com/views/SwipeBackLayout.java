package lib.phenix.com.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TODO 两个方向以下能保持良好体验
 *
 * @author zhouphenix on 2017-2-23.
 *         <p>
 *         ViewDragHelper 中的几个个方法理解
 *         boolean smoothSlideViewTo(View child, int finalLeft, int finalTop)
 *         将视图child animate到给定（finalLeft，finalTop）位置。
 *         如果返回true ，调用者应该还要调用 {@link ViewDragHelper#continueSettling(boolean)}
 *         在每个后续帧上继续运动，直到它返回false
 *         <p>
 *         boolean settleCapturedViewAt(int finalLeft, int finalTop)
 *         将捕获到View设置到给定（finalLeft，finalTop）位置。
 *         并且将先前运动的适当速度考虑在内。
 *         如果返回true ，调用者应该还要调用 {@link ViewDragHelper#continueSettling(boolean)}
 *         在每个后续帧上继续运动，直到它返回false
 *         <p>
 *         boolean continueSettling(boolean deferCallbacks)
 *         deferCallbacks：如果状态回调应通过发布消息延迟，则为true。
 *         如果是在{@link View#computeScroll()}或者类似的布局、绘图中调用，请设置为true
 *         <p>
 *         使用：
 *         step1:
 *         <style name="AppTheme.NoActionBar.Translucent">
 *         <item name="android:windowIsTranslucent">true</item>
 *         <item name="android:windowBackground">@android:color/transparent</item>
 *         </style>
 *         step2：
 *         布局相应位置节点设置背景色属性， 如： android:background="@android:color/white"
 *         step3：
 *         public void setContentView(View view) {
 *         SwipeBackLayout swipeBackLayout = new SwipeBackLayout(this, view, SwipeBackLayout.UP | SwipeBackLayout.LEFT);
 *         swipeBackLayout.setOnSwipeBackListener(new SwipeBackLayout.OnSwipeBackListener() {
 *          @Override public boolean onIntercept(@SwipeBackLayout.DragDirection int direction) {
 *          return onSwipeBack(direction);
 *          }
 *          @Override public void onViewPositionChanged(float fraction) {
 *          }
 *          @Override public void onAnimationEnd() {
 *          finish();
 *          overridePendingTransition(0, android.R.anim.fade_out);
 *          }
 *          });
 *          super.setContentView(swipeBackLayout);
 *             }
 */
public class SwipeBackLayout extends FrameLayout {

    /**
     * 定义代表方向的常量
     */
    public static final int LEFT = 1 << 0;
    public static final int UP = 1 << 1;
    public static final int RIGHT = 1 << 2;
    public static final int DOWN = 1 << 3;

    @IntDef({LEFT, UP, RIGHT, DOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DragDirection {
    }

    /**
     * 滑动方向mask
     */
    int mDragDirectionMask = LEFT;
    /**
     * 当前drag方向
     */
    int mCurDragDirection;

    /**
     * View 拖拽帮助类
     * 使用步骤①②③④⑤
     */
    private final ViewDragHelper mViewDragHelper;

    /**
     * 主视图view
     */
    private View mContentView;
    private View mShadowView;
    private int mShadowColor = Color.parseColor("#8f000000");

    /**
     * 设置是否可以swipe back
     */
    private boolean enableSwipeBack;

    /**
     * 记录scroll的child View
     */
    private View mScrollChild;

    /**
     * 水平drag的范围
     */
    int mVerticalDragRange;
    /**
     * 竖直drag的范围
     */
    int mHorizontalDragRange;

    /**
     * finish 因子，当前界面的范围[0,1]
     */
    float mFinishFactor = 0.3f;

    /**
     * drag偏移量
     */
    int mDragOffset;

    /**
     * 被拖拽View的原始位置坐标
     */
    int mOriginalX;
    int mOriginalY;
    /**
     * touch当前坐标
     */
    float mTouchX;
    float mTouchY;

    /**
     * 通过new SwipeBackLayout(Context context, View contentView)调用
     *
     * @param context       上下文
     * @param contentView   主体布局
     * @param directionMask 类似LEFT | UP
     */
    public SwipeBackLayout(@NonNull Context context, @NonNull View contentView, int directionMask) {
        super(context);
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelperCallback());
        this.mContentView = contentView;
        this.mDragDirectionMask = directionMask;
        enableSwipeBack = true;
        addShadowView(context);
        addView(mContentView);
    }


    /**
     * \
     * 通过xml布局 自定义属性调用
     *
     * @param context 上下文
     * @param attrs   xml属性
     */
    public SwipeBackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //①获取ViewDragHelper的实例
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelperCallback());
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SwipeBackLayout);
        mDragDirectionMask = ta.getInt(R.styleable.SwipeBackLayout_dragDirection, LEFT);
        mShadowColor = ta.getColor(R.styleable.SwipeBackLayout_shadowColor, mShadowColor);
        ta.recycle();

        addShadowView(context);
        enableSwipeBack = true;
    }

    private void addShadowView(Context context) {
        mShadowView = new View(context);
        mShadowView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mShadowView.setBackgroundColor(mShadowColor);
        addView(mShadowView);
    }


    public void setShadowColor(int color) {
        mShadowView.setBackgroundColor(color);
        mShadowView.invalidate();
    }


    public boolean isEnableSwipeBack() {
        return enableSwipeBack;
    }

    public void setEnableSwipeBack(boolean enableSwipeBack) {
        this.enableSwipeBack = enableSwipeBack;
    }

    public void setDragDirectionMask(int mDragDirectionMask) {
        this.mDragDirectionMask = mDragDirectionMask;
    }

    /**
     * 添加可以direction划动
     *
     * @param direction DragDirection
     */
    public void enableDragDirection(int direction) {
        mDragDirectionMask |= direction;
    }

    /**
     * 删除可以direction划动
     *
     * @param direction DragDirection
     */
    public void disableDragDirection(int direction) {
        mDragDirectionMask &= ~direction;
    }

    /**
     * 判断是否可以direction这个方向的划动
     *
     * @param direction DragDirection
     * @return boolean
     */
    private boolean isAllowDragDirection(int direction) {
        return direction == (mDragDirectionMask & direction);
    }


    /**
     * 是否禁用了direction
     *
     * @param direction 禁用了该方向
     * @return boolean
     */
    public boolean isNotAllowDragDirection(int direction) {
        return (mDragDirectionMask & direction) == 0;
    }

    /**
     * 是否仅仅允许direction
     *
     * @param direction 允许方向
     * @return boolean
     */
    public boolean isOnlyAllowDragDirection(int direction) {
        return mDragDirectionMask == direction;
    }

    /**
     * 动态new的方式不会调用该方法
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (null == mContentView && getChildCount() > 0) {
            mContentView = getChildAt(getChildCount() - 1);
        }

    }

    /**
     * Find out the scrollable child view
     * 这里添加了常用的一些可滑动类，特殊类需要添加
     * @param target targetView
     */
    private void findScrollView(ViewGroup target) {
        final int count = target.getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                final View child = target.getChildAt(i);
                if (child instanceof AbsListView
                        || isInstanceOfClass(child , ScrollView.class.getName())
                        || isInstanceOfClass(child , NestedScrollView.class.getName())
                        || isInstanceOfClass(child , RecyclerView.class.getName())
                        || child instanceof HorizontalScrollView
                        || child instanceof ViewPager
                        || child instanceof WebView){
                    mScrollChild = child;
                    break;
                }else if (child instanceof ViewGroup){
                    findScrollView((ViewGroup) child);
                }
            }
        }
        if (mScrollChild == null) mScrollChild = target;
//        Log.e("zhou", "mScroolChild=="+mScrollChild.getClass().getName());
    }


    private boolean isInstanceOfClass(Object o1, String className){
        return o1.getClass().getName() == className;

    }


    /**
     * 当view的大小发生变化时触发
     * 至少触发好一次
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mVerticalDragRange = h;
        mHorizontalDragRange = w;
    }

    /**
     * 父view 要求他内部的子view的mScrollX和mScrollY发生变化时,通常情况下子view使用Scroller对象进行动画滚动
     * 重写computeScroll()的原因:
     * 调用startScroll()是不会有滚动效果的，只有在computeScroll()获取滚动情况，做出滚动的响应
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * ②继承ViewDragHelper.Callback类
     * 拖拽回调
     */
    class ViewDragHelperCallback extends ViewDragHelper.Callback {
        private int mLastDragState;

        /**
         * 尝试捕获子view
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mContentView && enableSwipeBack;
        }


        @Override
        public int getViewHorizontalDragRange(View child) {
            return mHorizontalDragRange;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mVerticalDragRange;
        }

        /**
         * ③重写两个方法int clampViewPositionHorizontal(View child, int left, int dx)
         * 和int clampViewPositionHorizontal(View child, int left, int dx)
         * 这两个方法分别用来处理x方向和y方向的拖动的，返回值该child现在的位置。
         */
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int leftBounds;
            int rightBounds;
            if (isAllowDragDirection(LEFT) && !childCanScrollRight() && left >= 0 &&  mCurDragDirection == LEFT) {
                leftBounds = getPaddingLeft();
                rightBounds = mHorizontalDragRange;
                if (null != mOnSwipeBackCallback
                        && mOnSwipeBackCallback.onIntercept(mCurDragDirection, mTouchX, mTouchY)
                        && mCurDragDirection != 0
                        ) {
                    mCurDragDirection = 0;
                    return mOriginalX;
                }
                return Math.min(Math.max(left, leftBounds), rightBounds);
            }
            if (isAllowDragDirection(RIGHT) && !childCanScrollLeft() && left <= 0 && mCurDragDirection == RIGHT) {
                leftBounds = -mHorizontalDragRange;
                rightBounds = getPaddingLeft();
                if (null != mOnSwipeBackCallback
                        && mOnSwipeBackCallback.onIntercept(mCurDragDirection, mTouchX, mTouchY)
                        && mCurDragDirection != 0
                        ) {
                    mCurDragDirection = 0;
                    return mOriginalX;
                }
                return Math.min(Math.max(left, leftBounds), rightBounds);
            }

            return mOriginalX;
        }

        /***
         * clampViewPositionHorizontal()之后调用
         */
        @Override
        public int clampViewPositionVertical(final View child,final int top, final int dy) {
            if (mScrollChild instanceof ViewPager) {
                final ViewPager pager = (ViewPager) mScrollChild;
                    mScrollChild = pager.getChildAt(pager.getCurrentItem());
                final int cacheCount = pager.getChildCount();
                int[] points = new int[2];
                for (int i = 0; i < cacheCount; i++) {
                    View view = pager.getChildAt(i);
                    view.getLocationInWindow(points);
                    if ( mTouchX >= points[0] && mTouchX <points[0]+view.getWidth()) {
                        mScrollChild = view;
                        break;
                    }

                }
            }
            int topBounds;
            int bottomBounds;
            if (isAllowDragDirection(UP)
                    && !childCanScrollDown()
                    && top >= 0
                    && mCurDragDirection == UP) {
                topBounds = getPaddingTop();
                bottomBounds = mVerticalDragRange;
                if (null != mOnSwipeBackCallback
                        && mOnSwipeBackCallback.onIntercept(mCurDragDirection, mTouchX, mTouchY)
                        && mCurDragDirection != 0
                        ) {
                    mCurDragDirection = 0;
                    return mOriginalY;
                }
                return Math.min(Math.max(top, topBounds), bottomBounds);
            }
            if (isAllowDragDirection(DOWN)
                    && !childCanScrollUp()
                    && top <= 0
                    && mCurDragDirection == DOWN) {
                topBounds = -mVerticalDragRange;
                bottomBounds = getPaddingTop();
                if (null != mOnSwipeBackCallback
                        && mOnSwipeBackCallback.onIntercept(mCurDragDirection, mTouchX, mTouchY)
                        && mCurDragDirection != 0
                        ) {
                    mCurDragDirection = 0;
                    return mOriginalY;
                }
                return Math.min(Math.max(top, topBounds), bottomBounds);
            }
            return mOriginalY;
        }


        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            mDragOffset = mCurDragDirection == LEFT || mCurDragDirection == RIGHT ? Math.abs(left) : Math.abs(top);
            final float fraction = mDragOffset * 1.0f / getDragRange();
            mShadowView.setAlpha(1 - fraction);
            if (null != mOnSwipeBackCallback) {
                mOnSwipeBackCallback.onViewPositionChanged(fraction);
            }
        }

        private int getDragRange() {
            return mCurDragDirection == LEFT || mCurDragDirection == RIGHT ? mHorizontalDragRange : mVerticalDragRange;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            /***
             * 【用户体验问题】
             * 处理当状态从ViewDragHelper.STATE_SETTLING --> STATE_DRAGGING  --> STATE_IDLE 这样一个过程时，造成拖拽的View动画强制结束，未回到指定位置
             * 【问题重现】
             * 拖拽释放，当View还处在STATE_SETTLING状态时，快速点击View，造成state变换如上面描述过程，并停在该位置
             */
            if (mLastDragState == ViewDragHelper.STATE_SETTLING && state == ViewDragHelper.STATE_DRAGGING) {
                if (mViewDragHelper.smoothSlideViewTo(mContentView, mOriginalX, mOriginalY)) {
                    ViewCompat.postInvalidateOnAnimation(SwipeBackLayout.this);
                }
            }

            if ((mLastDragState == ViewDragHelper.STATE_DRAGGING || mLastDragState == ViewDragHelper.STATE_SETTLING)
                    && state == ViewDragHelper.STATE_IDLE) {
                if (null != mOnSwipeBackCallback && mDragOffset == getDragRange())
                    mOnSwipeBackCallback.onAnimationEnd();
            }
            if (state == ViewDragHelper.STATE_IDLE) mCurDragDirection = 0;
            mLastDragState = state;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            boolean isBack;

            int finalLeft, finalTop;
            switch (mCurDragDirection) {
                case LEFT:
                    isBack = mDragOffset >= mHorizontalDragRange * mFinishFactor;
                    finalLeft = isBack ? mHorizontalDragRange : mOriginalX;
                    if (mViewDragHelper.settleCapturedViewAt(finalLeft, mOriginalY)) {
                        ViewCompat.postInvalidateOnAnimation(SwipeBackLayout.this);
                    }
                    break;
                case RIGHT:
                    isBack = mDragOffset >= mHorizontalDragRange * mFinishFactor;
                    finalLeft = isBack ? -mHorizontalDragRange : mOriginalX;
                    if (mViewDragHelper.settleCapturedViewAt(finalLeft, mOriginalY)) {
                        ViewCompat.postInvalidateOnAnimation(SwipeBackLayout.this);
                    }
                    break;
                case UP:
                    isBack = mDragOffset >= mVerticalDragRange * mFinishFactor;
                    finalTop = isBack ? mVerticalDragRange : mOriginalY;
                    if (mViewDragHelper.settleCapturedViewAt(mOriginalX, finalTop)) {
                        ViewCompat.postInvalidateOnAnimation(SwipeBackLayout.this);
                    }
                    break;
                case DOWN:
                    isBack = mDragOffset >= mVerticalDragRange * mFinishFactor;
                    finalTop = isBack ? -mVerticalDragRange : mOriginalY;
                    if (mViewDragHelper.settleCapturedViewAt(mOriginalX, finalTop)) {
                        ViewCompat.postInvalidateOnAnimation(SwipeBackLayout.this);
                    }
                    break;
            }

        }


    }


    float downX ;
    float downY;

    /**
     * ④重写ViewGroup的onInterceptTouchEvent(MotionEvent ev)用来拦截事件
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean handled = false;
        mTouchX = ev.getRawX();
        mTouchY = ev.getRawY();
        if (isEnabled()) {
            if (mCurDragDirection == 0){
                switch (ev.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        downX = mTouchX;
                        downY = mTouchY;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float slope = (mTouchY - downY)/(mTouchX - downX);
                        mCurDragDirection = Math.abs(slope) >= 1 ? (mTouchY > downY? UP :DOWN ): (mTouchX > downX ? LEFT :RIGHT );
                        break;
                }
            }

            if ( mContentView instanceof ViewGroup){
                findScrollView((ViewGroup) mContentView);
            }else mScrollChild = mContentView;
            handled = null != mContentView && mViewDragHelper.shouldInterceptTouchEvent(ev);
        } else {
            mViewDragHelper.cancel();
        }
        if (!handled) {
            mCurDragDirection = 0;
            mScrollChild = mContentView;
        }
        return handled || super.onInterceptTouchEvent(ev);
    }

    /**
     * ⑤重写ViewGroup的onTouchEvent(MotionEvent event)
     * 在这里面只要做两件事：mDragHelper.processTouchEvent(event);处理拦截到的事件，这个方法会在返回前分发事件；
     * return true 表示消费了事件。
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    public boolean childCanScrollUp() {
        return ViewCompat.canScrollVertically(mScrollChild, 1);
    }

    public boolean childCanScrollDown() {
        return ViewCompat.canScrollVertically(mScrollChild, -1);
    }

    public boolean childCanScrollLeft() {
        return ViewCompat.canScrollHorizontally(mScrollChild, 1);
    }

    public boolean childCanScrollRight() {
        return ViewCompat.canScrollHorizontally(mScrollChild, -1);
    }

    private OnSwipeBackListener mOnSwipeBackCallback;

    public void setOnSwipeBackListener(OnSwipeBackListener callback) {
        mOnSwipeBackCallback = callback;
    }

    public interface OnSwipeBackListener {
        /**
         * 拦截事件
         *
         * @param direction
         * @return boolean 是否拦截该方向
         */
        boolean onIntercept(@DragDirection int direction,float touchX, float touchY);

        /**
         * @param fraction relative to the anchor.
         * @return
         */
        void onViewPositionChanged(float fraction);

        /**
         * View完全退出，不可见
         */
        void onAnimationEnd();
    }
}
