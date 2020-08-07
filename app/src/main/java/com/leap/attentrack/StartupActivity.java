package com.leap.attentrack;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;
import androidx.transition.AutoTransition;
import androidx.transition.Explode;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

public class StartupActivity extends AppCompatActivity implements View.OnTouchListener {
    //for ease of usage
    LinearLayout root;
    private ImageView session_plus;
    private TextView delete_slide, back_button;

    //persistent through fn calls. (to read data)
    private EditText edit_attendance;
    private TextView semester_d1, semester_d2;
    private LinkedList<EditText> sub_edits = new LinkedList<>();
    private String[] subjects;    //for spinner adapter
    private LinkedList<Spinner>[] tt_spinners;
    private boolean[] is_box_active;

    //constants of sorts
    private int page = -1, duration = 400, lag = 400, mode, tab_margin;
    private int INDEX_SESSIONS, INDEX_SUBJECTS, INDEX_TIMETABLE, INDEX_ATTENDANCE, INDEX_END;
    private final Integer[] colors = {0xffffbe93, 0xffbbf6bf, 0xffabecff, 0xfffcb1fa, 0xff88acfd,
            0xfff7f7be, 0xff9affff, 0xffdcfaa3, 0xffffb9b9, 0xffa3fad2, 0xffb5dfff, 0xffe6c6ff};
    private float density;
    private int x_touch_origin, y_touch_origin, moveDirection = -1, sess_guided = 0, sub_guided = 0, deleteActive = 0;
    private boolean condensed = false;
    private View sliding_view;

    //required data
    String sem_start, sem_end;
    LinkedList<String[]> sessions = new LinkedList<>();
    Subject[] data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        //variables assignment
        density = getResources().getDisplayMetrics().density;
        tab_margin = (int) (10 * density);
        root = findViewById(R.id.startup_main_linear);
        TextView[] intro = new TextView[]{findViewById(R.id.startup_hello),
                findViewById(R.id.startup_hello2), findViewById(R.id.startup_hello3)};
        TextView okay = findViewById(R.id.okay_btn_startup);
        back_button = findViewById(R.id.back_btn_startup);
        delete_slide = findViewById(R.id.delete_slide);
        tt_spinners = new LinkedList[]{new LinkedList<Spinner>(), new LinkedList<Spinner>(), new LinkedList<Spinner>(),
                new LinkedList<Spinner>(), new LinkedList<Spinner>(), new LinkedList<Spinner>(), new LinkedList<Spinner>()};

