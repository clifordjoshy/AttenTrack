package com.leap.attentrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    static String time_table_file, assignments_file, sec_file, delim_sec = "/", shared_pref_name = "my_data";
    static Subject[] data;
    LinkedList<int[]> extra_sessions = new LinkedList<>(), cancelled_sessions = new LinkedList<>(),
            missed_sessions = new LinkedList<>();
    static boolean dark_mode_on, is_male_avatar, is_notification_on;
    private int current_fragment = 0;
    static String name;
    static int is_first_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        time_table_file = this.getFilesDir().getPath() + "/schedule_data_serial.txt";
        assignments_file = this.getFilesDir().getPath() + "/assignments_data_serial.txt";
        sec_file = "secondary.txt";
        is_first_start = getSharedPreferences(shared_pref_name, MODE_PRIVATE).getInt("first_start", -1);
        if (is_first_start == -1) {
            Intent intent = new Intent(this, StartupActivity.class);
            startActivityForResult(intent, 23);
            return;
        }

        try {
            get_data();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();

            //essential data is gone. restart :(
            getSharedPreferences(shared_pref_name, MODE_PRIVATE).edit().remove("first_start").apply();
            is_first_start = -1;    //don't put data
            recreate();
            return;     //stop loading current screen
        }

        setTheme(dark_mode_on ? R.style.DarkTheme : R.style.LightTheme);
        setContentView(R.layout.activity_main);

        //Implement Navigation Drawer and Fragments
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        if (is_notification_on)
            updateAlarmBroadcastReceiver(true);

        drawer = findViewById(R.id.drawer_layout);

        //dark mode smoothening
        if (getIntent().getBooleanExtra("is_mode_changed", false)) {
            drawer.openDrawer(GravityCompat.START, false);
            current_fragment = getIntent().getIntExtra("current_fragment", 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawer.post(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().setStatusBarColor(dark_mode_on ? 0xff101010 : 0xff666666);
                    }
                });
            }
        }

        NavigationView navView = findViewById(R.id.nav_view);
        MenuItem notif_item = navView.getMenu().findItem(R.id.notification_message);
        notif_item.setTitle(is_notification_on ? R.string.notif_on_menu : R.string.notif_off_menu);
        notif_item.setIcon(is_notification_on ? R.drawable.icon_notification_on : R.drawable.icon_notifications_off);

        navView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  //status bar changes at api23[light and dark needed]
            drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, final float slideOffset) {
                    drawerView.postOnAnimation(new Runnable() {     //on next animation timestep. for sync between both colour changes.
                        @Override
                        public void run() {
                            int start_val = dark_mode_on ? 39 : 255, end_val = dark_mode_on ? 16 : 102;
                            int color_val = (int) (start_val - (start_val - end_val) * slideOffset);
                            getWindow().setStatusBarColor(Color.rgb(color_val, color_val, color_val));
                        }
                    });
                }
            });
        }
        toggle.syncState();

        MobileAds.initialize(this);

        if (savedInstanceState != null)
            current_fragment = savedInstanceState.getInt("current_fragment", 0);

        Fragment go_to = null;
        switch (current_fragment) {
            case 0:
                go_to = new ScheduleFragment();
                navView.getMenu().findItem(R.id.schedule_message).setChecked(true);
                break;
            case 1:
                go_to = new TimetableFragment();
                navView.getMenu().findItem(R.id.time_table_message).setChecked(true);
                break;
            case 2:
                go_to = new EditStatsFragment();
                navView.getMenu().findItem(R.id.edit_stats_message).setChecked(true);
                break;
            case 3:
                go_to = new AssignmentsFragment();
                navView.getMenu().findItem(R.id.assignments_message).setChecked(true);
                break;
            case 4:
                go_to = new SupportFragment();
                navView.getMenu().findItem(R.id.support_message).setChecked(true);
                break;
        }

        //fragments persist rotation
        if (getSupportFragmentManager().findFragmentByTag("current_fragment") == null)
            getSupportFragmentManager().
                    beginTransaction().
                    replace(R.id.fragment_container, go_to, "current_fragment").
                    commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (is_first_start != -1)    //going to startup activity
            put_data();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (is_first_start != -1)    //in first startup activity
            put_data();
    }

    void put_data() {
        try {
            FileOutputStream fos = new FileOutputStream(time_table_file);
            ObjectOutputStream outputStream = new ObjectOutputStream(fos);
            outputStream.writeObject(data);
            outputStream.close();
            fos.close();

            fos = new FileOutputStream(assignments_file);
            outputStream = new ObjectOutputStream(fos);
            outputStream.writeObject(AssignmentsFragment.assignments_list);
            outputStream.close();
            fos.close();

            SharedPreferences sp = getSharedPreferences(shared_pref_name, MODE_PRIVATE);
            SharedPreferences.Editor sp_editor = sp.edit();
            sp_editor.putInt("req_percent", Subject.req_percentage);
            sp_editor.putBoolean("dark_mode_on", dark_mode_on);
            sp_editor.putBoolean("monday_start", true);
            sp_editor.putString("username", name);
            sp_editor.putBoolean("is_male_avatar", is_male_avatar);
            sp_editor.putBoolean("notifs_on", is_notification_on);
            sp_editor.apply();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(sec_file, MODE_PRIVATE));
            StringBuilder to_write = new StringBuilder("session_encoder");
            for (String[] sess : Subject.session_encoder)
                to_write.append(delim_sec).append(sess[0]).append("-").append(sess[1]);
            to_write.append("\n");
            outputStreamWriter.write(to_write.toString());

            for (int[] sess : extra_sessions) {
                to_write = new StringBuilder("extra");
                for (int val : sess)
                    to_write.append("/").append(val);
                to_write.append("\n");
                outputStreamWriter.write(to_write.toString());
            }

            for (int[] sess : cancelled_sessions) {
                to_write = new StringBuilder("cancel");
                for (int val : sess)
                    to_write.append("/").append(val);
                to_write.append("\n");
                outputStreamWriter.write(to_write.toString());
            }

            for (int[] sess : missed_sessions) {
                to_write = new StringBuilder("missed");
                for (int val : sess)
                    to_write.append("/").append(val);
                to_write.append("\n");
                outputStreamWriter.write(to_write.toString());
            }
            outputStreamWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void get_data() throws IOException, ClassNotFoundException {

        FileInputStream fis = new FileInputStream(time_table_file);
        ObjectInputStream inputStream = new ObjectInputStream(fis);
        data = (Subject[]) inputStream.readObject();
        inputStream.close();
        fis.close();

        try {       //can work without assignments. so dealt with exceptions
            fis = new FileInputStream(assignments_file);
            inputStream = new ObjectInputStream(fis);
            AssignmentsFragment.assignments_list = (LinkedList<AssignmentsFragment.Assignment>) inputStream.readObject();
            inputStream.close();
            fis.close();
        } catch (IOException ignored) {
        }

        InputStream sec_input = openFileInput(sec_file);
        InputStreamReader reader = new InputStreamReader(sec_input);
        BufferedReader buffer_read = new BufferedReader(reader);

        String receiveString;

        while ((receiveString = buffer_read.readLine()) != null) {
            String[] line = receiveString.split(delim_sec);
            if ("session_encoder".equals(line[0])) {
                LinkedList<String[]> vals = new LinkedList<>();
                for (int i = 1; i < line.length; ++i)
                    vals.add(line[i].split("-"));
                Subject.session_encoder = new String[vals.size()][];
                for (int i = 0; i < vals.size(); ++i)
                    Subject.session_encoder[i] = vals.get(i);

            } else if ("extra".equals(line[0])) {
                int[] vals = new int[5];   //year, month, day, session, subject
                for (int i = 0; i < 5; ++i)
                    vals[i] = Integer.parseInt(line[i + 1]);
                extra_sessions.add(vals);

            } else if ("cancel".equals(line[0])) {
                int[] vals = new int[5];   //year, month, day, session, subject
                for (int i = 0; i < 5; ++i)
                    vals[i] = Integer.parseInt(line[i + 1]);
                cancelled_sessions.add(vals);

            } else if ("missed".equals(line[0])) {
                int[] vals = new int[5];   //year, month, day, session, subject
                for (int i = 0; i < 5; ++i)
                    vals[i] = Integer.parseInt(line[i + 1]);
                missed_sessions.add(vals);
            }
        }

        if(Subject.session_encoder == null)
            throw new IOException("Session Encoder Lost");

        try {
            SimpleDateFormat string_format = new SimpleDateFormat("yyyy-MM-dd");
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Date today = now.getTime();

            for (LinkedList<int[]> sess_list : new LinkedList[]{extra_sessions, cancelled_sessions, missed_sessions}) {
                //need to traverse backwards to prevent element upward shift
                for (int i = sess_list.size() - 1; i >= 0; --i) {
                    int[] sess = sess_list.get(i);
                    String month = (sess[1] < 9 ? "0" : "") + sess[1];
                    String day = (sess[2] < 9 ? "0" : "") + sess[2];
                    Date saved = string_format.parse(sess[0] + "-" + month + "-" + day);
                    if (today.after(saved)) {
                        sess_list.remove(i);
                    }
                }
            }
        } catch (ParseException ignored) {
        }  //properly formatted

        SharedPreferences sp = getSharedPreferences(shared_pref_name, MODE_PRIVATE);
        Subject.req_percentage = sp.getInt("req_percent", 75);
        dark_mode_on = sp.getBoolean("dark_mode_on", false);
        name = sp.getString("username", getString(R.string.default_username));
        is_male_avatar = sp.getBoolean("is_male_avatar", false);
        is_notification_on = sp.getBoolean("notifs_on", true);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.schedule_message:
                item.setChecked(true);
                current_fragment = 0;
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new ScheduleFragment()).commit();
                break;
            case R.id.time_table_message:
                item.setChecked(true);
                current_fragment = 1;
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new TimetableFragment()).commit();
                break;

            case R.id.edit_stats_message:
                item.setChecked(true);
                current_fragment = 2;
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new EditStatsFragment()).commit();
                break;

            case R.id.assignments_message:
                item.setChecked(true);
                current_fragment = 3;
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new AssignmentsFragment()).commit();
                break;

            case R.id.dark_message:
                dark_mode_on = !dark_mode_on;
                TransitionManager.beginDelayedTransition((DrawerLayout) findViewById(R.id.drawer_layout));
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("is_mode_changed", true);
                intent.putExtra("current_fragment", current_fragment);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_in_400, R.anim.fade_out_400);
                return true;     //don't close drawer

            case R.id.notification_message:
                is_notification_on = !is_notification_on;
                NavigationView navView = findViewById(R.id.nav_view);
                MenuItem notif_item = navView.getMenu().findItem(R.id.notification_message);
                notif_item.setTitle(is_notification_on ? R.string.notif_on_menu : R.string.notif_off_menu);
                notif_item.setIcon(is_notification_on ? R.drawable.icon_notification_on : R.drawable.icon_notifications_off);
                updateAlarmBroadcastReceiver(is_notification_on);
                return true;    //don't close drawer

            case R.id.start_new_semester_message: {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                dialog.setTitle(R.string.warning_title);
                dialog.setMessage(R.string.new_sem_warning);

                dialog.setPositiveButton(R.string.confirm_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, StartupActivity.class);
                        intent.putExtra("mode", 1);
                        startActivityForResult(intent, 31);
                    }
                });
                dialog.setNegativeButton(R.string.cancel_text, null);
                dialog.show();
                break;
            }

            case R.id.reset_message:
                AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                dialog.setTitle(R.string.warning_title);
                dialog.setMessage(R.string.reset_warning);

                dialog.setPositiveButton(R.string.confirm_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, StartupActivity.class);
                        intent.putExtra("mode", 2);
                        startActivityForResult(intent, 31);
                    }
                });
                dialog.setNegativeButton(R.string.cancel_text, null);
                dialog.show();
                break;

            case R.id.support_message:
                current_fragment = 4;
                item.setChecked(true);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new SupportFragment()).commit();
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void updateAlarmBroadcastReceiver(boolean state) {

        Intent _intent = new Intent(this, AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, _intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (state) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 17);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.DATE, 1);     //postpone by a day when app opened.

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        } else
            alarmManager.cancel(pendingIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 29) {
            cancelled_sessions.clear();
            extra_sessions.clear();
            missed_sessions.clear();
            AssignmentsFragment.assignments_list.clear();
            recreate();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("current_fragment", current_fragment);
        super.onSaveInstanceState(outState);
    }
}
