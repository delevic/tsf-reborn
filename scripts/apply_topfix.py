from pathlib import Path
import re

main = Path('app/src/main/java/rs/codel/tsfreborn/MainActivity.java')
text = main.read_text(encoding='utf-8')

new_method = '''    private void configureWindow() {
        Window w = getWindow();

        // Force fullscreen before the content view is measured. Some OEM
        // builds keep a status-bar-sized content inset if fullscreen is only
        // requested later through WindowInsetsController.
        w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        }

        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
'''

pattern = re.compile(r'    private void configureWindow\(\) \{.*?\n    \}\n\n(?=    private void hideStatusBar\(\))', re.S)
updated, count = pattern.subn(new_method + '\n', text, count=1)
if count != 1:
    raise SystemExit('Could not locate configureWindow() for migration')
main.write_text(updated, encoding='utf-8')

styles = Path('app/src/main/res/values/styles.xml')
styles.write_text('''<resources>\n    <style name="Theme.TSFReborn" parent="android:style/Theme.Material.NoActionBar">\n        <item name="android:fontFamily">sans</item>\n        <item name="android:windowNoTitle">true</item>\n        <item name="android:windowFullscreen">true</item>\n        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>\n        <item name="android:windowActionModeOverlay">true</item>\n        <item name="android:windowShowWallpaper">true</item>\n        <item name="android:windowBackground">@android:color/transparent</item>\n        <item name="android:colorAccent">#FFFFFFFF</item>\n        <item name="android:statusBarColor">@android:color/transparent</item>\n        <item name="android:navigationBarColor">#CC000000</item>\n        <item name="android:windowLightStatusBar">false</item>\n        <item name="android:windowLightNavigationBar">false</item>\n    </style>\n</resources>\n''', encoding='utf-8')
