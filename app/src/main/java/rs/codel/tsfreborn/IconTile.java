package rs.codel.tsfreborn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public final class IconTile extends FrameLayout {
    public interface Listener {
        void onActivate(IconTile tile);
        void onDragStart(IconTile tile);
        void onDragMove(IconTile tile, float rawY);
        void onDragEnd(IconTile tile, float rawY);
    }

    public final LauncherItem item;
    public final ShortcutRecord record;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int touchSlop;
    private final Listener listener;
    private boolean dragging;
    private boolean moved;
    private boolean selectedVisual;
    private float downRawX, downRawY, startX, startY;

    private final Runnable longPress = new Runnable() {
        @Override public void run() {
            dragging = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            bringToFront();
            animate().scaleX(1.08f).scaleY(1.08f).setDuration(90).start();
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            listener.onDragStart(IconTile.this);
        }
    };

    public IconTile(Context context, LauncherItem item, ShortcutRecord record, Listener listener) {
        super(context);
        this.item = item;
        this.record = record;
        this.listener = listener;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
        setFocusable(true);
        buildContent();
    }

    private void buildContent() {
        int w = dp(88);
        int h = dp(96);
        setLayoutParams(new FrameLayout.LayoutParams(w, h));

        ImageView image = new ImageView(getContext());
        image.setImageDrawable(item.icon);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(58), dp(58));
        ip.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        ip.topMargin = dp(3);
        addView(image, ip);

        TextView text = new TextView(getContext());
        text.setText(item.label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(12);
        text.setGravity(android.view.Gravity.CENTER);
        text.setSingleLine(true);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.setShadowLayer(4f, 0f, 1f, 0xFF000000);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(30));
        tp.gravity = android.view.Gravity.BOTTOM;
        tp.leftMargin = dp(2);
        tp.rightMargin = dp(2);
        addView(text, tp);
    }

    public void setSelectedVisual(boolean selected) {
        if (selectedVisual == selected) return;
        selectedVisual = selected;
        if (selected) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x332A8CFF);
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(2), 0xFF8AC1FF);
            setBackground(bg);
            animate().scaleX(1.04f).scaleY(1.04f).setDuration(90).start();
        } else {
            setBackground(null);
            if (!dragging) animate().scaleX(1f).scaleY(1f).setDuration(90).start();
        }
    }

    public boolean isSelectedVisual() { return selectedVisual; }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = false;
                moved = false;
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startX = getX();
                startY = getY();
                handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!moved && Math.hypot(dx, dy) > touchSlop) {
                    moved = true;
                    if (!dragging) handler.removeCallbacks(longPress);
                }
                if (dragging) {
                    View parent = (View) getParent();
                    float maxX = Math.max(0, parent.getWidth() - getWidth());
                    float maxY = Math.max(0, parent.getHeight() - getHeight() - dp(74));
                    setX(clamp(startX + dx, 0, maxX));
                    setY(clamp(startY + dy, 0, maxY));
                    listener.onDragMove(this, event.getRawY());
                }
                return true;

            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPress);
                if (dragging) {
                    dragging = false;
                    animate().scaleX(selectedVisual ? 1.04f : 1f).scaleY(selectedVisual ? 1.04f : 1f).setDuration(90).start();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    listener.onDragEnd(this, event.getRawY());
                } else if (!moved) {
                    listener.onActivate(this);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPress);
                if (dragging) {
                    dragging = false;
                    animate().scaleX(selectedVisual ? 1.04f : 1f).scaleY(selectedVisual ? 1.04f : 1f).setDuration(90).start();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    listener.onDragEnd(this, event.getRawY());
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void updateNormalizedPosition() {
        if (!(getParent() instanceof View)) return;
        View p = (View) getParent();
        float maxX = Math.max(1, p.getWidth() - getWidth());
        float maxY = Math.max(1, p.getHeight() - getHeight() - dp(74));
        record.x = clamp(getX() / maxX, 0f, 1f);
        record.y = clamp(getY() / maxY, 0f, 1f);
    }

    public void applyNormalizedPosition() {
        if (!(getParent() instanceof View)) return;
        View p = (View) getParent();
        float maxX = Math.max(0, p.getWidth() - getWidth());
        float maxY = Math.max(0, p.getHeight() - getHeight() - dp(74));
        setX(record.x * maxX);
        setY(record.y * maxY);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
