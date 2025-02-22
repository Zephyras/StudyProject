package study.easycalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import study.easycalendar.alarm.AlarmReceiver;
import study.easycalendar.model.Schedule;
import study.easycalendar.model.local.AppDatabase;
import study.easycalendar.model.local.DatabaseHandler;
import study.easycalendar.model.local.ScheduleDao;

public class DetailActivity extends AppCompatActivity {
    public static final String TAG = "[easy][Detail]";

    Toolbar toolbar;

    EditText edit_title;
    EditText edit_memo;

    Spinner spinner_category;
    Spinner spinner_repeat;
    Spinner spinner_notification;

    CheckBox checkBox_dday;

    // Calendar
    Calendar calendar;
    DatePicker picker_start_date;
    DatePicker picker_end_date;
    TimePicker picker_start_time;
    TimePicker picker_end_time;

    DatePicker picker_dday_date;
    TextView text_dday_date;

    LocalDate selectedDate;

    Schedule scheduleInfo = null;
    int scheduleId = -1;

    boolean condition = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        scheduleId = intent.getIntExtra("id", -1);
        Log.d(TAG, "id : " + scheduleId);
        //intent date로 date 값 보냅니다.
//        String scheduleDate = intent.getStringExtra("date");
//        Log.d(TAG, "date : " + scheduleDate);
        ArrayList<Integer> dateInfo = intent.getIntegerArrayListExtra("date");
        int year = dateInfo.get(0);
        int month = dateInfo.get(1);
        int day = dateInfo.get(2);
        Log.d(TAG, "date : " + year + "-" + month + "-" + day);

        selectedDate = LocalDate.of(year, month, day);

        Thread threadSchedule = null;

        if (scheduleId != -1) {
            Log.d(TAG, "onCreate scheduleId != -1");

            // id 조회
            ScheduleDao scheduleDao = AppDatabase.getInstance(getApplicationContext()).scheduleDao();
            threadSchedule = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        Log.d(TAG, "Thread RUN start");
                        scheduleInfo = scheduleDao.getSchedule(scheduleId);

                        Log.d(TAG, "schedule (id: " + scheduleInfo.id + ", title: " + scheduleInfo.title + ", memo: " + scheduleInfo.memo + ")");

                        setTitle("상세일정");

//                        notifyAll();
                        condition = true;
                        Log.d(TAG, "Thread notify");

                        initEdit();
                        initSpinner();
                        initDateTime();
                        initFAB();
//                        initAlarm();
                    }
                }
            });

            threadSchedule.start();
        } else {
            Log.d(TAG, "onCreate scheduleId == -1");
            initEdit();
            initSpinner();
            initDateTime();
            initFAB();
//            initAlarm();
        }

