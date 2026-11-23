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
import com.zsoftware.choresrpg.WeekDay;
import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ChoreAdapter extends BaseAdapter {
    private Context context;
    private final List<Chore> Chores;
    private WeekDay WeekDay;
    // The date the list is for; chores completed on this date get a green checkmark.
    private Calendar Date;

    public ChoreAdapter(Context context, List<Chore> chores, WeekDay weekDay, Calendar date) {
        this.context = context;
        this.Chores = new ArrayList();
        for (Chore c: chores) {
            if(c.IsOnDay(weekDay))
                Chores.add(c);
        }

        this.WeekDay = weekDay;
        this.Date = date;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View gridView;

        if (convertView == null) {
            gridView = inflater.inflate(R.layout.choreitem, parent, false);
        } else {
            gridView = (View) convertView;
        }

        // Fill the row every time: a recycled row (convertView) still shows the chore it was made for.
        Chore chore = Chores.get(position);

        ImageView imageView = (ImageView)gridView.findViewById(R.id.iconImage);
        imageView.setImageDrawable(ChoreIconDictionary.Instance().GetIcon(chore.GetIcon()));

        TextView textView = (TextView)gridView.findViewById(R.id.descriptionTextView);
        textView.setText(chore.GetDescription());

        ImageView completedView = (ImageView)gridView.findViewById(R.id.completedImage);
        completedView.setVisibility(chore.IsCompletedOn(Date) ? View.VISIBLE : View.GONE);

        return gridView;
    }

    @Override
    public int getCount() {
        return Chores.size();
    }
    @Override
    public Chore getItem(int position) {
        return Chores.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


}