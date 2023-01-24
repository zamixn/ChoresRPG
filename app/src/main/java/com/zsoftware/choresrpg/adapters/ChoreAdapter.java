package com.zsoftware.choresrpg.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zsoftware.choresrpg.Chore;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;

import java.util.List;

public class ChoreAdapter extends BaseAdapter {
    private Context context;
    private final List<Chore> Chores;

    public ChoreAdapter(Context context, List<Chore> chores) {
        this.context = context;
        this.Chores = chores;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View gridView;

        if (convertView == null) {

            Chore chore = Chores.get(position);
            gridView = new View(context);

            gridView = inflater.inflate(R.layout.choreitem, null);

            ImageView imageView = (ImageView)gridView.findViewById(R.id.iconImage);
            imageView.setImageDrawable(ChoreIconDictionary.Instance().GetIcon(chore.GetIcon()));

            TextView textView = (TextView)gridView.findViewById(R.id.descriptionTextView);
            textView.setText(chore.GetDescription());

        } else {
            gridView = (View) convertView;
        }

        return gridView;
    }

    @Override
    public int getCount() {
        return Chores.size();
    }
    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


}