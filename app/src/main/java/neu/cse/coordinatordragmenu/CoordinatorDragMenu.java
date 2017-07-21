package neu.cse.coordinatordragmenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by SteveWang on 2017/1/16.
 * 仿QQ6.x的协同侧滑菜单
 */

public class CoordinatorDragMenu extends FrameLayout {

    private ViewDragHelper mViewDragHelper;
    private View mMenuView, mMainView;
    private int mMenuWidth;
    private int mScreenHeight;
    private int mScreenWidth;

    private static final int MENU_OFFSET = 128;
    private int mMenuOffset;    // MenuView初始时的左偏移距离

    private static final String DEFAULT_SHADOW_OPACITY = "00";
    private String mShadowOpacity = DEFAULT_SHADOW_OPACITY; // 阴影透明度，随MainView位置的变化而变化

    private static final int MENU_CLOSED = 1;
    private static final int MENU_OPENED = 2;
    private int mMenuState = MENU_CLOSED;

    private int mDragOrientation;
    private static final int LEFT_TO_RIGHT = 3;
    private static final int RIGHT_TO_LEFT = 4;

    private static final float SPRING_BACK_VELOCITY = 1500;


    public CoordinatorDragMenu(Context context) {
        super(context);
        init();
    }

    public CoordinatorDragMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CoordinatorDragMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        float density = displayMetrics.density;   // 屏幕密度
        mMenuOffset = (int) (MENU_OFFSET * density + 0.5f);
        mViewDragHelper = ViewDragHelper.create(this, callback);
    }


    /**
     * 只有在从布局文件中加载View时会回调，直接new一个的话是不会回调的
     * 可以在这里拿到其子View，但还无法获取子View的宽高
     */
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMenuView = getChildAt(0);  // 第一个子View
        mMainView = getChildAt(1);  // 第二个子View

        // 如果子View不消耗触摸事件，那么触摸事件（DOWN-MOVE-UP）都是直接进入onTouchEvent()，在onTouchEvent()的DOWN的时候就确定了captureView
        // 这里为MainView设置了OnClickListener，会消耗触摸事件，导致先走onInterceptTouchEvent()，判断是否可以捕获
        // 而在判断的过程中会去判断另外两个回调的方法：getViewHorizontalDragRange和getViewVerticalDragRange，只有这两个方法返回大于0的值才能正常的捕获
        mMainView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnMainViewClickListener != null) {
                    mOnMainViewClickListener.onMainViewClick();
                }
            }
        });
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mMenuWidth = mMenuView.getMeasuredWidth();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mMenuState == MENU_OPENED) {
            mMenuView.layout(0, 0, mMenuWidth, bottom);
            mMainView.layout(mMenuWidth, 0, mMenuWidth + mScreenWidth, bottom);
        } else {
            mMenuView.layout(-mMenuOffset, top, mMenuWidth - mMenuOffset, bottom);  // 初始时MenuView向左偏移一个mMenuOffset距离
        }
    }


    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final int restoreCount = canvas.save(); // 保存裁剪前的画布
        if (child == mMenuView) {
            canvas.clipRect(0, 0, mMainView.getLeft(), getHeight());// 裁剪掉MenuView被MainView遮盖的部分，避免过渡绘制
        }
        boolean result = super.drawChild(canvas, child, drawingTime);// 完成原有的子View：MenuView和MainView的绘制
        canvas.restoreToCount(restoreCount);    // 恢复到剪裁前的画布，以正常绘制之后的View

        Paint shadowPaint = new Paint();        // 阴影画笔
        shadowPaint.setColor(Color.parseColor("#" + mShadowOpacity + "777777"));// 给画笔设置透明度变化的颜色
        shadowPaint.setStyle(Paint.Style.FILL); // 设置画笔类型填充
        canvas.drawRect(mMainView.getLeft(), 0, mScreenWidth, mScreenHeight, shadowPaint);// 画出阴影
        return result;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev); // ViewDragHelper决定是否拦截触摸事件
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);   // 触摸事件交给ViewDragHelper处理
        return true;
    }


    /**
     * 这个Callback是整个ViewDragHelper的逻辑核心
     */
    private ViewDragHelper.Callback callback = new ViewDragHelper.Callback() {

        /**
         * 告诉ViewDragHelper，parentView中哪个child可以接受触摸事件
         *
         * @param child
         * @param pointerId
         * @return
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mMainView || child == mMenuView;  // MainView和MenuView均可以接受触摸事件
        }

        /**
         * 用户触摸到可以接受触摸事件的child后回调
         *
         * @param capturedChild
         * @param activePointerId
         */
        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            // 如果当前触摸的View是MenuView，也交由MainView处理，这样手指在MenuView上滑动时，移动的也是MainView
            if (capturedChild == mMenuView) {
                mViewDragHelper.captureChildView(mMainView, activePointerId);
            }
        }

        /**
         * 如果child消耗触摸事件，只有该方法返回大于0的值ViewDragHelper才能正常捕获触摸事件
         *
         * @param child
         * @return
         */
        @Override
        public int getViewHorizontalDragRange(View child) {
            return mScreenWidth;
        }

        /**
         * 在该方法中对child的水平滑动边界进行限制
         * 若不重写，默认返回值为0，表示不可滑动
         *
         * @param child
         * @param left
         * @param dx
         * @return
         */
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            // 因为最终处理触摸事件的View是MainView，left指代MainView的左边缘的位置
            if (left < 0) {     // MainView不能左滑至屏幕外
                left = 0;
            } else if (left > mMenuWidth) { // MainView不能右滑至超出MenuView的宽度
                left = mMenuWidth;
            }
            return left;
        }

        /**
         * 当MainView位置改变时调用
         *
         * @param changedView
         * @param left
         * @param top
         * @param dx
         * @param dy
         */
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            // dx代表距离上一个滑动时间间隔后的滑动距离
            if (dx > 0) {
                mDragOrientation = LEFT_TO_RIGHT;
            } else if (dx < 0) {
                mDragOrientation = RIGHT_TO_LEFT;
            }
            // 实现MenuView跟随MainView移动
            // MenuView左边缘和MainView左边缘之间的距离由mMenuOffset增加到mMenuWidth
            // 此时MainView移动了mMenuWidth，可以认为这种增加是线性的
            // left - menuLeft = (mMenuWidth - mMenuOffset) / mMenuWidth * left + mMenuOffset
            float scale = (float) (mMenuWidth - mMenuOffset) / (float) mMenuWidth;
            int menuLeft = left - ((int) (scale * left) + mMenuOffset);
            mMenuView.layout(menuLeft, mMenuView.getTop(), menuLeft + mMenuWidth, mMenuView.getBottom());   // 移动MenuView的位置

            // 阴影透明度mShadowOpacity随MainView位置的变化而变化
            float showing = (float) (mScreenWidth - left) / (float) mScreenWidth;
            int hex = 255 - Math.round(showing * 255);
            if (hex < 16) {
                mShadowOpacity = "0" + Integer.toHexString(hex);
            } else {
                mShadowOpacity = Integer.toHexString(hex);
            }
        }

        /**
         * 拖动结束、手指离开屏幕后调用
         *
         * @param releasedChild
         * @param xvel
         * @param yvel
         */
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            // 增加回弹效果，xvel为水平滑动速度
            if (mDragOrientation == LEFT_TO_RIGHT) {    // 从左到右
                if (xvel > SPRING_BACK_VELOCITY || mMainView.getLeft() > 0.5 * mScreenWidth) { // 快滑或者大于屏幕的一半
                    openMenu();
                } else {
                    closeMenu();
                }
            } else if (mDragOrientation == RIGHT_TO_LEFT) { // 从右到左
                if (xvel < -SPRING_BACK_VELOCITY || mMainView.getLeft() < 0.5 * mScreenWidth) { // 快滑或者小于屏幕的一半
                    closeMenu();
                } else {
                    openMenu();
                }
            }
        }
    };

    /**
     * 滑动过程中调用
     * 使用ViewDragHelper同样需要重写computeScroll()
     * 因为ViewDragHelper内部也是通过Scroller来实现平滑移动的
     */
    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this); // 处理刷新，实现平滑移动
        }
        // 更新菜单的状态
        if (mMainView.getLeft() == 0) {
            mMenuState = MENU_CLOSED;
        } else if (mMainView.getLeft() == mMenuWidth) {
            mMenuState = MENU_OPENED;
        }
    }


    /**
     * 打开菜单
     */
    public void openMenu() {
        mViewDragHelper.smoothSlideViewTo(mMainView, mMenuWidth, 0);
        ViewCompat.postInvalidateOnAnimation(CoordinatorDragMenu.this);
    }

    /**
     * 关闭菜单
     */
    public void closeMenu() {
        mViewDragHelper.smoothSlideViewTo(mMainView, 0, 0);
        ViewCompat.postInvalidateOnAnimation(CoordinatorDragMenu.this);
    }

    /**
     * 判断菜单是否打开
     *
     * @return
     */
    public boolean isOpenedMenu() {
        return mMenuState == MENU_OPENED;
    }


    public interface OnMainViewClickListener {
        void onMainViewClick();
    }

    private OnMainViewClickListener mOnMainViewClickListener;

    public void setOnMainViewClickListener(OnMainViewClickListener listener) {
        mOnMainViewClickListener = listener;
    }

}
