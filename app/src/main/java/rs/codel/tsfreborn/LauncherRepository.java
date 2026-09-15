package rs.codel.tsfreborn;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LauncherRepository {
    private final Context context;
    private final PackageManager pm;

    public LauncherRepository(Context context) {
        this.context = context.getApplicationContext();
        this.pm = context.getPackageManager();
    }

    public List<LauncherItem> loadLaunchableApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        ArrayList<LauncherItem> result = new ArrayList<>();

        for (ResolveInfo ri : infos) {
            if (ri.activityInfo == null) continue;
            ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            // Do not show TSF Reborn itself in its own drawer.
            if (context.getPackageName().equals(cn.getPackageName())) continue;
            CharSequence cs = ri.loadLabel(pm);
            String label = cs == null ? ri.activityInfo.packageName : cs.toString();
            Drawable icon = ri.loadIcon(pm);
            result.add(new LauncherItem(label, cn, icon));
        }

        final Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(result, new Comparator<LauncherItem>() {
            @Override public int compare(LauncherItem a, LauncherItem b) {
                return collator.compare(a.label, b.label);
            }
        });
        return result;
    }
}
