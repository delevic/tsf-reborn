# TSF Reborn architecture – v0.2

## UI tree

`MainActivity` owns a transparent root `FrameLayout` over the system wallpaper.

- `WorkspacePager`
  - `WorkspacePage[0..2]`
    - `IconTile` children
- remove target
- edit toolbar
- drawer button + page indicator
- `DrawerOverlay`

## Workspace model

`ShortcutRecord` stores component name, page and normalized x/y coordinates. `LayoutStore` persists records and exports/imports the same model as JSON.

## Drag from drawer

The app drawer uses Android's platform drag-and-drop transport (`startDragAndDrop`) with the `LauncherItem` as local state. Once drag begins the drawer is hidden, exposing the workspace. The current `WorkspacePage` accepts `DragEvent.ACTION_DROP`, converts the drop point into a new freely positioned shortcut and persists its normalized coordinates.

## Edit / lasso

A long press on empty workspace enters edit mode. In edit mode the page captures touch instead of pager swipes and records a freehand `Path`. At release the path is closed and converted to a `Region`; icon centers inside that region are selected. Selection is visual only until an action (currently Delete) commits a model change.

## Next layers

The planned scene/effects layer should stay independent from the Android launcher model. That lets TSF-like 3D transitions be added without coupling icon persistence, app discovery, widgets and backup to a custom rendering engine.
