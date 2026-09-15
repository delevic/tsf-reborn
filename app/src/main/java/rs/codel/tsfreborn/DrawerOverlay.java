package rs.codel.tsfreborn;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public final class DrawerOverlay extends FrameLayout {
    public interface Listener {
        void onLaunch(LauncherItem item);
        void onAddToDesktop(LauncherItem item);
        void onBeginDragFromDrawer(LauncherItem item);
        void onCloseDrawer();
    }

    private final Listener listener;
    private final GridLayout grid;
    private final TextView countText;

    public DrawerOverlay(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setBackgroundResource(R.drawable.bg_drawer_panel);
        setClickable(true);
        setFocusable(true);

        TextView title = new TextView(context);
        title.setText("Applications");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(20), 0, 0, 0);
        LayoutParams titleLp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(64));
        titleLp.gravity = Gravity.TOP;
        addView(title, titleLp);

        TextView close = new TextView(context);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(38);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> listener.onCloseDrawer());
        LayoutParams closeLp = new LayoutParams(dp(64), dp(64), Gravity.TOP | Gravity.END);
        addView(close, closeLp);

        TextView hint = new TextView(context);
        hint.setText("Tap to launch • Hold and drag to desktop");
        hint.setTextColor(0xFFB8BDC7);
        hint.setTextSize(12);
        hint.setPadding(dp(20), 0, dp(20), 0);
        LayoutParams hintLp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(32));
        hintLp.topMargin = dp(58);
        addView(hint, hintLp);

        countText = new TextView(context);
        countText.setTextColor(0xFF9FA5B0);
        countText.setTextSize(11);
        countText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        countText.setPadding(0, 0, dp(18), 0);
        LayoutParams countLp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(28));
        countLp.topMargin = dp(86);
        addView(countText, countLp);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LayoutParams sp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        sp.topMargin = dp(116);
        sp.bottomMargin = dp(12);
        addView(scroll, sp);

        grid = new GridLayout(context);
        grid.setColumnCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        grid.setPadding(dp(8), dp(8), dp(8), dp(28));
        scroll.addView(grid, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void setApps(List<LauncherItem> apps) {
        grid.removeAllViews();
        if (apps == null) apps = Collections.emptyList();
        countText.setText(apps.size() + " apps");
        for (LauncherItem item : apps) grid.addView(makeCell(item), cellParams());
    }

    private View makeCell(LauncherItem item) {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(dp(4), dp(8), dp(4), dp(8));
        cell.setBackgroundColor(Color.TRANSPARENT);
        cell.setClickable(true);
        cell.setFocusable(true);

        ImageView icon = new ImageView(getContext());
        icon.setImageDrawable(item.icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cell.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView label = new TextView(getContext());
        label.setText(item.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(28));
        lp.topMargin = dp(4);
        cell.addView(label, lp);

        cell.setOnClickListener(v -> listener.onLaunch(item));
        cell.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            ClipData data = ClipData.newPlainText("component", item.componentKey());
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            boolean started = v.startDragAndDrop(data, shadow, item, View.DRAG_FLAG_GLOBAL);
            if (started) {
                listener.onBeginDragFromDrawer(item);
            } else {
                listener.onAddToDesktop(item);
            }
            return true;
        });
        return cell;
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = dp(102);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
