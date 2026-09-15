from pathlib import Path
import re

# TSF-style status bar: visible + transparent, launcher/background behind it.
# The drawer panel itself covers the status-bar area; only its content is inset.

main = Path('app/src/main/java/rs/codel/tsfreborn/MainActivity.java')
text = main.read_text(encoding='utf-8')

window_block = '''    private void configureWindow() {
        Window w = getWindow();

        // TSF-style system bars: status bar stays visible and transparent while
        // the launcher background is laid out underneath it.
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.BLACK);

        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(attrs);
        }

        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void showStatusBar() {
        if (Build.VERSION.SDK_INT >= 30) {
            View decor = getWindow().getDecorView();
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars());
                controller.setSystemBarsAppearance(
                        0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) showStatusBar();
    }

'''

pattern = re.compile(
    r'    private void configureWindow\(\) \{.*?'
    r'    @Override public void onWindowFocusChanged\(boolean hasFocus\) \{.*?\n    \}\n\n'
    r'(?=    private void buildUi\(\))',
    re.S
)
text, count = pattern.subn(window_block, text, count=1)
if count != 1:
    raise SystemExit('Could not replace window/status-bar block')

text = text.replace('root.post(this::hideStatusBar);', 'root.post(this::showStatusBar);')

insets_block = '''        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusTop = 0;
            int navBottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                statusTop = insets.getInsets(WindowInsets.Type.statusBars()).top;
                navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            }

            FrameLayout.LayoutParams p1 = (FrameLayout.LayoutParams) drawerButton.getLayoutParams();
            p1.bottomMargin = navBottom + dp(18);
            drawerButton.setLayoutParams(p1);

            FrameLayout.LayoutParams p2 = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
            p2.bottomMargin = navBottom + dp(90);
            pageIndicator.setLayoutParams(p2);

            FrameLayout.LayoutParams p3 = (FrameLayout.LayoutParams) removeZone.getLayoutParams();
            p3.topMargin = statusTop + dp(12);
            removeZone.setLayoutParams(p3);

            FrameLayout.LayoutParams p4 = (FrameLayout.LayoutParams) editBar.getLayoutParams();
            p4.topMargin = statusTop + dp(8);
            editBar.setLayoutParams(p4);

            return insets;
        });
'''

insets_pattern = re.compile(
    r'        root\.setOnApplyWindowInsetsListener\(\(v, insets\) -> \{.*?\n        \}\);\n\n(?=        pager\.post)',
    re.S
)
text, count = insets_pattern.subn(insets_block + '\n', text, count=1)
if count != 1:
    raise SystemExit('Could not replace root insets block')

main.write_text(text, encoding='utf-8')

# Drawer: full-screen panel background, content inset below status bar and above nav bar.
drawer = Path('app/src/main/java/rs/codel/tsfreborn/DrawerOverlay.java')
dtext = drawer.read_text(encoding='utf-8')
if 'import android.os.Build;' not in dtext:
    dtext = dtext.replace('import android.graphics.Color;\n', 'import android.graphics.Color;\nimport android.os.Build;\n')
if 'import android.view.WindowInsets;' not in dtext:
    dtext = dtext.replace('import android.view.View;\n', 'import android.view.View;\nimport android.view.WindowInsets;\n')

marker = '        // TSF-style drawer insets\n'
if marker not in dtext:
    needle = '''        setBackgroundResource(R.drawable.bg_drawer_panel);\n        setClickable(true);\n        setFocusable(true);\n'''
    replacement = needle + '''\n        // TSF-style drawer insets\n        // Background fills the entire screen, including behind the status bar.\n        // Only drawer content is padded away from system-bar icons.\n        setOnApplyWindowInsetsListener((v, insets) -> {\n            int top = 0;\n            int bottom = 0;\n            if (Build.VERSION.SDK_INT >= 30) {\n                top = insets.getInsets(WindowInsets.Type.statusBars()).top;\n                bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;\n            }\n            setPadding(0, top, 0, bottom);\n            return insets;\n        });\n'''
    if needle not in dtext:
        raise SystemExit('Could not locate DrawerOverlay init block')
    dtext = dtext.replace(needle, replacement, 1)

drawer.write_text(dtext, encoding='utf-8')

styles = Path('app/src/main/res/values/styles.xml')
styles.write_text('''<resources>\n    <style name="Theme.TSFReborn" parent="android:style/Theme.Material.NoActionBar">\n        <item name="android:fontFamily">sans</item>\n        <item name="android:windowNoTitle">true</item>\n        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>\n        <item name="android:windowActionModeOverlay">true</item>\n        <item name="android:windowShowWallpaper">true</item>\n        <item name="android:windowBackground">@android:color/transparent</item>\n        <item name="android:colorAccent">#FFFFFFFF</item>\n        <item name="android:statusBarColor">@android:color/transparent</item>\n        <item name="android:navigationBarColor">#CC000000</item>\n        <item name="android:windowLightStatusBar">false</item>\n        <item name="android:windowLightNavigationBar">false</item>\n    </style>\n</resources>\n''', encoding='utf-8')

# Version for this status-bar fix.
gradle = Path('app/build.gradle')
gtext = gradle.read_text(encoding='utf-8')
gtext = re.sub(r'versionCode\s+\d+', 'versionCode 4', gtext, count=1)
gtext = re.sub(r"versionName\s+'[^']+'", "versionName '0.2.2'", gtext, count=1)
gradle.write_text(gtext, encoding='utf-8')

print('Applied TSF-style transparent visible status bar fix')
