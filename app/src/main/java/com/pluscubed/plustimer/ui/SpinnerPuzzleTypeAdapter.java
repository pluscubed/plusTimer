package com.pluscubed.plustimer.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;

/**
 * An Adapter for a Spinner with values of the PuzzleTypes
 */
public class SpinnerPuzzleTypeAdapter extends ArrayAdapter<PuzzleType> {

    private LayoutInflater mLayoutInflater;

    private Context mContext;

    public SpinnerPuzzleTypeAdapter(LayoutInflater inflater, Context context) {
        super(context, 0, PuzzleType.getEnabledPuzzleTypes());
        mLayoutInflater = inflater;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = getItem(position).isScramblerOfficial() ? mLayoutInflater
                .inflate(R.layout.spinner_item, parent, false)
                : mLayoutInflater.inflate(R.layout.spinner_item2, parent,
                false);

        TextView textView = (TextView) convertView.findViewById(android.R.id
                .text1);
        textView.setText(getItem(position).getName());
        textView.setTextColor(Color.WHITE);

        if (!getItem(position).isScramblerOfficial()) {
            TextView textView2 = (TextView) convertView.findViewById(android
                    .R.id.text2);
            textView2.setText(mContext.getString(R.string.unofficial));
            textView2.setTextColor(Color.WHITE);
        }
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        convertView = getItem(position).isScramblerOfficial() ? mLayoutInflater
                .inflate(R.layout.spinner_item_dropdown, parent, false)
                : mLayoutInflater.inflate(R.layout.spinner_item_dropdown2,
                parent, false);

        TextView textView = (TextView) convertView.findViewById(android.R.id
                .text1);
        textView.setText(getItem(position).getName());
        textView.setTextColor(mContext.getResources().getColorStateList(R
                .color.list_dropdown_color_dark));
        if (!getItem(position).isScramblerOfficial()) {
            TextView textView2 = (TextView) convertView.findViewById(android
                    .R.id.text2);
            textView2.setText(mContext.getString(R.string.unofficial));
            textView2.setTextColor(mContext.getResources().getColorStateList
                    (R.color.list_dropdown_color_dark));
        }
        return convertView;
    }
}