        final View scroller = findViewById(R.id.startup_scrollview);
        scroller.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //ScrollView consumes touch events **from scrolling**. So intercept them here and pass on.
                //return true to stop scrolling
                boolean stopScroll = moveDirection == 1;    //saving moveDirection in case MotionEvent event is ACTION_UP
                if(sliding_view != null)
                    StartupActivity.this.onTouch(sliding_view, event);      //will change moveDirection
                return stopScroll;
            }
        });


        final View elevator = findViewById(R.id.elevation_view);
        scroller.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            private boolean elevated = false;

            @Override
            public void onScrollChanged() {
                if (scroller.canScrollVertically(-1)) {
                    if (!elevated) {
                        elevator.animate().translationZ(5 * density).setDuration(200);
                        elevated = true;
                    }
                } else if (elevated) {
                    elevator.animate().translationZ(0).setDuration(200);
                    elevated = false;
                }
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {       //status bar colour change
            getWindow().setStatusBarColor(getColor(R.color.startup_bg));
            //need some view to get systemui details
            root.setSystemUiVisibility(root.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        //disable layout appear animation. custom animation given
        LayoutTransition expand_transition = new LayoutTransition();
        expand_transition.disableTransitionType(LayoutTransition.APPEARING);
        expand_transition.setDuration(duration);
        root.setLayoutTransition(expand_transition);

        // Capture the width once it is laid out.
        delete_slide.post(new Runnable() {
            @Override
            public void run() {
                delete_slide.setVisibility(View.GONE);  //Initial Width is 0 if view starts as Gone. Need for swipe.
            }
        });

        mode = getIntent().getIntExtra("mode", 0);
        switch (mode) {
            case 0:    //First Start
                condensed = false;
                break;

            case 1:    //Subject Reset Mode
                condensed = true;
                page = 1;   //jump to subjects
                sessions = new LinkedList<>(Arrays.asList(Subject.session_encoder));
                okay_button_pressed(okay);
                return;     //no hello for menu reset

            case 2:     //Complete Reset Mode
                intro[1].setText(R.string.reset_mode_text);
                condensed = false;
                intro[0].setVisibility(View.GONE);
                intro[2].setVisibility(View.GONE);
                intro[1].animate().alpha(1f).setDuration(duration).setStartDelay(duration);
                okay.animate().alpha(1f).setDuration(duration).setStartDelay(2 * duration + lag);
                ++page;
                return;     //no hello for menu reset
        }

        //hello animation
        int animation_offset = 200;
        intro[0].animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset);
        intro[1].animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration + lag);
        intro[2].animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration + lag);

        //much longer animation than other cases. So, listener
        okay.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration + lag).
                setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }

                    @Override

                    public void onAnimationEnd(Animator animation) {
                        ++page;
                    }
                });
    }

    public void okay_button_pressed(View view) {

        switch (page) {
            case 0:     //sessions
            {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                root.removeViewsInLayout(0, 3);
                INDEX_SESSIONS = 0;
                TextView session_text = findViewById(R.id.startup_session_time);
                session_plus = findViewById(R.id.session_plus);
                int animation_offset = 0;
                session_text.setVisibility(View.VISIBLE);
                session_plus.setVisibility(View.VISIBLE);
                session_text.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration);
                session_plus.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration + lag);
                handle_session_input();
                ++page;
                findViewById(R.id.startup_progress).animate().
                        alpha(1f).
                        setDuration(400);
                updateProgressBar();
                break;
            }
            case 1:      //subjects
            {
                if (!condensed) {
                    boolean okay = sessions.size() != 0;
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    Date time1, time2 = null;

                    try {
                        //Selection Sort
                        for (int i = 0; i < sessions.size() - 1; ++i) {
                            int small = i;
                            time1 = sdf.parse(sessions.get(i)[0]);  //get start time
                            for (int j = i + 1; j < sessions.size(); ++j) {
                                time2 = sdf.parse(sessions.get(j)[0]);
                                if (time2.before(time1)) {
                                    time1 = time2;
                                    small = j;
                                }
                            }
                            if (small != i) {
//                                TransitionManager.beginDelayedTransition(root);
                                Collections.swap(sessions, i, small);
                                View temp1 = root.getChildAt(INDEX_SESSIONS + 1 + i),
                                        temp2 = root.getChildAt(INDEX_SESSIONS + small + 1);
                                root.removeView(temp1);
                                root.removeView(temp2);
                                root.addView(temp2, INDEX_SESSIONS + 1 + i);
                                root.addView(temp1, INDEX_SESSIONS + small + 1);
                            }
                        }

                        for (int i = 0; i < sessions.size(); ++i) {
                            time1 = sdf.parse(sessions.get(i)[0]);

                            if (i > 0 && time2.after(time1)) {       //previous time2 [overlap check]
                                okay = false;
                                break;
                            }

                            time2 = sdf.parse(sessions.get(i)[1]);

                            if (!time2.after(time1)) {
                                okay = false;
                                break;
                            }
                        }
                    } catch (ParseException | NullPointerException e) {
                        //ParseException from sdf.parse()
                        //NullPointerException if any of the times are null
                        okay = false;
                    }

                    if (!okay) {
                        Toast.makeText(this, R.string.invalid_sessions_toast, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    INDEX_SUBJECTS = root.indexOfChild(root.findViewById(R.id.startup_subjects));
                    for (int i = INDEX_SESSIONS; i < INDEX_SUBJECTS; ++i)
                        root.getChildAt(i).setVisibility(View.GONE);

                    back_button.setVisibility(View.VISIBLE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    root.removeViewsInLayout(0, 6);
                    INDEX_SUBJECTS = 0;
                }


                if (sub_edits.size() == 0) {     //first coming
                    final TextView subject_text = findViewById(R.id.startup_subjects);
                    if (condensed)
                        subject_text.setText(R.string.condensed_subject_title);
                    final ImageView subject_plus = findViewById(R.id.subject_plus);
                    subject_text.setVisibility(View.VISIBLE);
                    subject_plus.setVisibility(View.VISIBLE);
                    int animation_offset = 0;
                    subject_text.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration);
                    subject_plus.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration + lag);
                    if (condensed)
                        findViewById(R.id.okay_btn_startup).animate().alpha(1f).setDuration(duration).
                                setStartDelay(animation_offset += duration);

                    /*
                    final int[] index = new int[]{INDEX_SUBJECTS};
                    cannot use index variable because a change in session will change INDEX_SUBJECT but that
                    change will not be reflected in index. so, instead using indexOfChild(session_plus)
                     */
                    final LinkedList<Integer> colors_subjects = new LinkedList<>(Arrays.asList(colors));
                    Collections.shuffle(colors_subjects);

                    final View.OnTouchListener swipe_listener = new View.OnTouchListener() {
                        private float startX;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    startX = event.getRawX();
                                    break;

                                case MotionEvent.ACTION_UP:
                                    if (event.getRawX() == startX) {
                                        v.requestFocus();
                                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                        imm.showSoftInput(v, 0);
                                    }
                                    break;
                            }
                            StartupActivity.this.onTouch(v, event);
                            return true;
                        }
                    };

                    subject_plus.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AppCompatEditText e = new AppCompatEditText(StartupActivity.this);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    (int) (40 * density));
                            lp.setMargins(tab_margin, tab_margin, tab_margin, 0);
                            e.setLayoutParams(lp);
                            e.setSingleLine(true);
                            e.setGravity(Gravity.CENTER);
                            e.setBackgroundResource(R.drawable.curve_10dp);
                            e.setBackgroundTintList(ColorStateList.valueOf(colors_subjects.pop()));
                            if (colors_subjects.size() == 0) {
                                colors_subjects.addAll(Arrays.asList(colors));
                                Collections.shuffle(colors_subjects);
                            }
                            e.setHint(R.string.demo_subject_name);
                            e.setPadding((int) (10 * density), 0, (int) (10 * density), 0);
                            e.setOnTouchListener(swipe_listener);
                            e.requestFocus();

                            if (sub_guided == 0) {
                                View guide = root.getChildAt(INDEX_SUBJECTS + 1);
                                guide.setVisibility(View.VISIBLE);
                                guide.animate().alpha(1f).setStartDelay(duration);
                                sub_guided = 1;
                                root.addView(e, INDEX_SUBJECTS + 1);
                            } else {
                                if (sub_guided == 1) {
                                    root.removeViewAt(root.indexOfChild(v) - 1);
                                    sub_guided = 2;
                                }
                                root.addView(e, root.indexOfChild(v));
                            }
                            sub_edits.add(e);
                        }
                    });
                    if (!condensed)
                        back_button.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration);

                } else {    // blast from the past

                    TransitionManager.beginDelayedTransition(root);
                    for (int i = INDEX_SUBJECTS; i < root.indexOfChild(root.findViewById(R.id.startup_time_table)); ++i)
                        root.getChildAt(i).setVisibility(View.VISIBLE);
                }
                if (condensed)
                    findViewById(R.id.startup_progress).animate().alpha(1f).setDuration(400);
                ++page;
                updateProgressBar();
                break;
            }
            case 2:     //timetable
            {
                hideKeyboard();
                boolean okay = sub_edits.size() != 0;
                subjects = new String[sub_edits.size()];
                int[] sub_colors = new int[sub_edits.size()];
                for (int i = 0; i < sub_edits.size(); ++i) {
                    String text = sub_edits.get(i).getText().toString();
                    if ("".equals(text)) {
                        okay = false;
                        break;
                    }
                    subjects[i] = text;
                    sub_colors[i] = sub_edits.get(i).getBackgroundTintList().getDefaultColor();
                }
                if (!okay) {
                    Toast.makeText(this, R.string.invalid_subjects_toast, Toast.LENGTH_SHORT).show();
                    return;
                }

                data = new Subject[subjects.length];
                for (int i = 0; i < data.length; ++i) {
                    Subject sub = new Subject();
                    sub.name = subjects[i];
                    sub.color = sub_colors[i];
                    data[i] = sub;
                }

                INDEX_TIMETABLE = root.indexOfChild(root.findViewById(R.id.startup_time_table));
                for (int i = INDEX_SUBJECTS; i < INDEX_TIMETABLE; ++i)
                    root.getChildAt(i).setVisibility(View.GONE);

                while (tt_spinners[0].size() < sessions.size())  //to prevent extra sessions caused paradox [blast from the past]
                    for (int i = 0; i < 7; ++i)
                        tt_spinners[i].add(null);

                if (is_box_active == null) {      //first coming
                    final TextView[] week_views = new TextView[]{
                            findViewById(R.id.box_1),
                            findViewById(R.id.box_2),
                            findViewById(R.id.box_3),
                            findViewById(R.id.box_4),
                            findViewById(R.id.box_5),
                            findViewById(R.id.box_6),
                            findViewById(R.id.box_7)};

                    is_box_active = new boolean[7]; //def false

                    TextView timetable_text = findViewById(R.id.startup_time_table);
                    timetable_text.setVisibility(View.VISIBLE);
                    timetable_text.animate().alpha(1f).setDuration(duration).setStartDelay(duration);

                    for (int i = 0; i < 7; ++i) {
                        week_views[i].setVisibility(View.VISIBLE);
                        week_views[i].animate().alpha(1f).setDuration(duration).setStartDelay(2 * duration);
                        final int k = i;
                        week_views[i].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                week_clicked(k, is_box_active, week_views[k].getBackgroundTintList().getDefaultColor());
                            }
                        });
                    }
                    if (condensed) {
                        back_button.setVisibility(View.VISIBLE);
                        back_button.animate().alpha(1f).setDuration(duration).setStartDelay(3 * duration);
                    }
                } else {
                    TransitionManager.beginDelayedTransition(root);
                    for (int i = INDEX_TIMETABLE; i < root.indexOfChild(findViewById(R.id.startup_almost_there)); ++i)
                        root.getChildAt(i).setVisibility(View.VISIBLE);
                    if (condensed)
                        back_button.setVisibility(View.VISIBLE);
                }

                ++page;
                updateProgressBar();
                break;
            }
            case 3:     //Attendance percentage
            {
                TextView almost_there = findViewById(R.id.startup_almost_there);
                LinearLayout attendance_layout = findViewById(R.id.attendance_linear_layout);   //view after timetable
                edit_attendance = findViewById(R.id.editText_attendance);
                edit_attendance.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //To remove error warning red
                        v.setBackgroundTintList(ColorStateList.valueOf(0xffffffff));
                    }
                });

