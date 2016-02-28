package com.mijaz.project.aione;

/**
 * Created by matrixmachine on 22/2/16.
 */
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomList extends ArrayAdapter<String>{

    private final Activity context;
    private final String[] application_title;
    private final String[] application_description;
    private final Integer[] application_img;
    public CustomList(Activity context,
                      String[] application_title,String[] application_description, Integer[] application_img) {
        super(context, R.layout.application_list_layout, application_title);
        this.context = context;
        this.application_title = application_title;
        this.application_description = application_description;
        this.application_img = application_img;

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.application_list_layout, null, true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.application_title);
        TextView txtDesctiption = (TextView) rowView.findViewById(R.id.application_description);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.application_img);

        txtTitle.setText(application_title[position]);
        txtDesctiption.setText(application_description[position]);
        imageView.setImageResource(application_img[position]);
        return rowView;
    }
}

