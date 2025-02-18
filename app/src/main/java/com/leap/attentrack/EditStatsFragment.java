package com.leap.attentrack;

import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.transition.TransitionManager;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;

public class EditStatsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment_view = inflater.inflate(R.layout.fragment_all_classes, container, false);
        final LinearLayout root_layout = fragment_view.findViewById(R.id.all_subs_root_layout);

        for (int i = 0; i < MainActivity.data.length; ++i) {
            final Subject s = MainActivity.data[i];
            View element = getLayoutInflater().inflate(R.layout.element_all_subs, root_layout, false);
            setSubjectText((TextView)element.findViewById(R.id.subject_text), s.name);
            update_deets(s, (TextView) element.findViewById(R.id.percent_text), (TextView) element.findViewById(R.id.data_text));

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
                    AlertDialog.Builder edit_dialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
                    edit_dialog.setTitle(R.string.subject_name_edit_title);
                    final AppCompatEditText e = new AppCompatEditText(getContext());
                    e.setBackgroundColor(s.color);
                    e.setHint(s.name);
                    e.setHintTextColor(0xff808080);
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
                                TransitionManager.beginDelayedTransition(root_layout);
                                setSubjectText((TextView)v, temp);
                            }
                            int current_color = ((ColorDrawable)e.getBackground()).getColor();
                            if(s.color != current_color){
                                ValueAnimator animator = ValueAnimator.ofArgb(s.color, current_color);
                                s.color = current_color;
                                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        int value = (Integer)animation.getAnimatedValue();
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                                            ((ConstraintLayout)v.getParent()).getBackground().
                                                    setColorFilter(value, PorterDuff.Mode.MULTIPLY);
                                        else
                                            ((ConstraintLayout)v.getParent()).
                                                    setBackgroundTintList(ColorStateList.valueOf(value));
                                    }
                                });
                                animator.setDuration(350);
                                animator.start();
                            }
                        }
                    });
                    edit_dialog.setNegativeButton(R.string.cancel_text, null);
                    edit_dialog.setNeutralButton(R.string.change_color_text, null);
                    edit_dialog.show().getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                        int[] valid_colors = new int[]{0xffffbe93, 0xffbbf6bf, 0xffabecff, 0xfffcb1fa, 0xff88acfd,
                                0xfff7f7be, 0xff9affff, 0xffdcfaa3, 0xffffb9b9, 0xffa3fad2, 0xffb5dfff, 0xffe6c6ff};
                        int color_index = -1;

                        @Override
                        public void onClick(View v) {
                            if (color_index == -1) {
                                while(valid_colors[++color_index] != s.color);
                            }
                            color_index = (color_index + 1) % valid_colors.length;
                            e.setBackgroundColor(valid_colors[color_index]);
                        }
                    });
                }
            });

            root_layout.addView(element);
        }

        handle_first_start(root_layout);
        return fragment_view;
    }

    private void update_deets(Subject s, TextView percent, TextView deet) {
        percent.setText((s.attendance + "%"));
        String deet_text = getString(R.string.total_text) + s.total + "    " +
                getString(R.string.missed_text) + s.missed + "    " +
                getString(R.string.missable_text) + s.missable;
        deet.setText(deet_text);
    }

    private void handle_first_start(final LinearLayout root) {
        boolean first = getActivity().getSharedPreferences(MainActivity.shared_pref_name, MainActivity.MODE_PRIVATE).
                getBoolean("edit_stats_first_start", true);
        if (!first)
            return;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new GuideView.Builder(getContext())
                        .setTitle(getString(R.string.edit_stats_guide_title_1))
                        .setContentText(getString(R.string.edit_stats_guide_message_1))
                        .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                        .setTargetView(root.getChildAt(0))
                        .setGuideListener(new GuideListener() {
                            @Override
                            public void onDismiss(View view) {
                                new GuideView.Builder(getContext())
                                        .setTitle(getString(R.string.edit_stats_guide_title_2))
                                        .setContentText(getString(R.string.edit_stast_guide_message_2))
                                        .setTargetView(root.getChildAt(0).findViewById(R.id.subject_text))
                                        .build()
                                        .show();
                            }
                        })
                        .build()
                        .show();
            }
        }, 500);

        getActivity().getSharedPreferences(MainActivity.shared_pref_name, MainActivity.MODE_PRIVATE).
                edit().putBoolean("edit_stats_first_start", false).apply();

    }

    void setSubjectText(TextView subject, String text){
        if(text.indexOf(' ') == -1)   //if single word
            subject.setMaxLines(1);
        else
            subject.setMaxLines(2);
        subject.setText(text);
    }
}