//                INDEX_ATTENDANCE = root.indexOfChild(attendance_layout);
                INDEX_ATTENDANCE = root.indexOfChild(almost_there);
                INDEX_END = root.indexOfChild(findViewById(R.id.buttons_layout)) + 1;

                for (int i = INDEX_TIMETABLE; i < INDEX_ATTENDANCE; ++i)
                    root.getChildAt(i).setVisibility(View.GONE);

                for (int day = 0; day < 7; ++day) {
                    for (Subject s : data)
                        s.slots[day] = new LinkedList<>();
                    for (int sess = 0; sess < sessions.size(); ++sess) {
                        try {
                            int ind = tt_spinners[day].get(sess).getSelectedItemPosition() - 1;   //    <free>
                            // above line throws null pointer exception
                            if (ind >= 0)
                                data[ind].slots[day].add(sess);
                        } catch (NullPointerException e) {
                            //Completely Free Day(Never Opened Day)
                        }
                    }
                }

                ++page;
                updateProgressBar();
                if (!condensed) {
                    almost_there.setVisibility(View.VISIBLE);
                    attendance_layout.setVisibility(View.VISIBLE);
                    almost_there.animate().alpha(1f).setDuration(duration).setStartDelay(duration);
                    attendance_layout.animate().alpha(1f).setDuration(duration).setStartDelay(2 * duration + lag);
                    break;
                }
            }
            case 4:     //Semester Start/End
            {   //falls through if condensed
                hideKeyboard();
                TextView date_text = findViewById(R.id.startup_working),
                        date_hyphen = findViewById(R.id.semester_hyphen),
                        almost_there = findViewById(R.id.startup_almost_there);
                semester_d1 = findViewById(R.id.semester_start_date);
                semester_d2 = findViewById(R.id.semester_end_date);

                date_text.setVisibility(View.VISIBLE);
                semester_d1.setVisibility(View.VISIBLE);
                date_hyphen.setVisibility(View.VISIBLE);
                semester_d2.setVisibility(View.VISIBLE);

                if (date_hyphen.getAlpha() == 0) {     //first coming
                    int animation_offset = 0;
                    date_text.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration);
                    semester_d1.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset += duration);
                    date_hyphen.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset);
                    semester_d2.animate().alpha(1f).setDuration(duration).setStartDelay(animation_offset);
                    almost_there.setText(R.string.almost_there_text_2);
                    almost_there.setTextColor(getResources().getColor(R.color.colorAccent));

                    final Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

                    semester_d1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.startup_bg)));
                            new DatePickerDialog(StartupActivity.this,
                                    new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                            ++month;   //starts from 0
                                            String date = (dayOfMonth < 10 ? "0" : "") + dayOfMonth + "/" +
                                                    (month < 10 ? "0" : "") + month + "/" + year;
                                            ((TextView) v).setText(date);
                                        }
                                    }, calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)).
                                    show();
                        }
                    });

                    semester_d2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.startup_bg)));
                            new DatePickerDialog(StartupActivity.this,
                                    new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                            ++month;   //starts from 0
                                            String date = (dayOfMonth < 10 ? "0" : "") + dayOfMonth + "/" +
                                                    (month < 10 ? "0" : "") + month + "/" + year;
                                            ((TextView) v).setText(date);
                                        }
                                    }, calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)).
                                    show();
                        }
                    });
                }
                ++page;
                updateProgressBar();
                break;
            }
            case 5:     //Startup Complete
            {
                if (!condensed) {
                    String percent = edit_attendance.getText().toString();
                    if ("".equals(percent) || Integer.parseInt(edit_attendance.getText().toString()) > 100) {
                        edit_attendance.setBackgroundTintList(ColorStateList.valueOf(0xffff8080));
                        Toast.makeText(this, R.string.invalid_percentage_toast, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Subject.req_percentage = Integer.parseInt(percent);
                }

                String date_string_1 = semester_d1.getText().toString(), date_string_2 = semester_d2.getText().toString();
                if ("--/--/----".equals(date_string_1)) {
                    semester_d1.setBackgroundTintList(ColorStateList.valueOf(0xffff8080));
                    Toast.makeText(this, R.string.start_date_toast, Toast.LENGTH_SHORT).show();
                    return;
                }
                if ("--/--/----".equals(date_string_2)) {
                    semester_d2.setBackgroundTintList(ColorStateList.valueOf(0xffff8080));
                    Toast.makeText(this, R.string.end_date_toast, Toast.LENGTH_SHORT).show();
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Date d1, d2;
                try {
                    d1 = sdf.parse(date_string_1);
                    d2 = sdf.parse(date_string_2);
                } catch (ParseException e) {
                    return;
                }

                if (!d2.after(d1)) {
                    Toast.makeText(this, R.string.invalid_dates_toast, Toast.LENGTH_SHORT).show();
                    return;
                }

                hideKeyboard();
                for (int i = INDEX_ATTENDANCE; i < INDEX_END; ++i)
                    root.getChildAt(i).setVisibility(View.GONE);

                sem_start = semester_d1.getText().toString();
                sem_end = semester_d2.getText().toString();

                long diff = (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24);
                int number_of_weeks = (int) (diff / 7);

                int[] subject_distr = new int[data.length];
                for (int i = 0; i < data.length; ++i) {
                    for (LinkedList<Integer> day : data[i].slots)
                        subject_distr[i] += day.size();
                    subject_distr[i] *= number_of_weeks;
                }

                Calendar c = Calendar.getInstance();
                c.setTime(d1);
                int start_day = c.get(Calendar.DAY_OF_WEEK) - 1;    //sun-sat 0 -6
                start_day = start_day == 0 ? 6 : start_day - 1;     //mon-sun
                c.setTime(d2);
                int end_day = c.get(Calendar.DAY_OF_WEEK) - 1;
                end_day = end_day == 0 ? 6 : end_day - 1;     //mon-sun

                while (start_day != end_day) {  //should run diff% 7 times. To account for the rest of the days.
                    for (int i = 0; i < data.length; ++i)
                        subject_distr[i] += data[i].slots[start_day].size();
                    start_day = start_day == 6 ? 0 : start_day + 1; //cycle along
                }

                for (int i = 0; i < data.length; ++i) {
                    data[i].total = subject_distr[i];
                    data[i].missable = (100 - Subject.req_percentage) * data[i].total / 100;
                }

                ++page;
                updateProgressBar();

                TextView loading_text = findViewById(R.id.startup_loading);
                ProgressBar p = findViewById(R.id.loading_progress);
                loading_text.setVisibility(View.VISIBLE);
                p.setVisibility(View.VISIBLE);
                loading_text.animate().alpha(1f).setDuration(duration).setStartDelay(duration);
                p.animate().alpha(1f).setDuration(duration).setStartDelay(duration + lag);
                save_data();
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent returnIntent = new Intent();
                        setResult(29, returnIntent);
                        finish();
                    }
                }, 2000);
                break;
            }
        }
    }

    public void back_button_pressed(View view) {
        Transition explode = new Explode();
        explode.setDuration(duration);
        TransitionManager.beginDelayedTransition(root, explode);
        switch (page) {
            case 2:     //not condensed. Back pressed from subjects page. To sessions page
                hideKeyboard();
                for (int i = INDEX_SUBJECTS; i < root.indexOfChild(findViewById(R.id.startup_time_table)); ++i)
                    root.getChildAt(i).setVisibility(View.GONE);
                for (int i = INDEX_SESSIONS; i < INDEX_SUBJECTS; ++i)
                    root.getChildAt(i).setVisibility(View.VISIBLE);
                --page;
                updateProgressBar();
                back_button.setVisibility(View.GONE);
                break;

            case 3:     // from time table page. to subjects page
                for (int i = INDEX_TIMETABLE; i < root.indexOfChild(findViewById(R.id.attendance_linear_layout)); ++i)
                    root.getChildAt(i).setVisibility(View.GONE);
                for (int i = INDEX_SUBJECTS; i < INDEX_TIMETABLE; ++i)
                    root.getChildAt(i).setVisibility(View.VISIBLE);
                --page;
                updateProgressBar();
                if (condensed)
                    back_button.setVisibility(View.GONE);
                else {   //session edits
                    for (int i = 0; i < is_box_active.length; ++i) {
                        if (is_box_active[i]) {
                            ((GridLayout) (root.getChildAt(INDEX_TIMETABLE + i + 2))).removeAllViews();
                            // to separate parent
                            root.removeViewAt(INDEX_TIMETABLE + i + 2);
                            is_box_active[i] = false;
                        }
                    }
                }
                break;

            case 4:     //from attendance state 1. to time table page. not condensed
                hideKeyboard();
                root.getChildAt(INDEX_ATTENDANCE).setVisibility(View.GONE);
                root.getChildAt(INDEX_ATTENDANCE + 1).setVisibility(View.GONE);
                for (int i = INDEX_TIMETABLE; i < INDEX_ATTENDANCE; ++i)
                    root.getChildAt(i).setVisibility(View.VISIBLE);
                --page;
                updateProgressBar();
                break;

            case 5:     //from attendance state2. to time table page
                for (int i = INDEX_ATTENDANCE; i < INDEX_END - 1; ++i)
                    root.getChildAt(i).setVisibility(View.GONE);
                for (int i = INDEX_TIMETABLE; i < INDEX_ATTENDANCE; ++i)
                    root.getChildAt(i).setVisibility(View.VISIBLE);
                page -= 2;
                updateProgressBar();
                break;

        }
    }

    public void handle_session_input() {
        final int[] active_sess_index = new int[2], set_time = new int[]{8, 0};
        final TextView[] active_view = new TextView[1];
        final LinkedList<Integer> colors_sessions = new LinkedList<>(Arrays.asList(colors));
        Collections.shuffle(colors_sessions);

        final TimePickerDialog.OnTimeSetListener time_listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String time = (hourOfDay < 10 ? "0" : "") + hourOfDay + ":" + (minute < 9 ? "0" : "") + minute;
                sessions.get(active_sess_index[0])[active_sess_index[1]] = time;
                active_view[0].setText(time);
                set_time[0] = hourOfDay;
                set_time[1] = minute;
            }
        };

        session_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View[] time_view = create_session_view(colors_sessions.pop());

                time_view[0].setOnTouchListener(StartupActivity.this);

                time_view[1].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        active_sess_index[0] = root.indexOfChild(time_view[0]) - INDEX_SESSIONS - 1;
                        active_sess_index[1] = 0;
                        active_view[0] = (TextView) v;
                        new TimePickerDialog(StartupActivity.this, time_listener, set_time[0], set_time[1], false).show();
                    }
                });

                time_view[2].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        active_sess_index[0] = root.indexOfChild(time_view[0]) - INDEX_SESSIONS - 1;
                        active_sess_index[1] = 1;
                        active_view[0] = (TextView) v;
                        new TimePickerDialog(StartupActivity.this, time_listener, set_time[0], set_time[1], false).show();
                    }
                });

                if (colors_sessions.size() == 0) {
                    colors_sessions.addAll(Arrays.asList(colors));
                    Collections.shuffle(colors_sessions);
                }

                if (sess_guided == 0) {
                    View guide = root.getChildAt(INDEX_SESSIONS + 1);
                    guide.setVisibility(View.VISIBLE);
                    guide.animate().alpha(1f).setStartDelay(duration);
                    sess_guided = 1;
                    root.addView(time_view[0], INDEX_SESSIONS + 1);
                } else {
                    if (sess_guided == 1) {
                        root.removeViewAt(root.indexOfChild(v) - 1);
                        sess_guided = 2;
                    }
                    root.addView(time_view[0], root.indexOfChild(v));
                }
                sessions.add(new String[2]);
            }
        });
    }

    private View[] create_session_view(int bg_color) {
        final LinearLayout layout = new LinearLayout(this);
        layout.setBackgroundResource(R.drawable.curve_10dp);
        layout.setBackgroundTintList(ColorStateList.valueOf(bg_color));
        layout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (40 * density));
        lp.setMargins(tab_margin, tab_margin, tab_margin, 0);
        layout.setLayoutParams(lp);

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f);
        AppCompatTextView[] tvs = new AppCompatTextView[3];

        for (int i = 0; i < 3; ++i) {
            tvs[i] = new AppCompatTextView(this);
            //tvs[i].setTypeface(ResourcesCompat.getFont(this, R.font.poppins_medium));
            tvs[i].setPadding(0, (int) (2 * density), 0, (int) (2 * density));
            TextViewCompat.setAutoSizeTextTypeWithDefaults(tvs[i], TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            tvs[i].setTextColor(0xff272727);
        }

        View.OnTouchListener swipe_listener = new View.OnTouchListener() {
            private float startX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        break;

                    case MotionEvent.ACTION_UP:
//                        if(Math.abs(event.getRawX() - startDist) < 20)
                        if (event.getRawX() == startX)
                            v.performClick();
                        break;
                }
                StartupActivity.this.onTouch(layout, event);
                return true;
            }
        };

        tvs[0].setOnTouchListener(swipe_listener);
        tvs[2].setOnTouchListener(swipe_listener);
        tvs[1].setLayoutParams(lp2);
        tvs[1].setText("-");
        tvs[1].setGravity(Gravity.CENTER);
        tvs[0].setLayoutParams(lp3);
        tvs[2].setLayoutParams(lp3);
        tvs[0].setText(R.string.session_time_start);
        tvs[2].setText(R.string.session_time_end);
        tvs[0].setGravity(Gravity.END);
        tvs[2].setGravity(Gravity.START);

        layout.addView(tvs[0]);
        layout.addView(tvs[1]);
        layout.addView(tvs[2]);
        return new View[]{layout, tvs[0], tvs[2]};
    }

    private void week_clicked(int boxno, boolean[] active_boxes, int box_color) {
        int add_index = INDEX_TIMETABLE + boxno + 2;
        for (int i = 0; i < boxno; ++i)
            if (active_boxes[i])
                ++add_index;

        if (active_boxes[boxno]) {
            ((GridLayout) (root.getChildAt(add_index))).removeAllViews();
            root.removeViewAt(add_index);
            active_boxes[boxno] = false;
            return;
        }

        GridLayout grid = new GridLayout(this);
        grid.setBackgroundResource(R.drawable.curve_bottom_only_10dp);
        LinearLayout.LayoutParams gridparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridparams.setMargins((int) (density * 20), 0, (int) (density * 20), 0);
        grid.setLayoutParams(gridparams);
        grid.setPadding((int) (density * 10), 0, 0, (int) (density * 2));
        int gridwidth = root.getWidth() - (int) (density * 50),
                color = 0x60ffffff + box_color + 1;

        grid.setBackgroundTintList(ColorStateList.valueOf(color));
        root.addView(grid, add_index);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(StartupActivity.this, android.R.layout.simple_spinner_item);
        adapter.add(getString(R.string.free_session_text));
        adapter.addAll(subjects);

        for (int i = 0; i < sessions.size(); ++i) {
            grid.addView(createTextViewForGrid(i, 0, sessions.get(i)[0], gridwidth / 5, Gravity.END));
            grid.addView(createTextViewForGrid(i, 1, "-", gridwidth / 15, Gravity.CENTER_HORIZONTAL));
            grid.addView(createTextViewForGrid(i, 2, sessions.get(i)[1], gridwidth / 5, Gravity.START));

            GridLayout.LayoutParams grid_pos_lparam = new GridLayout.LayoutParams(GridLayout.spec(i), GridLayout.spec(3));
            grid_pos_lparam.height = (int) (30 * density);
            grid_pos_lparam.width = 8 * gridwidth / 15;
            grid_pos_lparam.setGravity(Gravity.BOTTOM);
            grid_pos_lparam.setMargins(0, (int) (5 * density), 0, 0);

            if (tt_spinners[boxno].get(i) == null) {
                Spinner s = new Spinner(this);
                s.setLayoutParams(grid_pos_lparam);
                s.setGravity(Gravity.END);
                s.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                s.setAdapter(adapter);
                tt_spinners[boxno].set(i, s);
            } else {    //reopen box
                int sel = tt_spinners[boxno].get(i).getSelectedItemPosition();
                tt_spinners[boxno].get(i).setAdapter(adapter);
                tt_spinners[boxno].get(i).setSelection(sel);
                tt_spinners[boxno].get(i).setLayoutParams(grid_pos_lparam);     //for session delete alone.
            }
            grid.addView(tt_spinners[boxno].get(i));
            active_boxes[boxno] = true;
        }
    }

    private TextView createTextViewForGrid(int row, int column, String text, int width, int gravity) {
        GridLayout.LayoutParams lparams = new GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column));
        lparams.width = width;
        lparams.height = (int) (30 * density);
        lparams.setMargins(0, (int) (5 * density), 0, 0);
        AppCompatTextView textView = new AppCompatTextView(this);
        textView.setText(text);
        textView.setTextColor(0xff272727);
        textView.setLayoutParams(lparams);
        textView.setGravity(gravity);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(textView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        return textView;
    }

    @Override
    public boolean onTouch(final View v, MotionEvent event) {
        int action = event.getActionMasked();// & MotionEvent.ACTION_MASK;
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                x_touch_origin = x;
                y_touch_origin = y;
                sliding_view = v;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int dist_moved_x = x - x_touch_origin;

                if (moveDirection == -1) {
                    int trigger_threshold = (int) (2 * density);
                    int dist_moved_y = y - y_touch_origin;
                    if (Math.abs(dist_moved_x) > Math.abs(dist_moved_y) && Math.abs(dist_moved_x) > trigger_threshold)
                        moveDirection = 1;
                    else if (Math.abs(dist_moved_y) > trigger_threshold)
                        moveDirection = 0;
                }

                if (moveDirection == 1) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
                    lp.leftMargin = tab_margin + dist_moved_x;
                    lp.rightMargin = tab_margin - dist_moved_x;
                    v.setLayoutParams(lp);

                    if (dist_moved_x < 0 && deleteActive != -1) {    //Left Movement
                        delete_slide.setVisibility(View.VISIBLE);
                        delete_slide.setX(35 * density + v.getWidth() - delete_slide.getWidth());
                        int[] location = new int[2];
                        v.getLocationOnScreen(location);
                        delete_slide.setY(location[1] - v.getHeight() / 2f);
                        deleteActive = -1;

                    } else if (dist_moved_x > 0 && deleteActive != 1) {    //Right Movement
                        delete_slide.setVisibility(View.VISIBLE);
                        delete_slide.setX(35 * density);
                        int[] location = new int[2];
                        v.getLocationOnScreen(location);
                        delete_slide.setY(location[1] - v.getHeight() / 2f);
                        deleteActive = 1;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (moveDirection == 1) {
                    float threshold_width = 0.42f * v.getWidth();
                    if (Math.abs(x_touch_origin - x) > threshold_width) {
                        float to_move = v.getX();
                        if (to_move < 0)
                            to_move = -(v.getWidth() + to_move);
                        v.animate().translationX(to_move).setDuration(200);

                        AlphaAnimation alpha = new AlphaAnimation(1f, 0f);  //to avoid permanent animations
                        alpha.setDuration(250);
                        delete_slide.startAnimation(alpha);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int ind = root.indexOfChild(v);
                                root.removeViewAt(ind);
                                if (page == 1) {
                                    int sess_index = ind - INDEX_SESSIONS - 1;
                                    sessions.remove(sess_index);
                                    for (int i = 0; i < 7; ++i) {
                                        if (tt_spinners[i].size() != 0)
                                            tt_spinners[i].remove(sess_index);
                                    }

                                } else if (page == 2) {
                                    int sub_index = ind - INDEX_SUBJECTS - 1;
                                    sub_edits.remove(sub_index);
                                    //Check if sub is selected anywhere
                                    for (LinkedList<Spinner> tt_spin : tt_spinners) {
                                        for (Spinner s : tt_spin) {
                                            if (s != null && s.getSelectedItemPosition() - 1 == sub_index)
                                                s.setSelection(0);
                                        }
                                    }
                                }

                                delete_slide.setVisibility(View.GONE);
                            }
                        }, 250);

                    } else {
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
                        Transition auto = new AutoTransition();
                        auto.setDuration(100);
                        TransitionManager.beginDelayedTransition(root, auto);
                        lp.leftMargin = tab_margin;
                        lp.rightMargin = tab_margin;
                        v.setLayoutParams(lp);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                delete_slide.setVisibility(View.GONE);
                            }
                        }, 100);
                    }
                    deleteActive = 0;
                }

                sliding_view = null;
                moveDirection = -1;
                break;
            }
        }
        return true;
    }

    void save_data() {
        SharedPreferences.Editor sp_editor = getSharedPreferences(MainActivity.shared_pref_name, MODE_PRIVATE).edit();

        if (mode == 0) {    // first start
            sp_editor.putInt("first_start", 0);
            MainActivity.is_notification_on = true;
            MainActivity.is_first_start = 0;    //put data has check for first start
        }

        sp_editor.putString("sem_start_date", sem_start);
        sp_editor.putString("sem_end_date", sem_end);
        sp_editor.apply();

        //MainActivity will be recreated on result of this activity, thus calling onPause() and hence put_data().
        //So, just save the values in MainActivity and it will be written to the file.
        Subject.session_encoder = new String[sessions.size()][];
        for (int i = 0; i < sessions.size(); ++i)
            Subject.session_encoder[i] = sessions.get(i);

        MainActivity.data = data;
    }

    @Override
    public void onBackPressed() {
    }

    public void updateProgressBar() {
        int progress = (page * 100) / 6;
        if (condensed) {
            if (page == 2)
                progress = 25;
            else if (page == 3)
                progress = 50;
            else if (page == 5)
                progress = 75;
            else if (page == 6)
                progress = 100;
        }
        ProgressBar progress_bar = findViewById(R.id.startup_progress);
        ObjectAnimator.ofInt(progress_bar, "progress", progress)
                .setDuration(500)
                .start();
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }
}
