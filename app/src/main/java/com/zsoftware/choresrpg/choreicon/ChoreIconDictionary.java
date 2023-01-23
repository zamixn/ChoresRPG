package com.zsoftware.choresrpg.choreicon;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;

import java.util.Dictionary;
import java.util.Hashtable;

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
        _Instance = new ChoreIconDictionary(context);
    }

    private Dictionary<ChoreIconEnum, Drawable> IconDictionary;
    private ChoreIconDictionary(Activity context)
    {
        IconDictionary = new Hashtable<>();
        Resources res = context.getResources();
        IconDictionary.put(ChoreIconEnum.Cleaning, ContextCompat.getDrawable(context, R.drawable.cleaning));
        IconDictionary.put(ChoreIconEnum.Sigh, ContextCompat.getDrawable(context, R.drawable.breath));
        IconDictionary.put(ChoreIconEnum.FloorMop, ContextCompat.getDrawable(context, R.drawable.floor_mop));
        IconDictionary.put(ChoreIconEnum.Fridge, ContextCompat.getDrawable(context, R.drawable.fridge));
    }

    public Drawable GetIcon(ChoreIconEnum icon)
    {
        return IconDictionary.get(icon);
    }
}
