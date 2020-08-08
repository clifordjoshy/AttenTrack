package com.leap.attentrack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;

import static android.content.Context.MODE_PRIVATE;

public class ScheduleList {

    private class ExtraViewHolder {
        private TextView info, cancel, percent;
        boolean isOpened;

        ExtraViewHolder(@NonNull View itemView) {
            info = itemView.findViewById(R.id.data_text);
            cancel = itemView.findViewById(R.id.cancel_button);
            percent = itemView.findViewById(R.id.percent_text);
            isOpened = false;
        }

        void toggleVisibility() {
            toggleVisibility(!isOpened);
        }

        void toggleVisibility(boolean isVisible) {
            isOpened = isVisible;
            int view_visibility = isVisible ? View.VISIBLE : View.GONE;
            info.setVisibility(view_visibility);
            cancel.setVisibility(view_visibility);
        }

        void updateInfo(int total, int missed, int missable) {
            String extra_text = context.getString(R.string.total_text) + total + "    " +
                    context.getString(R.string.missed_text) + missed + "    " +
                    context.getString(R.string.missable_text) + missable;
            info.setText(extra_text);
        }

        void updatePercent(int percent_value) {
            percent.setText((percent_value + "%"));
        }
    }

    private Context context;
    private LinearLayout main_list, cancel_tab;
    private Subject[] data;

    private LinkedList<ExtraViewHolder> card_details;
    private LinkedList<Subject> today_subjects;
    private LinkedList<Integer> today_sessions;

    private Handler cancel_remover = null;
    private int[] today, cancel_waiting = null;
    private int add_class_warned = -1;

    ScheduleList(Context ct, View root_fragment) {
        context = ct;
        main_list = root_fragment.findViewById(R.id.schedule_linear_layout);
        data = MainActivity.data;

        cancel_tab = root_fragment.findViewById(R.id.cancelled_undo_bar);

        String[] date_string = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()).split("-");
        today = new int[]{Integer.parseInt(date_string[0]), Integer.parseInt(date_string[1]), Integer.parseInt(date_string[2])};

        handle_data();    //fills up details, today_sess, all_subs

