package com.leap.attentrack;

import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.transition.TransitionManager;

import com.google.android.material.textfield.TextInputLayout;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class AssignmentsFragment extends Fragment {

    static class Assignment implements Serializable {
        String title;
        int status;
        String note;
        Date due_date;
        int subject;

        Assignment(String title, int status, String note, Date due_date, int subject) {
            this.title = title;
            this.status = status;
            this.note = note;
            this.due_date = due_date;
            this.subject = subject;
        }
    }

    public static LinkedList<Assignment> assignments_list = new LinkedList<>();
    private LinkedList<Boolean> is_expanded;
    private LinearLayout root;
    private float density;
    private Date today;
    private boolean new_assignment_active = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_assignments, container, false);
        setHasOptionsMenu(true);

        density = getResources().getDisplayMetrics().density;

        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        today = now.getTime();

        root = fragmentView.findViewById(R.id.linear_assignments);

        // sort and handle assignments_list
        handleAssignmentsData();
        printAssignments();

        return fragmentView;
    }

    private void handleAssignmentsData() {
        //sort
        Collections.sort(assignments_list, new Comparator<Assignment>() {
            @Override
            public int compare(Assignment o1, Assignment o2) {
                return o1.due_date.compareTo(o2.due_date);
            }
        });

        int i = 0;
        while (i < assignments_list.size()) {
            Assignment a = assignments_list.get(i);
            if (a.status == 1) {
                //remove completed assignments
                assignments_list.remove(i);
                --i;
            } else if (a.status == 0 && today.after(a.due_date)) {
                a.status = -1;     //overdue
            }
            ++i;
        }

        is_expanded = new LinkedList<>();
        for (i = 0; i < assignments_list.size(); ++i)
            is_expanded.add(false);

    }

    private void printAssignments() {
        if (assignments_list.size() == 0) {
            root.findViewById(R.id.no_assignments_message).setVisibility(View.VISIBLE);
            return;
        }

        boolean overdue_flag = false;
        Date current_date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd");
        for (Assignment a : assignments_list) {

            //add date title
            if (a.status == -1) {
                if (!overdue_flag) {
                    root.addView(createTitleTextView(getString(R.string.overdue_text)));
                    overdue_flag = true;
                }

            } else {
                if (!a.due_date.equals(current_date)) {
                    String date_string = sdf.format(a.due_date);
                    root.addView(createTitleTextView(date_string));
                    current_date = a.due_date;
                }
            }

            //add view
            root.addView(createAssignmentView(a));
        }
    }

    private TextView createTitleTextView(String text) {
        AppCompatTextView textView = new AppCompatTextView(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, (int) (6 * density), 0, (int) (6 * density));
        textView.setLayoutParams(lp);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        textView.setTextColor(0xff808080);
        textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
        return textView;
    }

    private TextView createAssignmentView(final Assignment a) {
        final AppCompatTextView assignment = new AppCompatTextView(getContext());
        //appcompat to support compound drawable
        assignment.setText(a.title);
        assignment.setTextColor(MainActivity.dark_mode_on ? 0xffffffff : 0xff272727);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins((int) (15 * density), 0, 0, (int) (4 * density));
        assignment.setLayoutParams(lp);
        assignment.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        updateIndicator(assignment, a.status);

        assignment.setCompoundDrawablesWithIntrinsicBounds(R.drawable.assignment_indicator, 0, 0, 0);
        assignment.setCompoundDrawablePadding((int) (20 * density));

        assignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int assignment_index = assignments_list.indexOf(a), view_index = root.indexOfChild(v);
                TransitionManager.beginDelayedTransition(root);
                if (!is_expanded.get(assignment_index)) {
                    root.addView(createExpandedView(a), view_index + 1);
                    is_expanded.set(assignment_index, true);
                } else {
                    root.removeViewAt(view_index + 1);
                    is_expanded.set(assignment_index, false);
                }
            }
        });
        return assignment;
    }

    private View createExpandedView(final Assignment assignment) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ConstraintLayout expanded = (ConstraintLayout) inflater.inflate(R.layout.assignment_expanded_view, root, false);
        int tint_color = MainActivity.data[assignment.subject].color;

        final TextView subject = expanded.findViewById(R.id.expanded_subject_view),
                due_days = expanded.findViewById(R.id.day_count_view);
        final EditText notes = expanded.findViewById(R.id.notes_view);

        subject.setText(MainActivity.data[assignment.subject].name);
        subject.setBackgroundTintList(ColorStateList.valueOf(tint_color));

        due_days.setText(getDueString(assignment.due_date));
        due_days.setBackgroundTintList(ColorStateList.valueOf(tint_color));

        notes.setText(assignment.note);
        notes.setBackgroundTintList(ColorStateList.valueOf(tint_color));

        expanded.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
                dialog.setMessage(R.string.delete_assignment_warning);
                dialog.setPositiveButton(R.string.okay_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TransitionManager.beginDelayedTransition(root);
                        int assignment_index = assignments_list.indexOf(assignment),
                                view_index = root.indexOfChild((ConstraintLayout) v.getParent()) - 1;
                        TransitionManager.beginDelayedTransition(root);
                        deleteAssignment(assignment_index, view_index);
                        if (assignments_list.size() == 0 && !new_assignment_active)
                            root.findViewById(R.id.no_assignments_message).setVisibility(View.VISIBLE);
                    }
                });
                dialog.setNegativeButton(R.string.cancel_text, null);
                dialog.show();
            }
        });

        final boolean[] editing = new boolean[1];
        expanded.findViewById(R.id.edit_button).setOnClickListener(new View.OnClickListener() {
            Date[] new_date = new Date[1];

            @Override
            public void onClick(View v) {
                editing[0] = !editing[0];
                changeEditMode(editing[0], due_days, notes, new_date);

                if (!editing[0]) {  //save changes
                    assignment.note = notes.getText().toString();
                    if (new_date[0] != null) {      //move assignment to new date
                        TransitionManager.beginDelayedTransition(root);
                        int delete_assignment_index = assignments_list.indexOf(assignment),
                                delete_view_index = root.indexOfChild((ConstraintLayout) v.getParent()) - 1;
                        deleteAssignment(delete_assignment_index, delete_view_index);

                        assignment.due_date = new_date[0];
                        addAssignment(assignment);
                    }
                    ((TextView) v).setText(R.string.edit_button_text);
                }
            }
        });

        expanded.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editing[0] = false;
                changeEditMode(false, due_days, notes, null);
            }
        });
        TextView mark_as_done_btn = expanded.findViewById(R.id.mark_as_done_button);
        if (assignment.status == 1)
            mark_as_done_btn.setText(R.string.mark_as_undone_text);
        mark_as_done_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (assignment.status != 1) {
                    assignment.status = 1;
                    ((TextView) v).setText(R.string.mark_as_undone_text);
                } else {    //undone
                    assignment.status = assignment.due_date.before(today) ? -1 : 0;
                    ((TextView) v).setText(R.string.mark_as_done_text);
                }
                int expanded_index = root.indexOfChild((ConstraintLayout) v.getParent());
                updateIndicator((TextView) root.getChildAt(expanded_index - 1), assignment.status);
            }
        });
        return expanded;
    }

    private void changeEditMode(boolean shouldEdit, final TextView due_days, final EditText notes,
                                final Date[] new_date) {
        int start_width = 0, end_width = 0;
        final int animation_duration = 500;

        ViewGroup parent = (ViewGroup) due_days.getParent();
        final TextView edit_btn = parent.findViewById(R.id.edit_button),
                cancel_btn = parent.findViewById(R.id.cancel_button),
                done_btn = parent.findViewById(R.id.mark_as_done_button),
                delete_btn = parent.findViewById(R.id.delete_button);

        edit_btn.animate().alpha(0f).setDuration(animation_duration / 2);
        cancel_btn.animate().alpha(0f).setDuration(animation_duration / 2);
        done_btn.animate().alpha(0f).setDuration(animation_duration / 2);
        delete_btn.animate().alpha(0f).setDuration(animation_duration / 2);

        if (shouldEdit) {
            end_width = (int) (2.5 * density);
            notes.setEnabled(true);
            notes.requestFocus();
            notes.setSelection(notes.getText().length());

            Calendar now = Calendar.getInstance();
            final DatePickerDialog picker = new DatePickerDialog(getContext(),
                    MainActivity.dark_mode_on ? android.R.style.Theme_Material_Dialog : android.R.style.Theme_Material_Light_Dialog,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            ++month;    //0-11
                            String date_string = (dayOfMonth < 10 ? "0" : "") + dayOfMonth + "/" +
                                    (month < 10 ? "0" : "") + month + "/" + year;
                            try {
                                new_date[0] = new SimpleDateFormat("dd/MM/yyyy").parse(date_string);
                                date_string = getDueString(new_date[0]);
                            } catch (ParseException ignored) {
                            }
                            due_days.setText(date_string);
                        }
                    }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(now.getTimeInMillis());

            due_days.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    picker.show();
                }
            });

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    int color = getResources().getColor(R.color.colorAccent);
                    notes.setTextColor(color);
                    due_days.setTextColor(color);

                    edit_btn.setText(R.string.save_text);

                    cancel_btn.setVisibility(View.VISIBLE);
                    done_btn.setVisibility(View.GONE);

                    edit_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                    cancel_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                    done_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                    delete_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                }
            }, animation_duration / 2);

        } else {
            start_width = (int) (2.5 * density);
            notes.setEnabled(false);
            due_days.setOnClickListener(null);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    int color = getResources().getColor(R.color.metaTextColor);
                    notes.setTextColor(color);
                    due_days.setTextColor(color);

                    edit_btn.setText(R.string.edit_button_text);

                    cancel_btn.setVisibility(View.GONE);
                    done_btn.setVisibility(View.VISIBLE);

                    edit_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                    cancel_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                    done_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                    delete_btn.animate().alpha(1f).setDuration(animation_duration / 2);
                }
            }, animation_duration / 2);
        }

        ValueAnimator animator = ValueAnimator.ofInt(start_width, end_width);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue(), color = getResources().getColor(R.color.colorAccent);
                ((GradientDrawable) due_days.getBackground()).setStroke(value, color);
                ((GradientDrawable) notes.getBackground()).setStroke(value, color);
            }
        });
        animator.setDuration(animation_duration);
        animator.start();
    }

    private void addAssignment(Assignment assignment) {
        int assignment_index = getAssignmentAddIndex(assignment),
                view_index = getViewAddIndex(assignment);

        if (assignment.status != 1)      //repositioning
            assignment.status = 0;



        if (assignment_index == 0 ||
                !assignment.due_date.equals(assignments_list.get(assignment_index - 1).due_date)) {
            root.addView(createTitleTextView(
                    new SimpleDateFormat("EEEE, MMMM dd").format(assignment.due_date)), view_index);
            ++view_index;
        }

        assignments_list.add(assignment_index, assignment);
        is_expanded.add(assignment_index, false);
        root.addView(createAssignmentView(assignment), view_index);
    }

    private void deleteAssignment(int assignmentIndex, int viewIndex) {
        Assignment removed = assignments_list.remove(assignmentIndex);
        if (is_expanded.remove(assignmentIndex))
            root.removeViewAt(viewIndex + 1);
        root.removeViewAt(viewIndex);

        boolean nothing_on_top, nothing_below;
        if (removed.status == -1) {
            nothing_on_top = assignmentIndex == 0;
            nothing_below = assignmentIndex == assignments_list.size() ||
                    assignments_list.get(assignmentIndex).status != -1;
        } else {
            nothing_on_top = assignmentIndex == 0 ||
                    !removed.due_date.equals(assignments_list.get(assignmentIndex - 1).due_date);
            nothing_below = assignmentIndex == assignments_list.size() ||
                    !removed.due_date.equals(assignments_list.get(assignmentIndex).due_date);
        }

        if (nothing_on_top && nothing_below)
            root.removeViewAt(viewIndex - 1);
    }

    private int getAssignmentAddIndex(Assignment assignment) {
        int add_index = 0;
        for (Assignment list_assignment : assignments_list) {
            if (list_assignment.status != -1 && list_assignment.due_date.after(assignment.due_date))
                break;
            ++add_index;
        }
        return add_index;
    }

    private int getViewAddIndex(Assignment assignment) {
        int add_index = root.indexOfChild(root.findViewById(R.id.no_assignments_message)) +
                ((assignments_list.size() != 0 && assignments_list.getFirst().status == -1) ? 2 : 1);
        Date previous_date = null;
        for (int i = 0; i < assignments_list.size(); ++i) {
            Assignment list_assignment = assignments_list.get(i);
            if (list_assignment.status != -1) {
                if (list_assignment.due_date.after(assignment.due_date))
                    break;
                if (!list_assignment.due_date.equals(previous_date)) {
                    ++add_index;   //title
                    previous_date = list_assignment.due_date;
                }
            }
            ++add_index;
            if (is_expanded.get(i))
                ++add_index;
        }
        return add_index;
    }

    private void updateIndicator(final TextView t, int status) {
        int color = 0xffffee6e;     //yellow
        if (status == -1)
            color = 0xfffc9161;     //red
        else if (status == 1)
            color = 0xff98ec7a;     //green

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && t.getCompoundDrawableTintList() != null) {
            ValueAnimator color_changer = ValueAnimator.ofArgb(t.getCompoundDrawableTintList().getDefaultColor(), color);
            color_changer.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    TextViewCompat.setCompoundDrawableTintList(t, ColorStateList.valueOf((Integer) animation.getAnimatedValue()));
                }
            });
            color_changer.setDuration(300);
            color_changer.start();
        } else
            TextViewCompat.setCompoundDrawableTintList(t, ColorStateList.valueOf(color));
    }

    private String getDueString(Date due_date) {
        String due_string;
        int day_count = (int) ((due_date.getTime() - today.getTime()) / 86400000);
        if (day_count > 1)
            due_string = getString(R.string.due_in_days_text).replace("0", "" + day_count);
        else if (day_count == 1)
            due_string = getString(R.string.due_tomorrow_text);
        else if (day_count == 0)
            due_string = getString(R.string.due_today_text);
        else if (day_count == -1)
            due_string = getString(R.string.due_yesterday_text);
        else
            due_string = getString(R.string.due_days_ago_text).replace("0", "" + (-day_count));

        return due_string;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.assignment_options_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        TransitionManager.beginDelayedTransition(root);

        if (!new_assignment_active) {
            new_assignment_active = true;
            if (assignments_list.size() == 0)
                root.findViewById(R.id.no_assignments_message).setVisibility(View.GONE);

            LayoutInflater inflater = LayoutInflater.from(getContext());
            final ConstraintLayout new_assignment_view = (ConstraintLayout) inflater.inflate(R.layout.assignment_new_view, root, false);

            final TextInputLayout title_wrapper = new_assignment_view.findViewById(R.id.assignment_title_wrapper),
                    subject_wrapper = new_assignment_view.findViewById(R.id.assignment_subject_wrapper),
                    due_wrapper = new_assignment_view.findViewById(R.id.assignment_due_wrapper);

            final AutoCompleteTextView subject_dropdown = new_assignment_view.findViewById(R.id.subject_dropdown);

            final EditText due_days = new_assignment_view.findViewById(R.id.assignment_due_date),
                    description_text = new_assignment_view.findViewById(R.id.assignment_description);

            final String[] subjects = new String[MainActivity.data.length];
            for (int i = 0; i < subjects.length; ++i)
                subjects[i] = MainActivity.data[i].name;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.assignment_spinner_item, subjects);

            subject_dropdown.setAdapter(adapter);
            subject_dropdown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideKeyboard();
                }
            });

            final Date[] picked = new Date[1];
            due_days.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideKeyboard();
                    Calendar now = Calendar.getInstance();
                    DatePickerDialog picker = new DatePickerDialog(getContext(),
                            MainActivity.dark_mode_on ? android.R.style.Theme_Material_Dialog : android.R.style.Theme_Material_Light_Dialog,
                            new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                    ++month;    //0-11
                                    String date_string = (dayOfMonth < 10 ? "0" : "") + dayOfMonth + "/" +
                                            (month < 10 ? "0" : "") + month + "/" + year;
                                    try {
                                        picked[0] = new SimpleDateFormat("dd/MM/yyyy").parse(date_string);
                                        date_string = new SimpleDateFormat("EEEE, MMMM dd").format(picked[0]);
                                    } catch (ParseException ignored) {
                                    }
                                    due_days.setText(date_string);
                                }
                            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    picker.getDatePicker().setMinDate(now.getTimeInMillis());
                    picker.show();
                }
            });


            new_assignment_view.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String title = title_wrapper.getEditText().getText().toString(),
                            subject = subject_dropdown.getText().toString(),
                            description = description_text.getText().toString();
                    int subject_index = -1;

                    boolean title_ok = true;
                    if ("".equals(title)) {
                        title_wrapper.setError(getString(R.string.assignment_title_error_message));
                        title_ok = false;
                    }

                    for (int i = 0; i < subjects.length; ++i) {
                        if (subjects[i].equals(subject)) {
                            subject_index = i;
                            break;
                        }
                    }
                    if (subject_index == -1) {
                        subject_wrapper.setError(getString(R.string.assignment_subject_error_message));
                    }

                    boolean date_ok = true;
                    if (picked[0] == null || picked[0].before(today)) {
                        date_ok = false;
                        due_wrapper.setError(getString(R.string.assignment_due_date_error_message));
                    }

                    if (title_ok && subject_index != -1 && date_ok) {
                        TransitionManager.beginDelayedTransition(root);
                        Assignment to_add = new Assignment(title, 0, description, picked[0], subject_index);
                        root.removeViewAt(0);
                        new_assignment_active = false;
                        addAssignment(to_add);
                    }
                }
            });

            new_assignment_view.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TransitionManager.beginDelayedTransition(root);
                    root.removeViewAt(0);
                    new_assignment_active = false;
                    if (assignments_list.size() == 0)
                        root.findViewById(R.id.no_assignments_message).setVisibility(View.VISIBLE);
                }
            });
            root.addView(new_assignment_view, 0);
        } else {
            Toast.makeText(getContext(), R.string.new_assignment_toast, Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }
}
