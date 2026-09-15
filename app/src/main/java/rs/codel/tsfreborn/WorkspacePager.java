package rs.codel.tsfreborn;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

public final class WorkspacePager extends ViewGroup {
    public interface Listener { void onPageChanged(int page); }

    private final OverScroller scroller;
    private final int touchSlop;
    private final int minFlingVelocity;
    private VelocityTracker velocityTracker;
    private float downX, downY;
    private int initialScrollX;
    private boolean swiping;
    private int currentPage;
    private Listener listener;

    public WorkspacePager(Context context) {
        super(context);
        scroller = new OverScroller(context);
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        setWillNotDraw(false);
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public int getCurrentPage() { return currentPage; }

    public void setCurrentPage(int page, boolean animate) {
        int count = getChildCount();
        if (count == 0) return;
        page = Math.max(0, Math.min(count - 1, page));
        int target = page * getWidth();
        if (animate && getWidth() > 0) {
            scroller.startScroll(getScrollX(), 0, target - getScrollX(), 0, 220);
            invalidate();
        } else {
            scrollTo(target, 0);
        }
        if (currentPage != page) {
            currentPage = page;
            if (listener != null) listener.onPageChanged(page);
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(w, h);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        }
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(i * w, 0, (i + 1) * w, b - t);
        }
        if (changed) post(() -> scrollTo(currentPage * getWidth(), 0));
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                initialScrollX = getScrollX();
                swiping = false;
                if (!scroller.isFinished()) scroller.abortAnimation();
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    swiping = true;
                    beginVelocity(ev);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                swiping = false;
                recycleVelocity();
                return false;
        }
        return swiping;
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        if (velocityTracker == null) beginVelocity(ev); else velocityTracker.addMovement(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float target = initialScrollX + (downX - ev.getX());
                int max = Math.max(0, (getChildCount() - 1) * getWidth());
                scrollTo((int) Math.max(0, Math.min(max, target)), 0);
                return true;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                float vx = velocityTracker.getXVelocity();
                int page;
                if (Math.abs(vx) >= minFlingVelocity) {
                    page = currentPage + (vx < 0 ? 1 : -1);
                } else {
                    page = Math.round(getScrollX() / (float) Math.max(1, getWidth()));
                }
                setCurrentPage(page, true);
                recycleVelocity();
                swiping = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                setCurrentPage(currentPage, true);
                recycleVelocity();
                swiping = false;
                return true;
        }
        return true;
    }

    private void beginVelocity(MotionEvent ev) {
        recycleVelocity();
        velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(ev);
    }

    private void recycleVelocity() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    @Override public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidateOnAnimation();
        }
    }
}
