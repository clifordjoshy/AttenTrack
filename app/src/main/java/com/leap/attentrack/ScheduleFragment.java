package com.leap.attentrack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

public class ScheduleFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_schedule, container, false);
        final Context context = getContext();

        final ScheduleList list_handler = new ScheduleList(context, fragmentView);

        final ImageView avatar = fragmentView.findViewById(R.id.avatar);
        avatar.setImageResource(MainActivity.is_male_avatar?R.drawable.avatar_man :R.drawable.avatar_woman);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            View divider = fragmentView.findViewById(R.id.divider);
            if(divider != null) {   //landscape
                int color = MainActivity.dark_mode_on ? 0x25ffffff : 0x25272727;
                divider.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
        }

        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.is_male_avatar = !MainActivity.is_male_avatar;
                avatar.setImageResource(MainActivity.is_male_avatar?R.drawable.avatar_man :R.drawable.avatar_woman);
            }
        });

        final TextView name_text = fragmentView.findViewById(R.id.name_text);
        name_text.setText(MainActivity.name);

        name_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder edit_dialog = new AlertDialog.Builder(context);
                edit_dialog.setTitle(R.string.username_edit_title);
                final AppCompatEditText e = new AppCompatEditText(context);
                e.setBackgroundColor(0xffb5dfff);
                e.setHint(MainActivity.name);
                e.setSingleLine(true);
                e.setGravity(Gravity.CENTER);
                e.setPadding(20, 20, 20, 20);
                edit_dialog.setView(e);
                edit_dialog.setCancelable(true);
                edit_dialog.setPositiveButton(R.string.save_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String new_name = e.getText().toString();
                        if(!"".equals(new_name)) {
                            MainActivity.name = new_name;
                            name_text.setText(new_name);
                        }
                    }
                });
                edit_dialog.setNegativeButton(R.string.cancel_text, null);

                if(MainActivity.is_first_start == 0) {
                    edit_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    list_handler.handle_first_start(3);  //group2
                                }
                            }, 500);
                        }
                    });
                }
                edit_dialog.show();
            }
        });

        fragmentView.findViewById(R.id.cancel_button_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setMessage(R.string.cancel_all_warning);
                dialog.setCancelable(true);
                dialog.setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        list_handler.cancel_all_classes();
                    }
                });
                dialog.setNegativeButton(R.string.nope_text, null);
                dialog.show();

            }
        });

        fragmentView.findViewById(R.id.extra_class_plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                final Spinner session, subject;

                final ConstraintLayout dialog_layout = (ConstraintLayout) LayoutInflater.from(context).
                        inflate(R.layout.dialog_extra_class, null);
                session = dialog_layout.findViewById(R.id.session_spinner);
                subject = dialog_layout.findViewById(R.id.subject_spinner);

                String[] all_subs = new String[MainActivity.data.length];
                for (int i = 0; i < all_subs.length; ++i)
                    all_subs[i] = MainActivity.data[i].name;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item, all_subs);
                subject.setAdapter(adapter);

                String[] all_times = new String[Subject.session_encoder.length];
                for (int i = 0; i < all_times.length; ++i)
                    all_times[i] = Subject.session_encoder[i][0] + "-" + Subject.session_encoder[i][1];
                adapter = new ArrayAdapter<>(context, R.layout.spinner_item, all_times);
                session.setAdapter(adapter);

                dialog.setView(dialog_layout);
                dialog.setCancelable(true);
                dialog.setNegativeButton(R.string.cancel_text, null);
                dialog.setPositiveButton(R.string.add_button_text, null);  //closes dialog after click. so overrided after showing

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface d) {
                        list_handler.removeWarning();   //cancel without override
                    }
                });


                final AlertDialog shown = dialog.show();
                shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Get Form Data
                        int session_selected_index = session.getSelectedItemPosition(),
                                sub_selected_index = subject.getSelectedItemPosition();
                        if (list_handler.handle_class_addition(dialog_layout, session_selected_index,sub_selected_index)) {
                            shown.dismiss();
                        }
                    }
                });
            }
        });

        return fragmentView;
    }
}
