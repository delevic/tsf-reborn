package rs.codel.tsfreborn;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

public final class LauncherItem {
    public final String label;
    public final ComponentName component;
    public final Drawable icon;

    public LauncherItem(String label, ComponentName component, Drawable icon) {
        this.label = label;
        this.component = component;
        this.icon = icon;
    }

    public String componentKey() {
        return component.flattenToString();
    }
}
