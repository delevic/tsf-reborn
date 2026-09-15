package rs.codel.tsfreborn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements
        WorkspacePage.Listener, WorkspacePager.Listener, DrawerOverlay.Listener {

    private static final int PAGE_COUNT = 3;
    private static final int START_PAGE = 1;
    private static final int REQ_BACKUP = 501;
    private static final int REQ_RESTORE = 502;
    private static final int REQ_HOME_ROLE = 503;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<LauncherItem> apps = new ArrayList<>();
    private final Map<String, LauncherItem> appByComponent = new HashMap<>();
    private final List<ShortcutRecord> records = new ArrayList<>();

    private FrameLayout root;
    private WorkspacePager pager;
    private WorkspacePage[] pages;
    private DrawerOverlay drawer;
    private TextView drawerButton;
    private TextView pageIndicator;
    private TextView removeZone;
    private LinearLayout editBar;
    private TextView editCount;
    private LayoutStore store;
    private LauncherRepository repository;
    private boolean layoutRestored;
    private WorkspacePage editingPage;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        store = new LayoutStore(this);
        repository = new LauncherRepository(this);
        records.addAll(store.load());
        buildUi();
        loadApps();
    }

    private void configureWindow() {
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= 30) {
            // Configure edge-to-edge here, but do not ask the Window for its
            // InsetsController before the decor view has been attached.
            w.setDecorFitsSystemWindows(false);
        } else {
            w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void hideStatusBar() {
        if (Build.VERSION.SDK_INT >= 30) {
            View decor = getWindow().getDecorView();
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideStatusBar();
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);
        setContentView(root);
        root.post(this::hideStatusBar);

        pager = new WorkspacePager(this);
        pager.setListener(this);
        root.addView(pager, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        pages = new WorkspacePage[PAGE_COUNT];
        for (int i = 0; i < PAGE_COUNT; i++) {
            pages[i] = new WorkspacePage(this, i, this);
            pager.addView(pages[i]);
        }

        removeZone = new TextView(this);
        removeZone.setText("REMOVE");
        removeZone.setTextColor(Color.WHITE);
        removeZone.setTextSize(14);
        removeZone.setGravity(Gravity.CENTER);
        removeZone.setBackgroundResource(R.drawable.bg_remove_zone);
        removeZone.setVisibility(View.GONE);
        FrameLayout.LayoutParams rz = new FrameLayout.LayoutParams(dp(150), dp(50), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        rz.topMargin = dp(12);
        root.addView(removeZone, rz);

        buildEditBar();

        drawerButton = new TextView(this);
        drawerButton.setText("⠿");
        drawerButton.setTextColor(Color.WHITE);
        drawerButton.setTextSize(35);
        drawerButton.setGravity(Gravity.CENTER);
        drawerButton.setBackgroundResource(R.drawable.bg_drawer_button);
        drawerButton.setElevation(dp(8));
        drawerButton.setOnClickListener(v -> openDrawer());
        drawerButton.setOnLongClickListener(v -> {
            showLauncherMenu();
            return true;
        });
        FrameLayout.LayoutParams db = new FrameLayout.LayoutParams(dp(66), dp(66), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        db.bottomMargin = dp(22);
        root.addView(drawerButton, db);

        pageIndicator = new TextView(this);
        pageIndicator.setTextColor(0xCCFFFFFF);
        pageIndicator.setTextSize(18);
        pageIndicator.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams pi = new FrameLayout.LayoutParams(dp(140), dp(32), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        pi.bottomMargin = dp(94);
        root.addView(pageIndicator, pi);

        drawer = new DrawerOverlay(this, this);
        drawer.setVisibility(View.GONE);
        root.addView(drawer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int navBottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            }
            FrameLayout.LayoutParams p1 = (FrameLayout.LayoutParams) drawerButton.getLayoutParams();
            p1.bottomMargin = navBottom + dp(18);
            drawerButton.setLayoutParams(p1);
            FrameLayout.LayoutParams p2 = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
            p2.bottomMargin = navBottom + dp(90);
            pageIndicator.setLayoutParams(p2);
            return insets;
        });

        pager.post(() -> pager.setCurrentPage(START_PAGE, false));
        updatePageIndicator(START_PAGE);
    }

    private void buildEditBar() {
        editBar = new LinearLayout(this);
        editBar.setOrientation(LinearLayout.HORIZONTAL);
        editBar.setGravity(Gravity.CENTER_VERTICAL);
        editBar.setPadding(dp(10), dp(5), dp(10), dp(5));
        editBar.setBackgroundColor(0xDD151A22);
        editBar.setElevation(dp(12));
        editBar.setVisibility(View.GONE);

        editCount = editAction("0 selected", null);
        editCount.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        editBar.addView(editCount, countLp);

        TextView selectAll = editAction("Select all", v -> {
            if (editingPage != null) editingPage.selectAll();
        });
        editBar.addView(selectAll, new LinearLayout.LayoutParams(dp(92), dp(44)));

        TextView delete = editAction("Delete", v -> deleteSelected());
        editBar.addView(delete, new LinearLayout.LayoutParams(dp(72), dp(44)));

        TextView done = editAction("Done", v -> exitEditMode());
        editBar.addView(done, new LinearLayout.LayoutParams(dp(64), dp(44)));

        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(56), Gravity.TOP);
        ep.leftMargin = dp(8);
        ep.rightMargin = dp(8);
        ep.topMargin = dp(8);
        root.addView(editBar, ep);
    }

    private TextView editAction(String text, View.OnClickListener click) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(13);
        v.setGravity(Gravity.CENTER);
        if (click != null) v.setOnClickListener(click);
        return v;
    }

    private void loadApps() {
        executor.execute(() -> {
            List<LauncherItem> loaded = repository.loadLaunchableApps();
            runOnUiThread(() -> {
                apps.clear();
                apps.addAll(loaded);
                appByComponent.clear();
                for (LauncherItem item : apps) appByComponent.put(item.componentKey(), item);
                drawer.setApps(apps);
                if (!layoutRestored) restoreWorkspace();
            });
        });
    }

    private void restoreWorkspace() {
        layoutRestored = true;
        ArrayList<ShortcutRecord> invalid = new ArrayList<>();
        for (ShortcutRecord record : records) {
            LauncherItem item = appByComponent.get(record.component);
            if (item == null || record.page < 0 || record.page >= PAGE_COUNT) {
                invalid.add(record);
                continue;
            }
            pages[record.page].addShortcut(item, record);
        }
        if (!invalid.isEmpty()) {
            records.removeAll(invalid);
            saveLayout();
        }
        if (records.isEmpty()) {
            Toast.makeText(this, "Open the drawer and hold-drag an app onto the desktop.", Toast.LENGTH_LONG).show();
        }
    }

    private void openDrawer() {
        exitEditMode();
        drawer.setApps(apps);
        drawer.setVisibility(View.VISIBLE);
        drawer.bringToFront();
    }

    private void closeDrawer() {
        drawer.setVisibility(View.GONE);
    }

    @Override public void onBackPressed() {
        if (drawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
            return;
        }
        if (editingPage != null) {
            exitEditMode();
            return;
        }
        if (pager.getCurrentPage() != START_PAGE) {
            pager.setCurrentPage(START_PAGE, true);
        }
    }

    private void launch(LauncherItem item) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(item.component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot launch " + item.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onTileActivate(IconTile tile) {
        launch(tile.item);
    }

    @Override public void onTileDragStart(IconTile tile) {
        removeZone.setVisibility(View.VISIBLE);
        removeZone.bringToFront();
    }

    @Override public void onTileDragMove(IconTile tile, float rawY) {
        boolean remove = rawY < dp(115);
        removeZone.animate().alpha(remove ? 1f : 0.65f).setDuration(60).start();
    }

    @Override public void onTileDragEnd(IconTile tile, float rawY) {
        removeZone.setVisibility(View.GONE);
        removeZone.setAlpha(1f);
        if (rawY < dp(125)) {
            if (tile.getParent() instanceof WorkspacePage) {
                ((WorkspacePage) tile.getParent()).removeShortcut(tile);
            }
            records.remove(tile.record);
        } else {
            tile.updateNormalizedPosition();
        }
        saveLayout();
        if (editingPage != null) onSelectionChanged(editingPage, editingPage.selectedTiles().size());
    }

    @Override public void onExternalDrop(WorkspacePage page, LauncherItem item, float x, float y) {
        if (page.getPageIndex() != pager.getCurrentPage()) return;
        ShortcutRecord record = ShortcutRecord.create(item.componentKey(), page.getPageIndex(), 0.5f, 0.45f);
        records.add(record);
        IconTile tile = page.addShortcutAt(item, record, x, y);
        tile.post(() -> {
            tile.updateNormalizedPosition();
            saveLayout();
        });
        Toast.makeText(this, item.label + " added", Toast.LENGTH_SHORT).show();
    }

    @Override public void onEditModeChanged(WorkspacePage page, boolean enabled) {
        if (enabled) {
            if (editingPage != null && editingPage != page) editingPage.setEditMode(false);
            editingPage = page;
            editBar.setVisibility(View.VISIBLE);
            editBar.bringToFront();
            drawerButton.setVisibility(View.INVISIBLE);
            pageIndicator.setVisibility(View.INVISIBLE);
            updateEditCount(0);
        } else if (editingPage == page) {
            editingPage = null;
            editBar.setVisibility(View.GONE);
            drawerButton.setVisibility(View.VISIBLE);
            pageIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override public void onSelectionChanged(WorkspacePage page, int selectedCount) {
        if (editingPage == page) updateEditCount(selectedCount);
    }

    private void updateEditCount(int count) {
        editCount.setText(count + (count == 1 ? " selected" : " selected"));
    }

    private void deleteSelected() {
        if (editingPage == null) return;
        List<IconTile> selected = new ArrayList<>(editingPage.selectedTiles());
        if (selected.isEmpty()) return;
        for (IconTile tile : selected) {
            records.remove(tile.record);
            editingPage.removeShortcut(tile);
        }
        saveLayout();
        onSelectionChanged(editingPage, 0);
    }

    private void exitEditMode() {
        if (editingPage != null) editingPage.setEditMode(false);
    }

    @Override public void onPageChanged(int page) {
        updatePageIndicator(page);
        if (editingPage != null && editingPage.getPageIndex() != page) exitEditMode();
    }

    private void updatePageIndicator(int page) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < PAGE_COUNT; i++) {
            if (i > 0) s.append("  ");
            s.append(i == page ? "●" : "•");
        }
        pageIndicator.setText(s.toString());
    }

    @Override public void onLaunch(LauncherItem item) {
        closeDrawer();
        launch(item);
    }

    @Override public void onAddToDesktop(LauncherItem item) {
        int page = pager.getCurrentPage();
        ShortcutRecord record = ShortcutRecord.create(item.componentKey(), page, 0.5f, 0.43f);
        records.add(record);
        pages[page].addShortcut(item, record);
        saveLayout();
        closeDrawer();
        Toast.makeText(this, item.label + " added", Toast.LENGTH_SHORT).show();
    }

    @Override public void onBeginDragFromDrawer(LauncherItem item) {
        drawer.postDelayed(this::closeDrawer, 40);
    }

    @Override public void onCloseDrawer() {
        closeDrawer();
    }

    private void showLauncherMenu() {
        String[] items = {"Set as default Home", "Backup layout", "Restore layout", "About"};
        new AlertDialog.Builder(this)
                .setTitle("TSF Reborn")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) requestHomeRole();
                    else if (which == 1) createBackup();
                    else if (which == 2) restoreBackup();
                    else Toast.makeText(this, "TSF Reborn v0.2 • drag + lasso", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void requestHomeRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = (RoleManager) getSystemService(ROLE_SERVICE);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_HOME_ROLE);
                return;
            }
        }
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Open Settings and choose TSF Reborn as Home app.", Toast.LENGTH_LONG).show();
        }
    }

    private void createBackup() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "tsf-reborn-layout.json");
        startActivityForResult(i, REQ_BACKUP);
    }

    private void restoreBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, REQ_RESTORE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try {
            if (requestCode == REQ_BACKUP) {
                try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new IllegalStateException("Cannot open destination");
                    out.write(store.serialize(records).getBytes(StandardCharsets.UTF_8));
                }
                Toast.makeText(this, "Layout backup saved", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQ_RESTORE) {
                StringBuilder text = new StringBuilder();
                try (InputStream in = getContentResolver().openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) text.append(line).append('\n');
                }
                List<ShortcutRecord> imported = store.importJson(text.toString());
                records.clear();
                records.addAll(imported);
                rebuildWorkspaceFromRecords();
                Toast.makeText(this, "Layout restored", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Backup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void rebuildWorkspaceFromRecords() {
        exitEditMode();
        for (WorkspacePage page : pages) page.removeAllViews();
        layoutRestored = true;
        ArrayList<ShortcutRecord> invalid = new ArrayList<>();
        for (ShortcutRecord record : records) {
            LauncherItem item = appByComponent.get(record.component);
            if (item == null || record.page < 0 || record.page >= PAGE_COUNT) {
                invalid.add(record);
                continue;
            }
            pages[record.page].addShortcut(item, record);
        }
        if (!invalid.isEmpty()) {
            records.removeAll(invalid);
            saveLayout();
        }
    }

    private void saveLayout() {
        store.save(records);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
