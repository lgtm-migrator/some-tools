package pers.zhc.tools.floatingdrawing;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.utils.GestureResolver;
import pers.zhc.tools.views.HSVAColorPickerRL;

public class FloatingViewOnTouchListener implements View.OnTouchListener {
    final private WindowManager.LayoutParams layoutParams;
    private int width;
    private int height;
    private final WindowManager wm;
    private final View view;
    private final ViewDimension viewDimension;
    private int lastRawX, lastRawY, paramX, paramY;

    public FloatingViewOnTouchListener(WindowManager.LayoutParams WM_layoutParams, WindowManager windowManager
            , View view, int width, int height, ViewDimension viewDimension) {
        this.layoutParams = WM_layoutParams;
        this.wm = windowManager;
        this.width = width;
        this.height = height;
        this.view = view;
        this.viewDimension = viewDimension;
    }

    private boolean performClick = true;

    public boolean onTouch(View v, @NotNull MotionEvent event, boolean doClick /* workaround */) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick = true;
                lastRawX = (int) rawX;
                lastRawY = (int) rawY;
                paramX = layoutParams.x;
                paramY = layoutParams.y;
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) rawX - lastRawX;
                int dy = (int) rawY - lastRawY;
                int lp2_x = paramX + dx;
                int lp2_y = paramY + dy;
                int constrainX, constrainY;
                constrainX = lp2_x - HSVAColorPickerRL.limitValue(lp2_x, ((int) (-width / 2F + viewDimension.width / 2F)), ((int) (width / 2F - viewDimension.width / 2F)));
                constrainY = lp2_y - HSVAColorPickerRL.limitValue(lp2_y, ((int) (-height / 2F + viewDimension.height / 2F)), ((int) (height / 2F - viewDimension.height / 2F)));
                if (constrainX == 0) {
                    layoutParams.x = lp2_x;
                } else {
                    paramX -= constrainX;
                }
                if (constrainY == 0) {
                    layoutParams.y = lp2_y;
                } else {
                    paramY -= constrainY;
                }
                // 更新悬浮窗位置
                wm.updateViewLayout(view, layoutParams);
                break;
            case MotionEvent.ACTION_UP:
                if (performClick && Math.abs(lastRawX - rawX) < 1 && Math.abs(lastRawY - rawY) < 1) {
                    if (doClick) {
                        v.performClick();
                    }
                }
                break;
        }
        return true;
    }

    public boolean onTouch(View v, @NotNull MotionEvent event) {
        return onTouch(v, event, true);
    }

    /**
     * <p>Cancel performing click this time.</p>
     * <p>Enable performing click again automatically when {@link MotionEvent#ACTION_DOWN} received next time.</p>
     */
    public void cancelPerformClick() {
        performClick = false;
    }

    public void updateParentDimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static class ViewDimension {
        public int width = 0, height = 0;

        public ViewDimension(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public ViewDimension() {
        }
    }
}
