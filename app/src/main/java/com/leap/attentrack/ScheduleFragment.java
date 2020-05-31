package com.leap.attentrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ScheduleFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_schedule, container, false);

        RecyclerView list = fragmentView.findViewById(R.id.recycler);
        list.setItemAnimator(null);     //Added transition manager for expand
        final RecyclerAdapter adapter = new RecyclerAdapter(getActivity(), fragmentView);
        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));

        fragmentView.findViewById(R.id.cancel_button_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), R.style.ThemedAlertDialog);
                dialog.setMessage(R.string.cancel_all_warning);
                dialog.setCancelable(true);
                dialog.setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.cancel_all_classes();
                    }
                });
                dialog.setNegativeButton(R.string.nope_text, null);
                dialog.show();

            }
        });

        final ImageView avatar = fragmentView.findViewById(R.id.avatar);
        avatar.setImageResource(MainActivity.is_male_avatar?R.drawable.man:R.drawable.woman);

        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.is_male_avatar = !MainActivity.is_male_avatar;
                avatar.setImageResource(MainActivity.is_male_avatar?R.drawable.man:R.drawable.woman);
            }
        });

        final TextView name_text = fragmentView.findViewById(R.id.name_text);
        name_text.setText(MainActivity.name);

        name_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder edit_dialog = new AlertDialog.Builder(getContext(), R.style.ThemedAlertDialog);
                edit_dialog.setTitle(R.string.username_edit_title);
                final EditText e = new EditText(getContext());
                e.setBackgroundColor(0xffb5dfff);
                e.setHint("Namey McNamus");
                e.setSingleLine(true);
                e.setGravity(Gravity.CENTER);
                e.setPadding(20, 20, 20, 20);
                edit_dialog.setView(e);
                edit_dialog.setCancelable(true);
                edit_dialog.setPositiveButton(R.string.save_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.name = e.getText().toString();
                        name_text.setText(MainActivity.name);
                        dialog.cancel();
                    }
                });

                if(MainActivity.is_first_start == 0) {
                    edit_dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.handle_first_start(3);  //group2
                                }
                            }, 500);
                        }
                    });
                }
                edit_dialog.show();
            }
        });
        return fragmentView;
    }
}
