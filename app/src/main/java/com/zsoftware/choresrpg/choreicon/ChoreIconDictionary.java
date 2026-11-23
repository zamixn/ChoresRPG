package com.zsoftware.choresrpg.choreicon;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Gives the drawable of each chore icon. There are a few hundred icons, so each one is loaded the
// first time it's needed and then kept, instead of loading all of them when a screen opens.
public class ChoreIconDictionary {
    private static String LOGTAG = "ChoreIconDictionary";
    private static ChoreIconDictionary _Instance;
    public static ChoreIconDictionary Instance()
    {
        if (_Instance == null)
            Log.e(LOGTAG, "Instance: is null");
        return _Instance;
    }
    public static void Initialize(Activity context)
    {
        // Every screen calls this; keep the loaded icons if it was already done.
        if (_Instance == null)
            _Instance = new ChoreIconDictionary(context);
    }

    private final Context AppContext;
    private final Map<ChoreIconEnum, Drawable> IconDictionary = new EnumMap<>(ChoreIconEnum.class);
    private ChoreIconDictionary(Activity context)
    {
        AppContext = context.getApplicationContext();
    }

    public Drawable GetIcon(ChoreIconEnum icon)
    {
        if (icon == null)
            return null;
        Drawable drawable = IconDictionary.get(icon);
        if (drawable == null)
        {
            drawable = ContextCompat.getDrawable(AppContext, icon.GetDrawableId());
            IconDictionary.put(icon, drawable);
        }
        return drawable;
    }

    // All icons, in ChoreIconEnum order (loads every icon).
    public List<Drawable> GetResourceList()
    {
        List<Drawable> drawables = new ArrayList<>();
        for (ChoreIconEnum icon : ChoreIconEnum.values())
            drawables.add(GetIcon(icon));
        return drawables;
    }

    public ChoreIconEnum GetChoreIconFromDrawable(Drawable d)
    {
        for (Map.Entry<ChoreIconEnum, Drawable> entry : IconDictionary.entrySet())
        {
            if (entry.getValue() == d)
                return entry.getKey();
        }
        return null;
    }
}