//        synchronized (threadSchedule) {
//
//            try {
//                Log.d(TAG, "Thread WAIT start");
//                threadSchedule.wait();
//                Log.d(TAG, "Thread WAIT end");
//
//                initEdit();
//                initSpinner();
//                initDateTime();
//                initDday();
//                initFAB();
//
//                drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//                drawer.addDrawerListener(toggle);
//                toggle.syncState();
//
//                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//                navigationView.setNavigationItemSelectedListener(this);
//            }catch(InterruptedException e){
//                e.printStackTrace();
//            }
//        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(getApplicationContext(), "Setting 기능 추가하기", Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void initEdit() {
        Log.d(TAG, "initEdit start");

        edit_title = (EditText) findViewById(R.id.edit_title);
        edit_memo = (EditText) findViewById(R.id.edit_memo);

        Log.d(TAG, "scheduleId : " + scheduleId);
        if (scheduleId != -1) {
            if (scheduleInfo != null) {
                Log.d(TAG, "scheduleInfo : " + scheduleInfo.toString());
                edit_title.setText(scheduleInfo.title);
                edit_memo.setText(scheduleInfo.memo);

//                edit_title.setEnabled(false);
//                edit_memo.setEnabled(false);
            } else {
                Log.d(TAG, "scheduleInfo : null");
            }
        }

        edit_title.post(new Runnable() {
            @Override
            public void run() {
                edit_title.setFocusableInTouchMode(true);
                edit_title.requestFocus();

//                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.showSoftInput(edittext,0);
            }
        });
    }

    private void initSpinner() {
        spinner_category = (Spinner) findViewById(R.id.spinner_category);
        spinner_repeat = (Spinner) findViewById(R.id.spinner_repeat);
        spinner_notification = (Spinner) findViewById(R.id.spinner_notification);

        ArrayAdapter<CharSequence> adapterCategory = ArrayAdapter.createFromResource(
                this, R.array.category, android.R.layout.simple_spinner_item);
        adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_category.setAdapter(adapterCategory);
        spinner_category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(parent.getContext(), parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<CharSequence> adapterRepeat = ArrayAdapter.createFromResource(
                this, R.array.repeat, android.R.layout.simple_spinner_item);
        adapterRepeat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_repeat.setAdapter(adapterRepeat);
        spinner_repeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(parent.getContext(), parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<CharSequence> adapterNotification = ArrayAdapter.createFromResource(
                this, R.array.notification, android.R.layout.simple_spinner_item);
        adapterNotification.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_notification.setAdapter(adapterNotification);
        spinner_notification.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(parent.getContext(), parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (scheduleId != -1) {
            // 스케쥴 정보에 맞게 스피너 설정
            spinner_category.setSelection(adapterCategory.getPosition(scheduleInfo.category));
            spinner_repeat.setSelection(adapterRepeat.getPosition(scheduleInfo.repeat));
            spinner_notification.setSelection(adapterNotification.getPosition(scheduleInfo.notification));
        }
    }

    private void initDateTime() {
        picker_start_date = (DatePicker) findViewById(R.id.picker_start_date);
        picker_end_date = (DatePicker) findViewById(R.id.picker_end_date);
        picker_start_time = (TimePicker) findViewById(R.id.picker_start_time);
        picker_end_time = (TimePicker) findViewById(R.id.picker_end_time);

        picker_dday_date = (DatePicker) findViewById(R.id.picker_dday_date);
        text_dday_date = (TextView) findViewById(R.id.text_dday_date);

        calendar = Calendar.getInstance();
        int year = picker_start_date.getYear();
        int month = picker_start_date.getMonth() + 1;
        int day = picker_start_date.getDayOfMonth();
        int hour = calendar.get(calendar.HOUR_OF_DAY);
        int min = calendar.get(calendar.MINUTE);
        int sec = calendar.get(calendar.SECOND);

        Log.d(TAG, "현재 시간 (" + year + "년 " + month + "월 " + day + "일 " + hour + "시 " + min + "분 " + sec + "초)");

        if (scheduleId != -1) {
            // 스케쥴 정보에 맞게 date/time 설정
            picker_start_date.init(scheduleInfo.startDate.getYear(),
                    scheduleInfo.startDate.getMonthValue() - 1,
                    scheduleInfo.startDate.getDayOfMonth(),
                    null);
            picker_end_date.init(scheduleInfo.endDate.getYear(),
                    scheduleInfo.endDate.getMonthValue() - 1,
                    scheduleInfo.endDate.getDayOfMonth(),
                    null);

            int startHour, startMinute, endHour, endMinute;
            startHour = scheduleInfo.startTime.getHour();
            startMinute = scheduleInfo.startTime.getMinute();
            endHour = scheduleInfo.endTime.getHour();
            endMinute = scheduleInfo.endTime.getMinute();

            if (Build.VERSION.SDK_INT < 23) {
                picker_start_time.setCurrentHour(startHour);
                picker_start_time.setCurrentMinute(startMinute);
                picker_end_time.setCurrentHour(endHour);
                picker_end_time.setCurrentMinute(endMinute);
            } else {
                picker_start_time.setHour(startHour);
                picker_start_time.setMinute(startMinute);
                picker_end_time.setHour(endHour);
                picker_end_time.setMinute(endMinute);
            }
        } else {
            int curYear = selectedDate.getYear();
            int curMonth = selectedDate.getMonthValue() - 1;
            int curDay = selectedDate.getDayOfMonth();

            Log.d(TAG, "curDate : " + curYear + "-" + curMonth + "-" + curDay);

            picker_start_date.updateDate(curYear, curMonth, curDay);
            picker_end_date.updateDate(curYear, curMonth, curDay);
            picker_dday_date.updateDate(curYear, curMonth, curDay);
        }

        initDday();
    }

    private void initDday() {
        picker_dday_date.setEnabled(false);
        text_dday_date.setEnabled(false);

        checkBox_dday = (CheckBox) findViewById(R.id.checkBox_dday);
        checkBox_dday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // dday date enable
                    picker_dday_date.setEnabled(true);
                    text_dday_date.setEnabled(true);
                } else {
                    // dday date disable
                    picker_dday_date.setEnabled(false);
                    text_dday_date.setEnabled(false);
                }
            }
        });

        if (scheduleId != -1) {
            boolean bDay = scheduleInfo.isDday();

            if (bDay) {
                checkBox_dday.setChecked(bDay);

                picker_dday_date.setEnabled(bDay);
                text_dday_date.setEnabled(bDay);

                picker_dday_date.init(scheduleInfo.dDayDate.getYear(),
                        scheduleInfo.dDayDate.getMonthValue() - 1,
                        scheduleInfo.dDayDate.getDayOfMonth(),
                        null);
            }
        }
    }

    private void initFAB() {
        Log.d(TAG, "initFAB");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "initFAB onClick : title " + edit_title.getText().toString());

                if (edit_title.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "일정 타이틀을 입력하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    int repeat = 0;
                    switch (spinner_repeat.getSelectedItem().toString()) {
//                        case "매일":
//                        repeat = 1;
//                            break;
                        case "매주":
                            repeat = 2;
                            break;
                        case "매월":
                            repeat = 3;
                            break;
                        case "매년":
                            repeat = 4;
                            break;
                    }

                    if (scheduleId != -1) {
                        UpdateSchedule(view);
                    } else {
                        if (repeat == 0) {
                            InsertSchdule(view);
                        } else {
                            InsertSchduleList(view, repeat);
                        }
                    }

                }
            }
        });
    }

    // for test
    private void initAlarm() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(calendar.HOUR_OF_DAY);
        int min = calendar.get(calendar.MINUTE);
        int sec = calendar.get(calendar.SECOND);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec + 5);

        Intent alarmIntent = new Intent(DetailActivity.this, AlarmReceiver.class);
        alarmIntent.putExtra("state", "ALARM test");

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        DetailActivity.this,
                        1,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent);
    }

    private void SetAlarm(LocalDate date, LocalTime time, String notification, String title, int id) {
        // type
        // 1 (10분 전), 2(30분 전), 3(1시간 전), 4(1일 전), 5(7일 전)

        Calendar calendar = Calendar.getInstance();
        int year = date.getYear();
        int month = date.getMonthValue(); // 1~12
        int day = date.getDayOfMonth();
        int hour = time.getHour();
        int min = time.getMinute();
        int sec = time.getSecond();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1); // 0~11
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);

        Log.d(TAG, "SetAlarm " + notification);
        Log.d(TAG, "SetAlarm (notification: " + notification + ", title: " + title + ", id: " + id + ")");
        switch (notification) {
            case "10분 전":
                calendar.add(Calendar.MINUTE, -10);
                break;
            case "30분 전":
                calendar.add(Calendar.MINUTE, -30);
                break;
            case "1시간 전":
                calendar.add(Calendar.HOUR, -1);
                break;
            case "1일 전":
                calendar.add(Calendar.DATE, -1);
                break;
            case "7일 전":
                calendar.add(Calendar.DATE, -7);
                break;
        }

        Intent alarmIntent = new Intent(DetailActivity.this, AlarmReceiver.class);
        alarmIntent.putExtra("type", notification);
        alarmIntent.putExtra("title", title);
        alarmIntent.putExtra("id", id);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        DetailActivity.this,
                        1,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent);
    }

    private void InsertSchdule(View view) {
        Log.d(TAG, "InsertSchdule");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int startHour, startMinute, endHour, endMinute;
                if (Build.VERSION.SDK_INT < 23) {
                    startHour = picker_start_time.getCurrentHour();
                    startMinute = picker_start_time.getCurrentMinute();
                    endHour = picker_end_time.getCurrentHour();
                    endMinute = picker_end_time.getCurrentMinute();
                } else {
                    startHour = picker_start_time.getHour();
                    startMinute = picker_start_time.getMinute();
                    endHour = picker_end_time.getHour();
                    endMinute = picker_end_time.getMinute();
                }

                LocalDate startDate = LocalDate.of(picker_start_date.getYear(),
                        picker_start_date.getMonth() + 1,
                        picker_start_date.getDayOfMonth());
                LocalTime startTime = LocalTime.of(startHour, startMinute);
                LocalDate endDate = LocalDate.of(picker_end_date.getYear(),
                        picker_end_date.getMonth() + 1,
                        picker_end_date.getDayOfMonth());
                LocalTime endTime = LocalTime.of(endHour, endMinute);

                String title = edit_title.getText().toString();
                String memo = edit_memo.getText().toString();

                String category = spinner_category.getSelectedItem().toString();
                String notification = spinner_notification.getSelectedItem().toString();
                String repeat = spinner_repeat.getSelectedItem().toString();

                boolean bDday = checkBox_dday.isChecked();
                LocalDate ddayDate = LocalDate.of(picker_dday_date.getYear(),
                        picker_dday_date.getMonth() + 1,
                        picker_dday_date.getDayOfMonth());

                Schedule s = new Schedule(startDate, startTime, endDate, endTime, title, memo, category, notification, repeat, bDday, ddayDate);

                long newId;
                if (scheduleId != -1) {
                    Log.d(TAG, "UpdateSchdule");
                    DatabaseHandler.getInstance().updateSchedule(s);
                    newId = scheduleId;
                } else {
                    newId = DatabaseHandler.getInstance().insertSchedule(s);
                    Log.d(TAG, "InsertSchdule (scheduleId: " + scheduleId + ")");
                }

                if (!notification.equals("없음")) {
                    SetAlarm(startDate, startTime, notification, title, (int)newId);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(view, "저장 완료", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                        if (scheduleId != -1) {
                            Log.d(TAG, "UpdateSchdule Complete : Data (" + startDate + ", " + startTime + ", " + endDate + ", " + endTime + ", " +
                                    title + ", " + memo + ", " + category + ", " + notification + ", " + repeat + ", " + (bDday ? "D-day checked" : "D-day not Checked") + ", " + ddayDate + ")");
                        } else {
                            Log.d(TAG, "InsertSchdule Complete : Data (" + startDate + ", " + startTime + ", " + endDate + ", " + endTime + ", " +
                                    title + ", " + memo + ", " + category + ", " + notification + ", " + repeat + ", " + (bDday ? "D-day checked" : "D-day not Checked") + ", " + ddayDate + ")");
                        }

//                        Toast.makeText(DetailActivity.this, "INSERT COMPLETE", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        }).start();
    }

    private void InsertSchduleList(View view, int repeatType) {
        Log.d(TAG, "InsertSchduleList" +
                "");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int startHour, startMinute, endHour, endMinute;
                if (Build.VERSION.SDK_INT < 23) {
                    startHour = picker_start_time.getCurrentHour();
                    startMinute = picker_start_time.getCurrentMinute();
                    endHour = picker_end_time.getCurrentHour();
                    endMinute = picker_end_time.getCurrentMinute();
                } else {
                    startHour = picker_start_time.getHour();
                    startMinute = picker_start_time.getMinute();
                    endHour = picker_end_time.getHour();
                    endMinute = picker_end_time.getMinute();
                }

                LocalDate startDate = LocalDate.of(picker_start_date.getYear(), picker_start_date.getMonth() + 1, picker_start_date.getDayOfMonth());
                LocalTime startTime = LocalTime.of(startHour, startMinute);
                LocalDate endDate = LocalDate.of(picker_end_date.getYear(), picker_end_date.getMonth() + 1, picker_end_date.getDayOfMonth());
                LocalTime endTime = LocalTime.of(endHour, endMinute);

                String title = edit_title.getText().toString();
                String memo = edit_memo.getText().toString();

                String category = spinner_category.getSelectedItem().toString();
                String notification = spinner_notification.getSelectedItem().toString();
                String repeat = spinner_repeat.getSelectedItem().toString();

                boolean bDday = checkBox_dday.isChecked();
                LocalDate ddayDate = LocalDate.of(picker_dday_date.getYear(),
                        picker_dday_date.getMonth() + 1,
                        picker_dday_date.getDayOfMonth());

                List<Schedule> list = new ArrayList<Schedule>();
                Schedule s = new Schedule(startDate, startTime, endDate, endTime, title, memo, category, notification, repeat, bDday, ddayDate);
                list.add(s);

                int s_year, s_month, s_day;
                // 매일, 매주, 매월, 매년
                /*if (repeatType == 1) {

                } else*/
                if (repeatType == 2) { // 매주, 1년
                    s = new Schedule(startDate, startTime, endDate, endTime, title, memo, category, notification, repeat, bDday, null);

                } else if (repeatType == 3) { // 매월, 5년
                    s_year = picker_start_date.getYear();
                    s_month = picker_start_date.getMonth() + 1;
                    s_day = picker_start_date.getDayOfMonth();
                    for (int i = 0; i < 12 * 5; i++, s_month++) {
                        if (s_month > 12) {
                            s_month -= 12;
                            s_year += 1;
                        }
                        startDate = LocalDate.of(s_year, s_month, s_day);
                        s = new Schedule(startDate, startTime, endDate, endTime, title, memo, category, notification, repeat, bDday, null);

                        Log.d(TAG, "Data (" + startDate + ", " + startTime + ", " + endDate + ", " + endTime + ", " + title + ", " + memo + ", " + category + ", " + notification + ", " + repeat + ", " + (bDday ? "D-day checked" : "D-day not Checked") + ")");
                        list.add(s);
                    }
                } else if (repeatType == 4) { // 매년, 10년
                    s_year = picker_start_date.getYear();
                    s_month = picker_start_date.getMonth() + 1;
                    s_day = picker_start_date.getDayOfMonth();
                    for (int i = 0; i < 10; i++) {
                        s_year += 1;

                        startDate = LocalDate.of(s_year, s_month, s_day);
                        s = new Schedule(startDate, startTime, endDate, endTime, title, memo, category, notification, repeat, bDday, null);

                        Log.d(TAG, "Data (" + startDate + ", " + startTime + ", " + endDate + ", " + endTime + ", " + title + ", " + memo + ", " + category + ", " + notification + ", " + repeat + ", " + (bDday ? "D-day checked" : "D-day not Checked") + ")");
                        list.add(s);
                    }
                }

                //                DatabaseHandler.getInstance().insertSchedule(s);
                DatabaseHandler.getInstance().insertScheduleList(list);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(view, "저장 완료", Snackbar.LENGTH_LONG).setAction("Action", null).show();
//                        Log.d(TAG, "InsertSchdule Complete : Data (" + startDate + ", " + startTime + ", " + endDate + ", " + endTime + ", " +
//                                title + ", " + memo + ", " + category + ", " + notification + ", " + repeat + ", " + (bDday ? "D-day checked" : "D-day not Checked") + ")");

//                        Toast.makeText(DetailActivity.this, "INSERT COMPLETE", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        }).start();
    }


    private void UpdateSchedule(View view) {
        Log.d(TAG, "UpdateSchedule");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int startHour, startMinute, endHour, endMinute;
                if (Build.VERSION.SDK_INT < 23) {
                    startHour = picker_start_time.getCurrentHour();
                    startMinute = picker_start_time.getCurrentMinute();
                    endHour = picker_end_time.getCurrentHour();
                    endMinute = picker_end_time.getCurrentMinute();
                } else {
                    startHour = picker_start_time.getHour();
                    startMinute = picker_start_time.getMinute();
                    endHour = picker_end_time.getHour();
                    endMinute = picker_end_time.getMinute();
                }

                LocalDate startDate = LocalDate.of(picker_start_date.getYear(),
                        picker_start_date.getMonth() + 1,
                        picker_start_date.getDayOfMonth());
                LocalTime startTime = LocalTime.of(startHour, startMinute);
                LocalDate endDate = LocalDate.of(picker_end_date.getYear(),
                        picker_end_date.getMonth() + 1,
                        picker_end_date.getDayOfMonth());
                LocalTime endTime = LocalTime.of(endHour, endMinute);

                String title = edit_title.getText().toString();
                String memo = edit_memo.getText().toString();

                String category = spinner_category.getSelectedItem().toString();
                String notification = spinner_notification.getSelectedItem().toString();
                String repeat = spinner_repeat.getSelectedItem().toString();

                boolean bDday = checkBox_dday.isChecked();
                LocalDate ddayDate = LocalDate.of(picker_dday_date.getYear(),
                        picker_dday_date.getMonth() + 1,
                        picker_dday_date.getDayOfMonth());

//                Schedule s = new Schedule(startDate, startTime, endDate, endTime, title, memo,
//                                          category, notification, repeat, bDday, ddayDate);
                scheduleInfo.setStartDate(startDate);
                scheduleInfo.setStartTime(startTime);
                scheduleInfo.setEndDate(endDate);
                scheduleInfo.setEndTime(endTime);
                scheduleInfo.setTitle(title);
                scheduleInfo.setMemo(memo);
                scheduleInfo.setCategory(category);
                scheduleInfo.setNotification(notification);
                scheduleInfo.setRepeat(repeat);
                scheduleInfo.setDday(bDday);
                scheduleInfo.setdDayDate(ddayDate);

                DatabaseHandler.getInstance().updateSchedule(scheduleInfo);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(view, "저장 완료", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                        Log.d(TAG, "UpdateSchdule Complete : Data (" + startDate + ", " + startTime + ", " + endDate + ", " + endTime + ", " +
                                title + ", " + memo + ", " + category + ", " + notification + ", " + repeat + ", " + (bDday ? "D-day checked" : "D-day not Checked") + ", " + ddayDate + ")");

                        finish();
                    }
                });
            }
        }).start();
    }

}
