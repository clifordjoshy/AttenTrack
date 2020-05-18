package com.leap.attentrack;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.transition.TransitionManager;

import com.google.android.gms.ads.InterstitialAd;

import java.util.Collections;
import java.util.LinkedList;

public class TimetableFragment extends Fragment {

    private TextView[] boxes;
    private TextView title;
    private LinearLayout list;
    private ConstraintLayout root;
    private Menu options_menu;
    private LinkedList<Subject> table;
    private Spinner[][] tt_spinners;
    private ArrayAdapter<String> spinner_options;
    private int active_box = -1;
    private float density;
    private Typeface font_med;
    private boolean dark, editing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_timetable, container, false);

        density = getContext().getResources().getDisplayMetrics().density;
        list = fragmentView.findViewById(R.id.linear_timetable);
        root = fragmentView.findViewById(R.id.time_table_root);
        title = fragmentView.findViewById(R.id.time_table_title);
        boxes = new TextView[]{fragmentView.findViewById(R.id.box_day_1),
                fragmentView.findViewById(R.id.box_day_2), fragmentView.findViewById(R.id.box_day_3),
                fragmentView.findViewById(R.id.box_day_4), fragmentView.findViewById(R.id.box_day_5),
                fragmentView.findViewById(R.id.box_day_6), fragmentView.findViewById(R.id.box_day_7)};
        font_med = ResourcesCompat.getFont(getContext(), R.font.poppins_regular);
        dark = MainActivity.dark_mode_on;

        boxes[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                box_clicked(0);
            }
        });
        boxes[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                box_clicked(1);
            }
        });
        boxes[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                box_clicked(2);
            }
        });
        boxes[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                box_clicked(3);
            }
        });
        boxes[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                box_clicked(4);
            }
        });
        boxes[5].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                box_clicked(5);
            }
        });
        boxes[6].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                box_clicked(6);
            }
        });

        table = MainActivity.data;
        setHasOptionsMenu(true);
        return fragmentView;
    }

    private void box_clicked(int boxno) {
        TransitionManager.beginDelayedTransition(list);

        if (active_box != -1) {
            if (editing)
                ((GridLayout) (list.getChildAt(active_box + 1))).removeAllViews();
            list.removeViewAt(active_box + 1);
        }

        if (boxno != active_box) {
            active_box = boxno;

            GridLayout grid = new GridLayout(getContext());
            grid.setBackgroundResource(R.drawable.curve_bottom_only_10dp);
            LinearLayout.LayoutParams gridparams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gridparams.setMargins((int) (density * 25), 0, (int) (density * 25), 0);
            grid.setLayoutParams(gridparams);
            grid.setPadding((int) (density * 5), 0, (int) (density * 5), (int) (density * 2));
            int gridwidth = list.getWidth() - (int) (density * 60),  //60 = padding + margin
                    color = ViewCompat.getBackgroundTintList(boxes[boxno]).getDefaultColor();
            color = 0xffffffff + color + 1;  // 2s complement
            color = color - 0x9f000000;     //Opacity
            ViewCompat.setBackgroundTintList(grid, ColorStateList.valueOf(color));
            list.addView(grid, boxno + 1);

            if (editing) {
                for (int i = 0; i < Subject.session_encoder.size(); ++i) {
                    grid.addView(createTextView(i, 0, Subject.session_encoder.get(i)[0], gridwidth / 5, Gravity.END));
                    grid.addView(createTextView(i, 1, "-", gridwidth / 15, Gravity.CENTER_HORIZONTAL));
                    grid.addView(createTextView(i, 2, Subject.session_encoder.get(i)[1], gridwidth / 5, Gravity.START));

                    if (tt_spinners[boxno][i] == null) {
                        GridLayout.LayoutParams lparams = new GridLayout.LayoutParams(GridLayout.spec(i), GridLayout.spec(3));
                        lparams.height = (int) (30 * density);
                        lparams.width = 8 * gridwidth / 15;
                        lparams.setGravity(Gravity.BOTTOM);
                        lparams.setMargins(0, (int) (5 * density), 0, 0);
                        Spinner s = new Spinner(getContext());
                        s.setLayoutParams(lparams);
                        s.setPopupBackgroundDrawable(new ColorDrawable(dark ? 0xff272727 : 0xffffffff));
                        s.setAdapter(spinner_options);
                        tt_spinners[boxno][i] = s;
                    }
                    grid.addView(tt_spinners[boxno][i]);

                    for (int j = 0; j < table.size(); ++j) {
                        if (table.get(j).slots[boxno].contains(i)) {
                            tt_spinners[boxno][i].setSelection(j + 1);
                        }
                    }
                }

            } else {
                for (int i = 0; i < Subject.session_encoder.size(); ++i) {
                    for (Subject s : table) {
                        if (s.slots[boxno].contains(i)) {
                            grid.addView(createTextView(i, 0, Subject.session_encoder.get(i)[0], gridwidth / 5, Gravity.END));
                            grid.addView(createTextView(i, 1, "-", gridwidth / 15, Gravity.CENTER_HORIZONTAL));
                            grid.addView(createTextView(i, 2, Subject.session_encoder.get(i)[1], gridwidth / 5, Gravity.START));
                            grid.addView(createTextView(i, 3, s.name, 8 * gridwidth / 15, Gravity.END));
                            break;
                        }
                    }
                }
            }


        } else {
            active_box = -1;
        }
    }

    private TextView createTextView(int row, int column, String text, int width, int gravity) {
        GridLayout.LayoutParams lparams = new GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column));
        lparams.width = width;
        lparams.height = (int) (30 * density);
        lparams.setMargins(0, (int) (5 * density), 0, 0);
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTypeface(font_med);
        textView.setTextColor(dark ? 0xffffffff : 0xff272727);
        textView.setLayoutParams(lparams);
        textView.setGravity(gravity);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(textView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        return textView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.timetable_options_menu, menu);
        options_menu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        TransitionManager.beginDelayedTransition(root);
        if (active_box != -1) {
            list.removeViewAt(active_box + 1);
            active_box = -1;
        }

        switch(item.getItemId()){

            case R.id.edit_timetable_button:
                editing = true;
                title.setText(R.string.time_table_edit);
                root.findViewById(R.id.edit_note).setVisibility(View.VISIBLE);
                item.setVisible(false);
                options_menu.findItem(R.id.save_timetable_button).setVisible(true);
                options_menu.findItem(R.id.cancel_timetable_button).setVisible(true);
                tt_spinners = new Spinner[7][Subject.session_encoder.size()];
                String[] spinner_options_array = new String[table.size() + 1];
                spinner_options_array[0] = "<free>";
                for (int i = 1; i < spinner_options_array.length; ++i)
                    spinner_options_array[i] = table.get(i - 1).name;
                spinner_options = new ArrayAdapter<>(getContext(), R.layout.spinner_item, spinner_options_array);
                break;

            case R.id.save_timetable_button:
                editing = false;
                title.setText(R.string.time_table_title);
                root.findViewById(R.id.edit_note).setVisibility(View.GONE);

                item.setVisible(false);
                options_menu.findItem(R.id.cancel_timetable_button).setVisible(false);
                options_menu.findItem(R.id.edit_timetable_button).setVisible(true);

                Toast.makeText(getContext(), "Edits Saved", Toast.LENGTH_SHORT).show();

                for (int i = 0; i < tt_spinners.length; ++i) {
                    for (int j = 0; j < tt_spinners[i].length; ++j) {
                        if (tt_spinners[i][j] != null) {

                            //Remove the slot if it's already there
                            for(Subject k: table)
                                k.slots[i].remove(Integer.valueOf(j));

                            try {
                                Subject to_edit = table.get(tt_spinners[i][j].getSelectedItemPosition() - 1);
                                to_edit.slots[i].add(j);
                            } catch(IndexOutOfBoundsException e){
                                //Free Slot
                            }
                        }
                    }
                }

                tt_spinners = null;
                spinner_options = null;
                break;

            case R.id.cancel_timetable_button:
                editing = false;
                title.setText(R.string.time_table_title);
                root.findViewById(R.id.edit_note).setVisibility(View.GONE);

                item.setVisible(false);
                options_menu.findItem(R.id.save_timetable_button).setVisible(false);
                options_menu.findItem(R.id.edit_timetable_button).setVisible(true);

                Toast.makeText(getContext(), "Edits Cancelled", Toast.LENGTH_SHORT).show();
                tt_spinners = null;
                spinner_options = null;
                break;
        }



