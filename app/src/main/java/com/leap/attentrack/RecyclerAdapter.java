package com.leap.attentrack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;

import static android.content.Context.MODE_PRIVATE;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    private Context context;
    private RecyclerView mRecyclerView;

    private LinkedList<Subject> today_subjects;
    private LinkedList<Integer> today_sessions;
    private LinkedList<Boolean> is_open;

    private int[] today;
    private int warned_for = -1;

    RecyclerAdapter(Context ct) {
        context = ct;
        String[] date_string = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()).split("-");
        today = new int[]{Integer.parseInt(date_string[0]), Integer.parseInt(date_string[1]), Integer.parseInt(date_string[2])};
        handle_data();    //fills up details, today_sess, all_subs
        is_open = new LinkedList<>();
        for (int i = 0; i < today_sessions.size(); ++i)
            is_open.add(false);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View created = inflater.inflate(R.layout.element_schedule, parent, false);
        return new MyViewHolder(created);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
        if (MainActivity.is_first_start == 0) {
            Subject demo = new Subject();
            demo.name = "Subject";
            demo.attendance = 96;
            demo.total = 56;
            demo.missed = 2;
            demo.missable = (100 - Subject.req_percentage) * demo.total / 100;
            demo.color = 0xffffa5c0;

            //demo subject
            today_subjects.addFirst(demo);
            is_open.addFirst(false);
            today_sessions.addFirst(0);
            notifyItemInserted(0);
            notifyItemRangeChanged(0, getItemCount());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    handle_first_start(1);  //group1
                }
            }, 1000);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {
        handle_print(holder, position);
        handle_main_click(holder, position);
    }

    private void handle_data() {
        LinkedList<Subject> deets = MainActivity.data;
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
        for (int i = 0; i < Subject.session_encoder.size(); ++i) {

            boolean overwrite = false;
            for (int[] sess : extra_sess) {
                if (sess[3] == i) {
                    today_subjects.add(deets.get(sess[4]));
                    today_sessions.add(i);
                    overwrite = true;
                    break;
                }
            }
            if (overwrite)
                continue;

            for (int s = 0; s < deets.size(); ++s) {
                schedule = deets.get(s).slots[weekday];
                for (Integer sess : schedule) {
                    if (sess == i) {
                        boolean cancelled = false;
                        for (int[] csess : cancel_sess)
                            if (i == csess[3] && s == csess[4]) {
                                cancelled = true;
                                break;
                            }
                        if (!cancelled) {
                            today_subjects.add(deets.get(s));
                            today_sessions.add(i);
                        }
                    }
                }
            }
        }
    }

    private void handle_main_click(@NonNull final MyViewHolder holder, final int position) {
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position < today_subjects.size()) {
                    Transition tr = new AutoTransition();
                    tr.setDuration(150);
                    TransitionManager.beginDelayedTransition(mRecyclerView, tr);

                    is_open.set(position, !is_open.get(position));

                    if (is_open.get(position)) {
                        holder.extras.setVisibility(View.VISIBLE);
                        holder.cancel.setVisibility(View.VISIBLE);
                    } else {
                        holder.extras.setVisibility(View.GONE);
                        holder.cancel.setVisibility(View.GONE);
                    }
                    notifyItemChanged(position);

                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.ThemedAlertDialog);
                    final Spinner session, subject;

                    final LinearLayout dialog_layout = (LinearLayout) ((MainActivity)context).
                            getLayoutInflater().inflate(R.layout.extra_class_dialog_layout, null);
                    session = dialog_layout.findViewById(R.id.session_spinner);
                    subject = dialog_layout.findViewById(R.id.subject_spinner);

                    String[] all_subs = new String[MainActivity.data.size()];
                    for (int i = 0; i < all_subs.length; ++i)
                        all_subs[i] = MainActivity.data.get(i).name;
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item, all_subs);
                    subject.setAdapter(adapter);

                    String[] all_times = new String[Subject.session_encoder.size()];
                    for (int i = 0; i < all_times.length; ++i)
                        all_times[i] = Subject.session_encoder.get(i)[0] + "-" + Subject.session_encoder.get(i)[1];
                    adapter = new ArrayAdapter<>(context, R.layout.spinner_item, all_times);
                    session.setAdapter(adapter);
                    dialog.setView(dialog_layout);

                    dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            d.cancel();
                        }
                    });

                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface d) {
                            warned_for = -1;  //cancel without override
                        }
                    });

                    dialog.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dlog, int which) {
                            //Get Form Data
                            int session_selected_index = session.getSelectedItemPosition(),
                                    sub_selected_index = subject.getSelectedItemPosition();
                            if (handle_class_addition(dialog_layout, session_selected_index)) {
                                dlog.cancel();
                                int[] add_extra = new int[]{today[0], today[1], today[2],
                                        session_selected_index, sub_selected_index};
                                ((MainActivity) context).extra_sessions.add(add_extra);
                                MainActivity.data.get(sub_selected_index).add_session();

                                //add class to list
                                int add_index = today_sessions.size();
                                for (int i = 0; i < add_index; ++i)
                                    if (today_sessions.get(i) > session_selected_index) {
                                        add_index = i;
                                        break;
                                    }
                                TransitionManager.beginDelayedTransition(mRecyclerView);
                                today_sessions.add(add_index, session_selected_index);
                                is_open.add(add_index, false);
                                today_subjects.add(add_index, MainActivity.data.get(sub_selected_index));
                                notifyItemInserted(add_index);
                                notifyItemRangeChanged(add_index, getItemCount());

                                // [called before view is added]
                                update_printed_details(MainActivity.data.get(sub_selected_index),
                                        today_sessions.indexOf(session_selected_index));    //ignore this position
                            }
                        }
                    });
                dialog.show();
                }
            }
        });
    }

    private void handle_print(final MyViewHolder holder, final int position) {
        if (position < today_subjects.size()) {    // Subject Field
            final Subject this_sub = today_subjects.get(position);

            holder.subject.setText(this_sub.name);
            holder.time.setText(Subject.session_encoder.get(today_sessions.get(position))[0]);
            holder.percent.setText((this_sub.attendance + "%"));
            holder.extras.setText(("Total:" + this_sub.total + "    Missed:" + this_sub.missed + "    Missable:" + this_sub.missable));
            ViewCompat.setBackgroundTintList(holder.root,ColorStateList.valueOf(this_sub.color));

            holder.picture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int missed_sess = today_sessions.get(position), missed_sub = MainActivity.data.indexOf(today_subjects.get(position));
                    for (int[] sess : ((MainActivity) context).missed_sessions) {
                        if (sess[3] == missed_sess && sess[4] == missed_sub) {
                            Toast.makeText(context, "You Already Missed This Class", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    int[] add_missed = new int[]{today[0], today[1], today[2], missed_sess, missed_sub};
                    ((MainActivity) context).missed_sessions.add(add_missed);
                    this_sub.missed_session();
                    update_printed_details(this_sub, -1);
                }
            });

            holder.cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TransitionManager.beginDelayedTransition(mRecyclerView);
                    cancel_class(this_sub, position, holder);
                }
            });


        } else {        // + button
            holder.subject.setVisibility(View.GONE);
            holder.time.setVisibility(View.GONE);
            holder.percent.setVisibility(View.GONE);
            boolean dark_mode = MainActivity.dark_mode_on;
            ViewCompat.setBackgroundTintList(holder.root, ColorStateList.valueOf(dark_mode ? 0xffffffff : 0xff272727));
            holder.picture.setImageResource(dark_mode ? R.drawable.plusb : R.drawable.plusw);

            ConstraintSet constraints = new ConstraintSet();
            int dim1 = (int) (22 * context.getResources().getDisplayMetrics().density),
                    dim2 = (int) (42 * context.getResources().getDisplayMetrics().density);
            constraints.clear(holder.picture.getId());
            constraints.constrainHeight(holder.picture.getId(), dim2);
            constraints.constrainWidth(holder.picture.getId(), dim1);
            constraints.connect(holder.picture.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
            constraints.connect(holder.picture.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
            constraints.applyTo(holder.root);
        }
    }

    private boolean handle_class_addition(LinearLayout dialog_layout, int sess_ind) {
        //confirm press check[class override]
        if (warned_for == sess_ind) {
                int chng_ind = today_sessions.indexOf(sess_ind);
                cancel_class(today_subjects.get(chng_ind), chng_ind, (MyViewHolder)mRecyclerView.findViewHolderForAdapterPosition(chng_ind));
                warned_for = -1;
                return true;
        }

        if (today_sessions.contains(sess_ind)) {
            warned_for = sess_ind;
            TextView warning = new TextView(context);
            warning.setText(R.string.class_override_warning);
            warning.setPadding(0, 0,0,0);
            warning.setTextColor(context.getResources().getColor(R.color.dialog_warning));
            dialog_layout.addView(warning);
            return false;
        }

        warned_for = -1;  //corrected warning without override
        return true;
    }

    private void update_printed_details(Subject s, int ignore_position) {
        int last_visible = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findLastVisibleItemPosition();
        int first_visible = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();

        if (today_subjects.size() == last_visible)
            --last_visible;     //From (+) to last subject

        for (int i = 0; i < today_subjects.size(); ++i) {
            if (i != ignore_position && today_subjects.get(i) == s) {
                if (i < first_visible || i > last_visible)
                    notifyItemChanged(i);       //out of range view
                else {
                    MyViewHolder holder = (MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
                    holder.percent.setText((s.attendance + "%"));
                    holder.extras.setText(("Total:" + s.total + "    Missed:" + s.missed + "    Missable:" + s.missable));
//                  notifyItemChanged(i);       //works, but probably creats new onclicklistener
                }
            }
        }
    }

    private void cancel_class(Subject this_sub, int position, MyViewHolder holder){
        this_sub.cancel_session();
        update_printed_details(this_sub, -1);

        boolean in_extra = false;
        for (int[] sess : ((MainActivity) context).extra_sessions) {
            if (sess[3] == today_sessions.get(position) && sess[4] == MainActivity.data.indexOf(today_subjects.get(position))) {
                ((MainActivity) context).extra_sessions.remove(sess);
                in_extra = true;
                break;
            }
        }
        if (!in_extra) {
            int[] add_cancel = new int[]{today[0], today[1],
                    today[2], today_sessions.get(position),
                    MainActivity.data.indexOf(today_subjects.get(position))};

            ((MainActivity) context).cancelled_sessions.add(add_cancel);
        }

        today_sessions.remove(position);
        today_subjects.remove(position);

        is_open.remove(position);
        holder.cancel.setVisibility(View.GONE);     //Reusing same view?
        holder.extras.setVisibility(View.GONE);

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    void cancel_all_classes() {
        TransitionManager.beginDelayedTransition(mRecyclerView);
        for (int i = today_subjects.size() - 1; i >= 0; --i)    //backward traverse[removal]
            cancel_class(today_subjects.get(i), i, (MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i));

    }

    @Override
    public int getItemCount() {
        // +1 is for the (+) button
        return today_subjects.size() + 1;
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView subject, time, percent, extras;
        ConstraintLayout root;
        ImageView picture, cancel;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.subject_text);
            time = itemView.findViewById(R.id.time_text);
            percent = itemView.findViewById(R.id.percent_text);
            root = itemView.findViewById(R.id.card_view);
            picture = itemView.findViewById(R.id.click_image);
            extras = itemView.findViewById(R.id.data_text);
            cancel = itemView.findViewById(R.id.cancel_button);

        }
    }

    void handle_first_start(int stage) {

        switch (stage) {

            //group 1[user deets] //from onAttachedToRecyclerView()
            case 1:
                new GuideView.Builder(context)
                        .setTitle("Change Your Avatar")
                        .setContentText("Have It Your Way :)")
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
                        .setTitle("Change Your Username")
                        .setContentText("You're already spectacular. You\ndon't need me to say it.")
                        .setTargetView(((MainActivity) context).findViewById(R.id.name_text))
                        .build()
                        .show();
                break;


            //group 2 [subject]
            case 3:     //called from name_text listener[schedule fragment class]

                new GuideView.Builder(context)
                        .setTitle("Here's Your Subject Card")
                        .setContentText("Click For More Info.")
                        .setTargetView(((MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(0)).root)
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
                        .setTitle("Miss A Class?")
                        .setContentText("Click here to mark your absence\n" +
                                "and see your percentage change.")
                        .setTargetView(((MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(0)).picture)
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
                        .setTitle("Session Cancelled?")
                        .setContentText("Click here to mark it and see how\n" +
                                "your attendance changes.")
                        .setTargetView(((MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(0)).cancel)
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
                mRecyclerView.smoothScrollToPosition(today_subjects.size());

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new GuideView.Builder(context)
                                .setTitle("Extra Class?")
                                .setContentText("Click here to add classes\n" +
                                        "to the schedule.")
                                .setTargetView(((MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(today_subjects.size())).root)
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
