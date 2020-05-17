package com.leap.attentrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    static String time_table_file, sec_file, delim_sec = "/", shared_pref_name = "my_data";
    static LinkedList<Subject> data;
    LinkedList<int[]> extra_sessions = new LinkedList<>(), cancelled_sessions = new LinkedList<>(),
            missed_sessions = new LinkedList<>();
    static boolean dark_mode_on, is_male_avatar, is_notification_on;
    static String name;
    static int is_first_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        time_table_file = this.getFilesDir().getPath() + "/schedule_data_serial.txt";
        sec_file = "secondary.txt";
        is_first_start = getSharedPreferences(shared_pref_name, MODE_PRIVATE).getInt("first_start", -1);
        if (is_first_start == -1) {
            Intent intent = new Intent(this, StartupActivity.class);
            startActivityForResult(intent, 23);
            return;
        }

        get_data();
        setTheme(dark_mode_on ? R.style.DarkTheme : R.style.LightTheme);
        setContentView(R.layout.activity_main);

        //Implement Navigation Drawer and Fragments
        Toolbar toolbar = findViewById(R.id.toolbar);
        ViewCompat.setElevation(toolbar,0);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        if (is_notification_on)
            updateAlarmBroadcastReceiver(true);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        MenuItem notif_item = navView.getMenu().findItem(R.id.notification_message);
        notif_item.setTitle(is_notification_on ? R.string.notif_on_menu : R.string.notif_off_menu);
        notif_item.setIcon(is_notification_on ? R.drawable.icon_notification_on : R.drawable.icon_notifications_off);
        navView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        MobileAds.initialize(this);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new ScheduleFragment()).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        put_data();
    }

    void put_data() {
        try {
            FileOutputStream fos = new FileOutputStream(time_table_file);
            ObjectOutputStream outputStream = new ObjectOutputStream(fos);
            outputStream.writeObject(data);
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
            Log.i("mylog", "Writing Error: " + e.getMessage());
        }
    }

    void get_data() {
        try {
            FileInputStream fis = new FileInputStream(time_table_file);
            ObjectInputStream inputStream = new ObjectInputStream(fis);
            data = (LinkedList<Subject>) inputStream.readObject();
            inputStream.close();
            fis.close();

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
                    Subject.session_encoder = vals;

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

            SimpleDateFormat string_format = new SimpleDateFormat("yyyy-MM-dd");
            Date today = Calendar.getInstance().getTime();
            today = string_format.parse(string_format.format(today));   //to get rid of time

            //need to traverse backwards to prevent element upward shift
            for (int i = extra_sessions.size() - 1; i >= 0; --i) {
                int[] sess = extra_sessions.get(i);
                String month = (sess[1] < 9 ? "0" : "") + sess[1];
                String day = (sess[2] < 9 ? "0" : "") + sess[2];
                Date saved = string_format.parse(sess[0] + "-" + month + "-" + day);
                if (today.after(saved)) {
                    extra_sessions.remove(i);
                }
            }

            for (int i = cancelled_sessions.size() - 1; i >= 0; --i) {
                int[] sess = cancelled_sessions.get(i);
                String month = (sess[1] < 9 ? "0" : "") + sess[1];
                String day = (sess[2] < 9 ? "0" : "") + sess[2];
                Date saved = string_format.parse(sess[0] + "-" + month + "-" + day);
                if (today.after(saved)) {
                    cancelled_sessions.remove(i);
                }
            }

            for (int i = missed_sessions.size() - 1; i >= 0; --i) {
                int[] sess = missed_sessions.get(i);
                String month = (sess[1] < 9 ? "0" : "") + sess[1];
                String day = (sess[2] < 9 ? "0" : "") + sess[2];
                Date saved = string_format.parse(sess[0] + "-" + month + "-" + day);
                if (today.after(saved)) {
                    missed_sessions.remove(i);
                }
            }

            SharedPreferences sp = getSharedPreferences(shared_pref_name, MODE_PRIVATE);
            Subject.req_percentage = sp.getInt("req_percent", 75);
            dark_mode_on = sp.getBoolean("dark_mode_on", false);
            name = sp.getString("username", "Spectacular User");
            is_male_avatar = sp.getBoolean("is_male_avatar", false);
            is_notification_on = sp.getBoolean("notifs_on", true);

        } catch (Exception e) {
            e.printStackTrace();
            Log.i("mylog", "Reading Error: " + e.getMessage());
        }
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
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new ScheduleFragment()).commit();
                break;
            case R.id.time_table_message:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new TimetableFragment()).commit();
                break;
            case R.id.missed_message:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new AllClassesFragment()).commit();
                break;
            case R.id.dark_message:
                dark_mode_on = !dark_mode_on;
                recreate();
                break;
            case R.id.notification_message:
                is_notification_on = !is_notification_on;
                NavigationView navView = findViewById(R.id.nav_view);
                MenuItem notif_item = navView.getMenu().findItem(R.id.notification_message);
                notif_item.setTitle(is_notification_on ? R.string.notif_on_menu : R.string.notif_off_menu);
                notif_item.setIcon(is_notification_on ? R.drawable.icon_notification_on : R.drawable.icon_notifications_off);
//                Toast.makeText(this, "Notifications " + (is_notification_on ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
                updateAlarmBroadcastReceiver(is_notification_on);
                return true;    //don't close drawer
            case R.id.reset_message:
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.warning_title);
                dialog.setMessage(R.string.reset_warning);

                dialog.setCancelable(true);
                dialog.setPositiveButton("SUBJECT RESET", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, StartupActivity.class);
                        intent.putExtra("mode", 1);
                        startActivityForResult(intent, 31);
                    }
                });
                dialog.setNegativeButton("COMPLETE RESET", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, StartupActivity.class);
                        intent.putExtra("mode", 2);
                        startActivityForResult(intent, 31);
                    }
                });

                ((TextView)dialog.show().findViewById(android.R.id.message)).
                        setTypeface(ResourcesCompat.getFont(this, R.font.poppins_regular));
                //Editing Typeface after dialog is shown. dialog.show() returns an AlertDialog
                break;
            case R.id.support_message:
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
            calendar.setTimeInMillis(System.currentTimeMillis());
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
            if (requestCode == 31) {    //reset button press
                cancelled_sessions.clear();
                extra_sessions.clear();
                missed_sessions.clear();
            }
            recreate();
        }
    }
}