//        if (item.getItemId() == R.id.edit_timetable_button) {
//            editing = !editing;
//            TransitionManager.beginDelayedTransition(root);

//            if (active_box != -1) {
//                list.removeViewAt(active_box + 1);
//                active_box = -1;
//            }
//
//            if (editing) {
//                title.setText(R.string.time_table_edit);
//                root.findViewById(R.id.edit_note).setVisibility(View.VISIBLE);
//                item.setIcon(R.drawable.toolbar_icon_save);
//                tt_spinners = new Spinner[7][Subject.session_encoder.size()];
//                String[] spinner_options_array = new String[table.size() + 1];
//                spinner_options_array[0] = "<free>";
//                for (int i = 1; i < spinner_options_array.length; ++i)
//                    spinner_options_array[i] = table.get(i - 1).name;
//                spinner_options = new ArrayAdapter<>(getContext(), R.layout.spinner_item, spinner_options_array);

//            } else {
//                title.setText(R.string.time_table_title);
//                root.findViewById(R.id.edit_note).setVisibility(View.GONE);
//                item.setIcon(R.drawable.toolbar_icon_edit);
//                Toast.makeText(getContext(), "Edits Saved", Toast.LENGTH_SHORT).show();
//
//                for (int i = 0; i < tt_spinners.length; ++i) {
//                    for (int j = 0; j < tt_spinners[i].length; ++j) {
//                        if (tt_spinners[i][j] != null) {
//
//                            //Remove the slot if it's already there
//                            for(Subject k: table)
//                                k.slots[i].remove(Integer.valueOf(j));
//
//                            try {
//                                Subject to_edit = table.get(tt_spinners[i][j].getSelectedItemPosition() - 1);
//                                to_edit.slots[i].add(j);
//                            } catch(IndexOutOfBoundsException e){
//                                //Free Slot
//                            }
//                        }
//                    }
//                }
//
//                tt_spinners = null;
//                spinner_options = null;
//            }
//        }
        return super.onOptionsItemSelected(item);
    }
}