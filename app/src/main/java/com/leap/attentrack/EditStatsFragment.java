package com.leap.attentrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;

public class EditStatsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment_view = inflater.inflate(R.layout.fragment_all_classes, container, false);
        LinearLayout root_layout = fragment_view.findViewById(R.id.all_subs_root_layout);

        for(int i = 0; i < MainActivity.data.length; ++i) {
            final Subject s = MainActivity.data[i];
            View element = getLayoutInflater().inflate(R.layout.element_all_subs, root_layout, false);
            ((TextView)element.findViewById(R.id.subject_text)).setText(s.name);
            update_deets(s, (TextView)element.findViewById(R.id.percent_text), (TextView)element.findViewById(R.id.data_text));

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                element.getBackground().setColorFilter(s.color, PorterDuff.Mode.MULTIPLY);
            else
                element.setBackgroundTintList(ColorStateList.valueOf(s.color));

            final TextView percent = element.findViewById(R.id.percent_text),
                    data = element.findViewById(R.id.data_text);

            element.findViewById(R.id.plus_image).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    s.unmiss_session();
                    update_deets(s, percent, data);
                }
            });
            element.findViewById(R.id.minus_image).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    s.missed_session();
                    update_deets(s, percent, data);
                }
            });
            element.findViewById(R.id.remove_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    s.cancel_session();
                    update_deets(s, percent, data);
                }
            });
            element.findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    s.add_session();
                    update_deets(s, percent, data);
                }
            });
            element.findViewById(R.id.subject_text).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(final View v) {
                   AlertDialog.Builder edit_dialog = new AlertDialog.Builder(getContext(), R.style.ThemedAlertDialog);
                   edit_dialog.setTitle(R.string.subject_name_edit_title);
                   final EditText e = new EditText(getContext());
                   e.setBackgroundColor(s.color);
                   e.setHint(s.name);
                   e.setSingleLine(true);
                   e.setGravity(Gravity.CENTER);
                   e.setPadding(20, 20, 20, 20);
                   edit_dialog.setView(e);
                   edit_dialog.setCancelable(true);
                   edit_dialog.setPositiveButton(R.string.save_text, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           String temp = e.getText().toString();
                           if (!"".equals(temp)) {
                               s.name = temp;
                               ((TextView) v).setText(temp);
                           }
                       }
                   });
                   edit_dialog.show();
               }
            });

            root_layout.addView(element);
        }

        handle_first_start(root_layout);
        return fragment_view;
    }

    private void update_deets(Subject s, TextView percent, TextView deet){
        percent.setText((s.attendance + "%"));
        String deet_text = getString(R.string.total_text) + s.total + "    "+
                getString(R.string.missed_text) + s.missed + "    " +
                getString(R.string.missable_text) + s.missable;
        deet.setText(deet_text);
    }

    private void handle_first_start(final LinearLayout root){
        boolean first = getActivity().getSharedPreferences(MainActivity.shared_pref_name, MainActivity.MODE_PRIVATE).
                getBoolean("edit_stats_first_start", true);
        if(!first)
            return;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new GuideView.Builder(getContext())
                        .setTitle(getString(R.string.edit_stats_guide_title))
                        .setContentText(getString(R.string.edit_stats_guide_message))
                        .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                        .setTargetView(root.getChildAt(0))
                        .build()
                        .show();
            }
        }, 500);

        getActivity().getSharedPreferences(MainActivity.shared_pref_name, MainActivity.MODE_PRIVATE).
                edit().putBoolean("edit_stats_first_start", false).apply();

    }
}
