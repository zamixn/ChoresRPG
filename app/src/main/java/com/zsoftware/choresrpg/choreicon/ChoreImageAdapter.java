package com.zsoftware.choresrpg.choreicon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.zsoftware.choresrpg.R;

import java.util.ArrayList;
import java.util.List;

// Grid cells of the icon picker: the icons matching the search text, with the currently chosen icon marked.
public class ChoreImageAdapter extends BaseAdapter {
    private final Context context;
    private final List<ChoreIconEnum> Icons = new ArrayList<>();
    private final ChoreIconEnum SelectedIcon;

    public ChoreImageAdapter(Context context, ChoreIconEnum selectedIcon) {
        this.context = context;
        this.SelectedIcon = selectedIcon;
        SetSearchText("");
    }

    // Shows only icons whose tags match the search text (all icons for empty text).
    public void SetSearchText(String searchText)
    {
        Icons.clear();
        for (ChoreIconEnum icon : ChoreIconEnum.values())
        {
            if (icon.MatchesSearch(searchText))
                Icons.add(icon);
        }
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View gridView;
        if (convertView == null)
            gridView = LayoutInflater.from(context).inflate(R.layout.chore_image_item, parent, false);
        else
            gridView = convertView;

        // Set every time: the grid scrolls and reuses cells for other icons.
        ChoreIconEnum icon = Icons.get(position);
        ImageView imageView = gridView.findViewById(R.id.chore_image);
        imageView.setImageDrawable(ChoreIconDictionary.Instance().GetIcon(icon));
        imageView.setContentDescription(icon.name());
        gridView.setBackgroundResource(icon == SelectedIcon ? R.drawable.icon_picker_selected : 0);

        return gridView;
    }

    // Position of the icon in the current list, or 0 if it isn't shown.
    public int GetPosition(ChoreIconEnum icon)
    {
        int position = Icons.indexOf(icon);
        return Math.max(position, 0);
    }

    @Override
    public int getCount() {
        return Icons.size();
    }

    @Override
    public ChoreIconEnum getItem(int position) {
        return Icons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).ordinal();
    }
}
