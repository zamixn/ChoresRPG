package com.zsoftware.choresrpg.choreicon;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.zsoftware.choresrpg.R;

import java.util.List;

public class ChoreImageAdapter extends BaseAdapter {
    private Context context;
    private final List<Drawable> mThumbIds;
    private Dialog Dialog;

    public ChoreImageAdapter(Context context, Dialog dialog, List<Drawable> mThumbIds) {
        this.context = context;
        this.mThumbIds = mThumbIds;
        this.Dialog = dialog;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View gridView;

        if (convertView == null) {

            gridView = new View(context);

            // get layout from mobile.xml
            gridView = inflater.inflate(R.layout.chore_image_item, null);

            // set image based on selected text
            ImageView imageView = (ImageView) gridView
                    .findViewById(R.id.chore_image);


            imageView.setImageDrawable(mThumbIds.get(position));


        } else {
            gridView = (View) convertView;
        }

        return gridView;
    }

    @Override
    public int getCount() {
        return mThumbIds.size();
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