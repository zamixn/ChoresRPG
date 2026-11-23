package com.zsoftware.choresrpg.choreicon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.zsoftware.choresrpg.R;

// Grid cells of the icon picker: one per ChoreIconEnum, with the currently chosen icon marked.
public class ChoreImageAdapter extends BaseAdapter {
    private final Context context;
    private final ChoreIconEnum[] Icons = ChoreIconEnum.values();
    private final ChoreIconEnum SelectedIcon;

    public ChoreImageAdapter(Context context, ChoreIconEnum selectedIcon) {
        this.context = context;
        this.SelectedIcon = selectedIcon;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View gridView;
        if (convertView == null)
            gridView = LayoutInflater.from(context).inflate(R.layout.chore_image_item, parent, false);
        else
            gridView = convertView;

        // Set every time: the grid scrolls and reuses cells for other icons.
        ChoreIconEnum icon = Icons[position];
        ImageView imageView = gridView.findViewById(R.id.chore_image);
        imageView.setImageDrawable(ChoreIconDictionary.Instance().GetIcon(icon));
        imageView.setContentDescription(icon.name());
        gridView.setBackgroundResource(icon == SelectedIcon ? R.drawable.icon_picker_selected : 0);

        return gridView;
    }

    public int GetPosition(ChoreIconEnum icon)
    {
        return icon == null ? 0 : icon.ordinal();
    }

    @Override
    public int getCount() {
        return Icons.length;
    }

    @Override
    public ChoreIconEnum getItem(int position) {
        return Icons[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
