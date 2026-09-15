# TSF Reborn v0.2

Clean-room Android launcher rebuild inspired by the interaction model of the old TSF Shell.
The project does not copy TSF source code or assets. The decompiled application is used only to understand behavior while this implementation is written independently.

## v0.2 features

- Android HOME launcher role
- 3-page workspace
- application drawer
- tap an app to launch it
- **hold and drag an app from the drawer directly onto the current desktop page**
- free-position desktop shortcuts
- long-press a desktop shortcut to move it
- drag a shortcut to the top REMOVE target to delete it
- persistent normalized shortcut positions
- JSON layout backup / restore
- long-press empty desktop to enter **Edit mode**
- freehand **lasso selection** in Edit mode
- tap icons to add/remove them from the selection
- Select all / Delete / Done edit toolbar
- modern edge-to-edge fullscreen window handling

## Basic test sequence

1. Open the project in Android Studio.
2. Allow Gradle Sync to finish.
3. Run the `app` configuration on the test phone.
4. Choose TSF Reborn as the Home app when Android asks.
5. Open the drawer with the bottom-center button.
6. Long-press an app and drag it onto the desktop.
7. Long-press empty desktop space to enter Edit mode.
8. Draw a closed loop around several icons; their centers inside the lasso become selected.
9. Use Select all / Delete / Done in the top toolbar.
10. Long-press the drawer button for Backup / Restore / default Home options.

## Build requirements

- Android Studio with Android SDK 35 installed
- JDK 17 compatible Android Gradle setup
- minSdk 26, targetSdk 35

This is still an early functional prototype. Widgets, folders, TSF-style 3D scenes/animations, icon effects, dock customization and notification badges are intentionally scheduled for later milestones.
