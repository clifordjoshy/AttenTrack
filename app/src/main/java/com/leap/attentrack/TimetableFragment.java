package com.leap.attentrack;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
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
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.transition.Explode;
import androidx.transition.TransitionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class TimetableFragment extends Fragment {

    private TextView[] boxes;
    private LinearLayout list;
    private Menu options_menu;
    TextSwitcher title;
    private Subject[] data;
    private Spinner[][] tt_spinners;
    private ArrayAdapter<String> spinner_options;
    private int active_box = -1;
    private float density;
    private boolean dark, editing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_timetable, container, false);

        density = getContext().getResources().getDisplayMetrics().density;
        list = fragmentView.findViewById(R.id.linear_timetable);
        boxes = new TextView[]{fragmentView.findViewById(R.id.box_day_1),
                fragmentView.findViewById(R.id.box_day_2), fragmentView.findViewById(R.id.box_day_3),
                fragmentView.findViewById(R.id.box_day_4), fragmentView.findViewById(R.id.box_day_5),
                fragmentView.findViewById(R.id.box_day_6), fragmentView.findViewById(R.id.box_day_7)};
        dark = MainActivity.dark_mode_on;

        title = fragmentView.findViewById(R.id.time_table_title);
        title.setCurrentText(getString(R.string.time_table_title));
        title.setInAnimation(getContext(), android.R.anim.slide_in_left);
        title.setOutAnimation(getContext(), android.R.anim.slide_out_right);

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

        data = MainActivity.data;
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
                    color = boxes[boxno].getBackgroundTintList().getDefaultColor();
            color = color - 0x9f000000;     //Opacity

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                grid.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            else
                grid.setBackgroundTintList(ColorStateList.valueOf(color));

            list.addView(grid, boxno + 1);

            if (editing) {
                for (int i = 0; i < Subject.session_encoder.length; ++i) {
                    grid.addView(createTextView(i, 0, Subject.session_encoder[i][0], gridwidth / 5, Gravity.END));
                    grid.addView(createTextView(i, 1, "-", gridwidth / 15, Gravity.CENTER_HORIZONTAL));
                    grid.addView(createTextView(i, 2, Subject.session_encoder[i][1], gridwidth / 5, Gravity.START));

                    if (tt_spinners[boxno][i] == null) {
                        GridLayout.LayoutParams lparams = new GridLayout.LayoutParams(GridLayout.spec(i), GridLayout.spec(3));
                        lparams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                        lparams.width = 8 * gridwidth / 15;
                        lparams.setGravity(Gravity.CENTER);
                        lparams.setMargins(0, (int) (5 * density), 0, 0);
                        Spinner s = new Spinner(getContext());
                        s.setLayoutParams(lparams);
                        s.setPopupBackgroundDrawable(new ColorDrawable(dark ? 0xff272727 : 0xffffffff));
                        s.setAdapter(spinner_options);
                        s.setBackgroundTintList(ColorStateList.valueOf(0xff808080));
                        tt_spinners[boxno][i] = s;
                        for (int j = 0; j < data.length; ++j) {
                            if (data[j].slots[boxno].contains(i)) {
                                tt_spinners[boxno][i].setSelection(j + 1);
                            }
                        }
                    }
                    grid.addView(tt_spinners[boxno][i]);
                }

            } else {
                boolean isEmpty = true;
                for (int i = 0; i < Subject.session_encoder.length; ++i) {
                    for (Subject s : data) {
                        if (s.slots[boxno].contains(i)) {
                            isEmpty = false;
                            grid.addView(createTextView(i, 0, Subject.session_encoder[i][0], gridwidth / 5, Gravity.END));
                            grid.addView(createTextView(i, 1, "-", gridwidth / 15, Gravity.CENTER_HORIZONTAL));
                            grid.addView(createTextView(i, 2, Subject.session_encoder[i][1], gridwidth / 5, Gravity.START));
                            grid.addView(createTextView(i, 3, s.name, 8 * gridwidth / 15, Gravity.END));
                            break;
                        }
                    }
                }
                if (isEmpty)
                    Toast.makeText(getContext(), R.string.empty_tt_toast, Toast.LENGTH_SHORT).show();
            }


        } else {
            active_box = -1;
        }
    }

    private TextView createTextView(int row, int column, String text, int width, int gravity) {
        GridLayout.LayoutParams lparams = new GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column));
        lparams.width = width;
        lparams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        lparams.setMargins(0, (int) (5 * density), 0, 0);
        AppCompatTextView textView = new AppCompatTextView(getContext());
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textView.setTextColor(dark ? 0xffffffff : 0xff272727);
        textView.setLayoutParams(lparams);
        textView.setGravity(gravity);
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

        switch (item.getItemId()) {
            case R.id.edit_timetable_button: {
                TransitionManager.beginDelayedTransition(list);
                if (active_box != -1) {
                    list.removeViewAt(active_box + 1);
                    active_box = -1;
                }
                editing = true;
                title.setText(getString(R.string.time_table_edit));

                Explode explode = new Explode();
                explode.setDuration(400);
                TransitionManager.beginDelayedTransition((Toolbar) getActivity().findViewById(R.id.toolbar), explode);
                item.setVisible(false);
                options_menu.findItem(R.id.save_timetable_button).setVisible(true);
                options_menu.findItem(R.id.cancel_timetable_button).setVisible(true);
                tt_spinners = new Spinner[7][Subject.session_encoder.length];
                String[] spinner_options_array = new String[data.length + 1];
                spinner_options_array[0] = getString(R.string.free_session_text);
                for (int i = 1; i < spinner_options_array.length; ++i)
                    spinner_options_array[i] = data[i - 1].name;
                spinner_options = new ArrayAdapter<>(getContext(), R.layout.spinner_item, spinner_options_array);
                break;
            }
            case R.id.save_timetable_button: {

                final int[] changes = get_distribution_changes();

                if (changes == null)
                    save_timetable_changes();

                else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
                    dialog.setTitle(R.string.warning_title);
                    final StringBuilder s = new StringBuilder(getString(R.string.time_table_change_warning));
                    for (int i = 0; i < changes.length; ++i) {
                        if (changes[i] != 0) {
                            s.append('\t').append(data[i].name).append(" : ");
                            s.append(data[i].total).append(" → ");
                            s.append(data[i].total + changes[i]).append('\n');
                        }
                    }
                    dialog.setMessage(s);
                    dialog.setNegativeButton(R.string.edit_button_text, null);

                    dialog.setNeutralButton(R.string.ignore_button_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            save_timetable_changes();
                        }
                    });

                    dialog.setPositiveButton(R.string.confirm_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (int i = 0; i < changes.length; ++i)
                                data[i].total += changes[i];
                            save_timetable_changes();
                        }
                    });
                    dialog.show();
                }
                break;
            }
            case R.id.cancel_timetable_button: {
                TransitionManager.beginDelayedTransition(list);
                if (active_box != -1) {
                    list.removeViewAt(active_box + 1);
                    active_box = -1;
                }

                editing = false;
                title.setText(getString(R.string.time_table_title));

                Explode explode = new Explode();
                explode.setDuration(400);
                TransitionManager.beginDelayedTransition((Toolbar) getActivity().findViewById(R.id.toolbar), explode);
                options_menu.findItem(R.id.save_timetable_button).setVisible(false);
                item.setVisible(false);
                options_menu.findItem(R.id.edit_timetable_button).setVisible(true);

                Toast.makeText(getContext(), R.string.tt_edits_cancelled_toast, Toast.LENGTH_SHORT).show();
                tt_spinners = null;
                spinner_options = null;
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private int[] get_distribution_changes() {

        Date start, end;

        try {
            String start_string, end_string;
            SimpleDateFormat string_format = new SimpleDateFormat("dd/MM/yyyy");
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Date today = now.getTime();

            SharedPreferences sp = getActivity().getSharedPreferences(MainActivity.shared_pref_name, Context.MODE_PRIVATE);
            start_string = sp.getString("sem_start_date", null);
            end_string = sp.getString("sem_end_date", null);

            if (start_string != null && end_string != null) {
                start = string_format.parse(start_string);
                end = string_format.parse(end_string);
                if (start.before(today))
                    start = today;
            } else
                return null;

        } catch (ParseException e) {
            return null;
        }


        Calendar c = Calendar.getInstance();
        c.setTime(start);
        int start_day = c.get(Calendar.DAY_OF_WEEK) - 1;    //sun-sat 0 -6
        start_day = start_day == 0 ? 6 : start_day - 1;     //mon-sun
        c.setTime(end);
        int end_day = c.get(Calendar.DAY_OF_WEEK) - 1;
        end_day = end_day == 0 ? 6 : end_day - 1;     //mon-sun

        int weeks_to_go = (int) ((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24 * 7));
        int[] subject_distr_old = new int[data.length];
        int[] subject_distr_new = new int[data.length];

        //old tt classes
        for (int i = 0; i < data.length; ++i) {
            for (LinkedList<Integer> day : data[i].slots)
                subject_distr_old[i] += day.size();
            subject_distr_old[i] *= weeks_to_go;
        }

        for (int day = start_day; day != end_day; day = day == 6 ? 0 : (day + 1)) {  //To account for remainder days.
            for (int i = 0; i < data.length; ++i)
                subject_distr_old[i] += data[i].slots[day].size();
        }

        //new tt classes
        for (int i = 0; i < tt_spinners.length; ++i) {
            for (int j = 0; j < tt_spinners[i].length; ++j) {
                int sub_ind = get_subject_new_tt(i, j);
                if (sub_ind != -1)
                    ++subject_distr_new[sub_ind];
            }
        }
        for (int i = 0; i < subject_distr_new.length; ++i)
            subject_distr_new[i] *= weeks_to_go;

        for (int day = start_day; day != end_day; day = day == 6 ? 0 : (day + 1)) {  //To account for remainder days.
            for (int j = 0; j < tt_spinners[day].length; ++j) {
                int sub_ind = get_subject_new_tt(day, j);
                if (sub_ind != -1) {
                    ++subject_distr_new[sub_ind];
                }
            }
        }

        boolean changed = false;
        int[] changes = new int[data.length];
        for (int i = 0; i < changes.length; ++i) {
            changes[i] = subject_distr_new[i] - subject_distr_old[i];
            if (changes[i] != 0)
                changed = true;
        }
        if (changed)
            return changes;
        else
            return null;
    }

    private int get_subject_new_tt(int day_num, int sess_num) {
        if (tt_spinners[day_num][sess_num] != null) {
            int opt = tt_spinners[day_num][sess_num].getSelectedItemPosition() - 1;
            if (opt >= 0)     //not free
                return opt;
        } else {
            //get from table.
            for (int k = 0; k < data.length; ++k) {
                if (data[k].slots[day_num].contains(sess_num))
                    return k;
            }
        }
        return -1;
    }

    private void save_timetable_changes() {
        TransitionManager.beginDelayedTransition(list);
        if (active_box != -1) {
            list.removeViewAt(active_box + 1);
            active_box = -1;
        }

        editing = false;
        title.setText(getString(R.string.time_table_title));

        Explode explode = new Explode();
        explode.setDuration(400);
        TransitionManager.beginDelayedTransition((Toolbar) getActivity().findViewById(R.id.toolbar), explode);
        options_menu.findItem(R.id.save_timetable_button).setVisible(false);
        options_menu.findItem(R.id.cancel_timetable_button).setVisible(false);
        options_menu.findItem(R.id.edit_timetable_button).setVisible(true);

        Toast.makeText(getContext(), R.string.tt_edits_saved_toast, Toast.LENGTH_SHORT).show();

        for (int i = 0; i < tt_spinners.length; ++i) {
            for (int j = 0; j < tt_spinners[i].length; ++j) {
                if (tt_spinners[i][j] != null) {

                    //Remove the slot if it's already there
                    for (Subject k : data)
                        k.slots[i].remove(Integer.valueOf(j));

                    try {
                        Subject to_edit = data[tt_spinners[i][j].getSelectedItemPosition() - 1];
                        to_edit.slots[i].add(j);
                    } catch (IndexOutOfBoundsException e) {
                        //Free Slot
                    }
                }
            }
        }
        tt_spinners = null;
        spinner_options = null;
    }


}