        if (MainActivity.is_first_start == 0) {
            //Add demo subject
            Subject demo = new Subject();
            demo.name = context.getString(R.string.demo_subject_name);
            demo.attendance = 96;
            demo.total = 56;
            demo.missed = 2;
            demo.missable = (100 - Subject.req_percentage) * demo.total / 100;
            demo.color = 0xffffa5c0;

            //demo subject
            today_subjects.addFirst(demo);
            today_sessions.addFirst(0);

            main_list.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handle_first_start(1);  //group1
                }
            }, 1000);
        }

        fillList();

        if (MainActivity.is_first_start == 0) {
            //custom cancel listener for demo subject
            card_details.getFirst().cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TransitionManager.beginDelayedTransition(main_list);
                    today_subjects.getFirst().cancel_session();
                    deleteItemAt(0);
                }
            });

            main_list.getChildAt(0).findViewById(R.id.minus_image).
                    setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            today_subjects.getFirst().missed_session();
                            updateListForSubject(today_subjects.getFirst());
                        }
                    });
        }
    }

    private void fillList() {
        card_details = new LinkedList<>();

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < today_subjects.size(); ++i)
            makeSubjectView(inflater, i);

    }

    private void makeSubjectView(LayoutInflater inflater, int index) {
        Subject sub = today_subjects.get(index);
        View element = inflater.inflate(R.layout.element_schedule, main_list, false);
        ExtraViewHolder element_holder = new ExtraViewHolder(element);

        ((TextView) element.findViewById(R.id.subject_text)).setText(sub.name);
        ((TextView) element.findViewById(R.id.time_text)).setText(Subject.session_encoder[today_sessions.get(index)][0]);
        element_holder.updatePercent(sub.attendance);
        element_holder.updateInfo(sub.total, sub.missed, sub.missable);

        //backgroundtint doesnt seem to work for api21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            element.getBackground().setColorFilter(sub.color, PorterDuff.Mode.MULTIPLY);
        else
            element.setBackgroundTintList(ColorStateList.valueOf(sub.color));

        addEventListeners(element, element_holder);
        card_details.add(index, element_holder);
        main_list.addView(element, index);
    }

    private void addEventListeners(final View element, ExtraViewHolder holder) {
        element.findViewById(R.id.card_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Transition tr = new AutoTransition();
                tr.setDuration(150);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    TransitionManager.beginDelayedTransition(main_list, tr);

                card_details.get(main_list.indexOfChild(element)).toggleVisibility();

                //transition after for api 21 compatibility
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    TransitionManager.beginDelayedTransition(main_list, tr);
            }
        });

        element.findViewById(R.id.minus_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = main_list.indexOfChild(element);
                int missed_sess = today_sessions.get(position),
                        missed_sub = getSubjectIndex(today_subjects.get(position));
                for (int[] sess : ((MainActivity) context).missed_sessions) {
                    if (sess[3] == missed_sess && sess[4] == missed_sub) {
                        Toast.makeText(context, R.string.missed_class_toast, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                int[] add_missed = new int[]{today[0], today[1], today[2], missed_sess, missed_sub};
                ((MainActivity) context).missed_sessions.add(add_missed);
                today_subjects.get(position).missed_session();
                updateListForSubject(today_subjects.get(position));
            }
        });

        holder.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = main_list.indexOfChild(element);

                boolean in_missed = false;
                int count = 0;
                for (int[] sess : ((MainActivity) context).missed_sessions) {
                    if (sess[3] == today_sessions.get(position) &&
                            sess[4] == getSubjectIndex(today_subjects.get(position))) {
                        in_missed = true;
                        break;
                    }
                    ++count;
                }

                if (in_missed) {
                    final int missed_sess_index = count;
                    AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.ThemedAlertDialog);
                    dialog.setTitle(context.getString(R.string.absence_dialog_title));
                    dialog.setMessage(context.getString(R.string.absence_dialog_message));
                    dialog.setPositiveButton(context.getString(R.string.undo_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int[] m_sess = ((MainActivity) context).missed_sessions.remove(missed_sess_index);
                            data[m_sess[3]].unmiss_session();
                            updateListForSubject(data[m_sess[3]]);
                        }
                    });
                    dialog.setNegativeButton(context.getString(R.string.ignore_text), null);
                    dialog.show();
                } else {
                    //no undoing here. since the cancel tab will not be seen because of dialog.
                    showCancelTab(today_subjects.get(position).name);

                    cancel_waiting = new int[]{today_sessions.get(position),
                            getSubjectIndex(today_subjects.get(position))};
                }
                TransitionManager.beginDelayedTransition(main_list);
                cancelClass(position);
            }
        });
    }

    private void showCancelTab(String subject_name) {
        final int display_height = context.getResources().getDisplayMetrics().heightPixels;

        String message = subject_name + " " + context.getString(R.string.cancelled_undo_text);
        ((TextView) cancel_tab.findViewById(R.id.cancelled_message))
                .setText(message);

        cancel_tab.setY(display_height);
        cancel_tab.setVisibility(View.VISIBLE);
        cancel_tab.animate().translationY(0).setDuration(500);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            cancel_tab.getBackground().setColorFilter(
                    MainActivity.dark_mode_on ? 0xff272727 : 0xffffffff, PorterDuff.Mode.MULTIPLY);

        if (cancel_remover != null)
            cancel_remover.removeCallbacksAndMessages(null);   //cancel old runnable
        cancel_remover = new Handler();

        cancel_tab.findViewById(R.id.undo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                addExtraClass(cancel_waiting[0], cancel_waiting[1]);
                cancel_waiting = null;
                cancel_tab.animate().translationY(display_height).setDuration(300);
                cancel_remover.removeCallbacksAndMessages(null);   //cancel handler
                cancel_remover = null;

            }
        });

        cancel_remover.postDelayed(new Runnable() {
            @Override
            public void run() {
                cancel_tab.animate().translationY(display_height).setDuration(300);
                cancel_waiting = null;
                cancel_remover = null;
            }
        }, 3000);
    }

    private void handle_data() {
        int weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;    // Get Today[0-6 sun-sat]
        weekday = weekday > 0 ? weekday - 1 : 6;    //mon-sun

        today_subjects = new LinkedList<>();
        today_sessions = new LinkedList<>();

        LinkedList<int[]> extra_sess = new LinkedList<>();
        LinkedList<int[]> cancel_sess = ((MainActivity) context).cancelled_sessions;

        for (int[] sess : ((MainActivity) context).extra_sessions)
            if (today[0] == sess[0] && today[1] == sess[1] && today[2] == sess[2])
                extra_sess.add(sess);

        LinkedList<Integer> schedule;
        for (int i = 0; i < Subject.session_encoder.length; ++i) {

            boolean overwrite = false;
            for (int[] sess : extra_sess) {
                if (sess[3] == i) {
                    today_subjects.add(data[sess[4]]);
                    today_sessions.add(i);
                    overwrite = true;
                    break;
                }
            }
            if (overwrite)
                continue;

            for (int s = 0; s < data.length; ++s) {
                schedule = data[s].slots[weekday];
                for (Integer sess : schedule) {
                    if (sess == i) {
                        boolean cancelled = false;
                        for (int[] csess : cancel_sess)
                            if (i == csess[3] && s == csess[4]) {
                                cancelled = true;
                                break;
                            }
                        if (!cancelled) {
                            today_subjects.add(data[s]);
                            today_sessions.add(i);
                        }
                    }
                }
            }
        }
    }

    void removeWarning() {
        add_class_warned = -1;
    }

    boolean handle_class_addition(LinearLayout dialog_layout, int sess_ind, int sub_ind) {

        if (add_class_warned != sess_ind && today_sessions.contains(sess_ind)) {
            //issue warning
            add_class_warned = sess_ind;
            TransitionManager.beginDelayedTransition(dialog_layout);
            dialog_layout.findViewById(R.id.dialog_warning).setVisibility(View.VISIBLE);
            return false;
        }

        //confirm override
        if (add_class_warned == sess_ind) {   //if conditions have not changed since last warning
            int chng_ind = today_sessions.indexOf(sess_ind);
            cancelClass(chng_ind);
        }

        removeWarning();
        addExtraClass(sess_ind, sub_ind);
        return true;
    }

    private void updateListForSubject(Subject s) {
        for (int i = 0; i < today_subjects.size(); ++i) {
            if (today_subjects.get(i) == s)
                updateItem(i);
        }
    }

    private void updateItem(int index) {
        Subject s = today_subjects.get(index);
        ExtraViewHolder h = card_details.get(index);
        h.updatePercent(s.attendance);
        h.updateInfo(s.total, s.missed, s.missable);
    }

    private void cancelClass(int position) {
        Subject s = today_subjects.get(position);
        s.cancel_session();
        updateListForSubject(s);

        boolean in_extra = false;
        for (int[] sess : ((MainActivity) context).extra_sessions) {
            if (sess[3] == today_sessions.get(position) && sess[4] == getSubjectIndex(today_subjects.get(position))) {
                ((MainActivity) context).extra_sessions.remove(sess);
                in_extra = true;
                break;
            }
        }
        if (!in_extra) {
            int[] add_cancel = new int[]{today[0], today[1],
                    today[2], today_sessions.get(position), getSubjectIndex(today_subjects.get(position))};

            ((MainActivity) context).cancelled_sessions.add(add_cancel);
        }
        deleteItemAt(position);
    }

    private void deleteItemAt(int position) {
        today_sessions.remove(position);
        today_subjects.remove(position);
        card_details.remove(position);
        main_list.removeViewAt(position);
    }

    void cancel_all_classes() {

        if (((MainActivity) context).missed_sessions.size() > 0) {
            StringBuilder dialog_message = new StringBuilder(context.getString(R.string.multi_absence_dialog_message_1));
            for (int[] sess_deets : ((MainActivity) context).missed_sessions) {
                dialog_message.append("\n\t").
                        append(Subject.session_encoder[sess_deets[3]][0]).
                        append("\t-\t").
                        append(data[sess_deets[4]].name);
            }
            dialog_message.append("\n\n").append(context.getString(R.string.multi_absence_dialog_message_2));
            AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.ThemedAlertDialog);
            dialog.setTitle(context.getString(R.string.absence_dialog_title));
            dialog.setMessage(dialog_message);

            dialog.setPositiveButton(context.getString(R.string.undo_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    for (int[] sess_deets : ((MainActivity) context).missed_sessions)
                        data[sess_deets[4]].unmiss_session();
                    ((MainActivity) context).missed_sessions.clear();
                }
            });

            dialog.setNegativeButton(context.getString(R.string.ignore_text), null);
            dialog.show();
        }

        TransitionManager.beginDelayedTransition(main_list);
        for (int i = today_subjects.size() - 1; i >= 0; --i)    //backward traverse[removal]
            cancelClass(i);
    }

    private void addExtraClass(int session_index, int sub_index) {
        int[] add_extra = new int[]{today[0], today[1], today[2],
                session_index, sub_index};
        ((MainActivity) context).extra_sessions.add(add_extra);
        data[sub_index].add_session();

        updateListForSubject(data[sub_index]);

        //add class to list
        int add_index = today_sessions.size();
        for (int i = 0; i < add_index; ++i) {
            if (today_sessions.get(i) > session_index) {
                add_index = i;
                break;
            }
        }

        TransitionManager.beginDelayedTransition(main_list);
        today_sessions.add(add_index, session_index);
        today_subjects.add(add_index, data[sub_index]);
        LayoutInflater inflater = LayoutInflater.from(context);
        makeSubjectView(inflater, add_index);
    }

    private int getSubjectIndex(Subject to_check) {
        for (int i = 0; i < data.length; ++i) {
            if (data[i] == to_check)
                return i;
        }
        return -1;
    }

    void handle_first_start(int stage) {
        View demo_sub = main_list.getChildAt(0);

        switch (stage) {

            //group 1[user deets]
            case 1:
                new GuideView.Builder(context)
                        .setTitle(context.getString(R.string.startup_guide_title_1))
                        .setContentText(context.getString(R.string.startup_guide_message_1))
                        //.setDismissType(DismissType.targetView) //optional - default DismissType.targetView
                        .setTargetView(((MainActivity) context).findViewById(R.id.avatar))
                        .setGuideListener(new GuideListener() {
                            @Override
                            public void onDismiss(View view) {
                                handle_first_start(2);
                            }
                        })
                        .build()
                        .show();
                break;

            case 2: //called from case 1
                new GuideView.Builder(context)
                        .setTitle(context.getString(R.string.startup_guide_title_2))
                        .setContentText(context.getString(R.string.startup_guide_message_2))
                        .setTargetView(((MainActivity) context).findViewById(R.id.name_text))
                        .build()
                        .show();
                break;


            //group 2 [subject]
            case 3:     //called from name_text listener[schedule fragment class]

                new GuideView.Builder(context)
                        .setTitle(context.getString(R.string.startup_guide_title_3))
                        .setContentText(context.getString(R.string.startup_guide_message_3))
                        .setTargetView(demo_sub.findViewById(R.id.card_view))
                        .setGuideListener(new GuideListener() {
                            @Override
                            public void onDismiss(View view) {
                                handle_first_start(4);
                            }
                        })
                        .build()
                        .show();
                break;

            case 4:     //called from case 3

                new GuideView.Builder(context)
                        .setTitle(context.getString(R.string.startup_guide_title_4))
                        .setContentText(context.getString(R.string.startup_guide_message_4))
                        .setTargetView(demo_sub.findViewById(R.id.minus_image))
                        .setGuideListener(new GuideListener() {
                            @Override
                            public void onDismiss(View view) {
                                handle_first_start(5);
                            }
                        })
                        .build()
                        .show();
                break;

            case 5:     //called from case 4

                new GuideView.Builder(context)
                        .setTitle(context.getString(R.string.startup_guide_title_5))
                        .setContentText(context.getString(R.string.startup_guide_message_5))
                        .setTargetView(card_details.getFirst().cancel)
                        .setGuideListener(new GuideListener() {
                            @Override
                            public void onDismiss(View view) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        handle_first_start(6);
                                    }
                                }, 1000);
                            }
                        })
                        .build()
                        .show();
                break;

            case 6: //from 5
                final View plus = main_list.getChildAt(today_subjects.size());
                TransitionManager.beginDelayedTransition(main_list);
                plus.getParent().requestChildFocus(plus, plus);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new GuideView.Builder(context)
                                .setTitle(context.getString(R.string.startup_guide_title_6))
                                .setContentText(context.getString(R.string.startup_guide_message_6))
                                .setTargetView(plus)
                                .build()
                                .show();

                        context.getSharedPreferences(MainActivity.shared_pref_name, MODE_PRIVATE).edit().
                                putInt("first_start", 1).apply();
                        MainActivity.is_first_start = 1;
                    }
                }, 500);
                break;
        }
    }
}
