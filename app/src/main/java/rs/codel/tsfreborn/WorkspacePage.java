package rs.codel.tsfreborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public final class WorkspacePage extends FrameLayout implements IconTile.Listener {
    public interface Listener {
        void onTileActivate(IconTile tile);
        void onTileDragStart(IconTile tile);
        void onTileDragMove(IconTile tile, float rawY);
        void onTileDragEnd(IconTile tile, float rawY);
        void onExternalDrop(WorkspacePage page, LauncherItem item, float x, float y);
        void onEditModeChanged(WorkspacePage page, boolean enabled);
        void onSelectionChanged(WorkspacePage page, int selectedCount);
    }

    private final Listener listener;
    private final int pageIndex;
    private final GestureDetector gestureDetector;
    private final Paint lassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lassoFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path lassoPath = new Path();
    private final ArrayList<PointF> lassoPoints = new ArrayList<>();

    private boolean editMode;
    private boolean lassoActive;
    private float downX;
    private float downY;

    public WorkspacePage(Context context, int pageIndex, Listener listener) {
        super(context);
        this.pageIndex = pageIndex;
        this.listener = listener;
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setWillNotDraw(false);

        lassoPaint.setColor(0xFFD2E8FF);
        lassoPaint.setStyle(Paint.Style.STROKE);
        lassoPaint.setStrokeWidth(dp(2));
        lassoPaint.setStrokeCap(Paint.Cap.ROUND);
        lassoPaint.setStrokeJoin(Paint.Join.ROUND);

        lassoFillPaint.setColor(0x223A9BFF);
        lassoFillPaint.setStyle(Paint.Style.FILL);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public void onLongPress(MotionEvent e) {
                if (!editMode) {
                    setEditMode(true);
                    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                }
            }
        });

        setOnDragListener((v, event) -> handleExternalDrag(event));

        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                applyAllPositions();
            }
        });
    }

    public int getPageIndex() { return pageIndex; }
    public boolean isEditMode() { return editMode; }

    public IconTile addShortcut(LauncherItem item, ShortcutRecord record) {
        record.page = pageIndex;
        IconTile tile = new IconTile(getContext(), item, record, this);
        addView(tile);
        tile.post(tile::applyNormalizedPosition);
        return tile;
    }

    public IconTile addShortcutAt(LauncherItem item, ShortcutRecord record, float x, float y) {
        IconTile tile = addShortcut(item, record);
        post(() -> {
            float maxX = Math.max(0, getWidth() - tile.getWidth());
            float maxY = Math.max(0, getHeight() - tile.getHeight() - dp(74));
            tile.setX(clamp(x - tile.getWidth() / 2f, 0, maxX));
            tile.setY(clamp(y - tile.getHeight() / 2f, 0, maxY));
            tile.updateNormalizedPosition();
        });
        return tile;
    }

    public void removeShortcut(IconTile tile) {
        removeView(tile);
    }

    public List<IconTile> tiles() {
        ArrayList<IconTile> out = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof IconTile) out.add((IconTile) v);
        }
        return out;
    }

    public List<IconTile> selectedTiles() {
        ArrayList<IconTile> out = new ArrayList<>();
        for (IconTile tile : tiles()) if (tile.isSelectedVisual()) out.add(tile);
        return out;
    }

    public void selectAll() {
        if (!editMode) setEditMode(true);
        for (IconTile tile : tiles()) tile.setSelectedVisual(true);
        notifySelection();
    }

    public void clearSelection() {
        for (IconTile tile : tiles()) tile.setSelectedVisual(false);
        notifySelection();
    }

    public void setEditMode(boolean enabled) {
        if (editMode == enabled) return;
        editMode = enabled;
        lassoActive = false;
        lassoPath.reset();
        lassoPoints.clear();
        if (!enabled) {
            for (IconTile tile : tiles()) tile.setSelectedVisual(false);
        }
        invalidate();
        listener.onEditModeChanged(this, enabled);
        notifySelection();
    }

    public void applyAllPositions() {
        post(() -> {
            for (IconTile tile : tiles()) tile.applyNormalizedPosition();
        });
    }

    private boolean handleExternalDrag(DragEvent event) {
        Object state = event.getLocalState();
        if (!(state instanceof LauncherItem)) return false;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                setAlpha(0.94f);
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                setAlpha(1f);
                return true;
            case DragEvent.ACTION_DROP:
                setAlpha(1f);
                listener.onExternalDrop(this, (LauncherItem) state, event.getX(), event.getY());
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                setAlpha(1f);
                return true;
            default:
                return true;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        if (!editMode) return true;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                downX = event.getX();
                downY = event.getY();
                lassoActive = true;
                lassoPath.reset();
                lassoPoints.clear();
                lassoPath.moveTo(downX, downY);
                lassoPoints.add(new PointF(downX, downY));
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (lassoActive) {
                    float x = event.getX();
                    float y = event.getY();
                    PointF last = lassoPoints.get(lassoPoints.size() - 1);
                    if (Math.hypot(x - last.x, y - last.y) >= dp(4)) {
                        lassoPath.lineTo(x, y);
                        lassoPoints.add(new PointF(x, y));
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (lassoActive) {
                    finishLasso(event.getX(), event.getY());
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                lassoActive = false;
                lassoPath.reset();
                lassoPoints.clear();
                invalidate();
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return true;
    }

    private void finishLasso(float x, float y) {
        if (!lassoActive) return;
        lassoActive = false;
        if (lassoPoints.size() >= 3) {
            lassoPath.lineTo(x, y);
            lassoPath.close();
            RectF bounds = new RectF();
            lassoPath.computeBounds(bounds, true);
            Region clip = new Region(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight()));
            Region region = new Region();
            region.setPath(lassoPath, clip);
            for (IconTile tile : tiles()) {
                int cx = Math.round(tile.getX() + tile.getWidth() / 2f);
                int cy = Math.round(tile.getY() + tile.getHeight() / 2f);
                tile.setSelectedVisual(region.contains(cx, cy));
            }
        } else {
            clearSelection();
        }
        lassoPath.reset();
        lassoPoints.clear();
        invalidate();
        notifySelection();
    }

    private void toggleSelected(IconTile tile) {
        tile.setSelectedVisual(!tile.isSelectedVisual());
        notifySelection();
    }

    private void notifySelection() {
        listener.onSelectionChanged(this, selectedTiles().size());
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (editMode && lassoActive && lassoPoints.size() > 1) {
            canvas.drawPath(lassoPath, lassoFillPaint);
            canvas.drawPath(lassoPath, lassoPaint);
        }
    }

    @Override public void onActivate(IconTile tile) {
        if (editMode) toggleSelected(tile); else listener.onTileActivate(tile);
    }

    @Override public void onDragStart(IconTile tile) {
        if (editMode && !tile.isSelectedVisual()) {
            clearSelection();
            tile.setSelectedVisual(true);
            notifySelection();
        }
        listener.onTileDragStart(tile);
    }

    @Override public void onDragMove(IconTile tile, float rawY) { listener.onTileDragMove(tile, rawY); }
    @Override public void onDragEnd(IconTile tile, float rawY) { listener.onTileDragEnd(tile, rawY); }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
