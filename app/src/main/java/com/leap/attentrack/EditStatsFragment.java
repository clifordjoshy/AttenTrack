package com.leap.attentrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

public class EditStatsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment_view = inflater.inflate(R.layout.fragment_all_classes, container, false);
        LinearLayout root_layout = fragment_view.findViewById(R.id.all_subs_root_layout);

        for(int i = 0; i < MainActivity.data.size(); ++i) {
            final Subject s = MainActivity.data.get(i);
            View element = getLayoutInflater().inflate(R.layout.element_all_subs, root_layout, false);
            ((TextView)element.findViewById(R.id.subject_text)).setText(s.name);
            String deets = "Total:" + s.total + "    Missed:" + s.missed + "    Missable:" + s.missable;
            ((TextView)element.findViewById(R.id.data_text)).setText(deets);
            ((TextView)element.findViewById(R.id.percent_text)).setText((s.attendance+"%"));
            ViewCompat.setBackgroundTintList(element, ColorStateList.valueOf(s.color));

            final TextView percent = element.findViewById(R.id.percent_text),
                    data = element.findViewById(R.id.data_text);

            element.findViewById(R.id.plus_image).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    s.unmiss_session();
                    percent.setText((s.attendance + "%"));
                    data.setText(("Total:" + s.total + "    Missed:" + s.missed + "    Missable:" + s.missable));
                }
            });
            element.findViewById(R.id.minus_image).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    s.missed_session();
                    percent.setText((s.attendance + "%"));
                    data.setText(("Total:" + s.total + "    Missed:" + s.missed + "    Missable:" + s.missable));
                }
            });
            element.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    s.cancel_session();
                    percent.setText((s.attendance + "%"));
                    data.setText(("Total:" + s.total + "    Missed:" + s.missed + "    Missable:" + s.missable));
                }
            });
            element.findViewById(R.id.subject_text).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(final View v) {
                   AlertDialog.Builder edit_dialog = new AlertDialog.Builder(getContext());
                   edit_dialog.setTitle("Enter Subject Name");
                   final EditText e = new EditText(getContext());
                   e.setBackgroundColor(s.color);
                   e.setHint(s.name);
                   e.setSingleLine(true);
                   e.setGravity(Gravity.CENTER);
                   e.setPadding(20, 20, 20, 20);
                   edit_dialog.setView(e);
                   edit_dialog.setCancelable(true);
                   edit_dialog.setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           String temp = e.getText().toString();
                           if (!"".equals(temp)) {
                               s.name = temp;
                               ((TextView) v).setText(temp);
                           }
                           dialog.cancel();
                       }
                   });
                   edit_dialog.show();
               }
            });

            root_layout.addView(element);
        }
        return fragment_view;
    }
}
