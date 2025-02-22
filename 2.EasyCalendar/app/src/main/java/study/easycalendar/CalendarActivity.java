package study.easycalendar;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;

import study.easycalendar.adapter.CalendarAdapter;
import study.easycalendar.databinding.ActivityCalendarBinding;
import study.easycalendar.list.ListActivity;
import study.easycalendar.model.CalendarViewModel;
import study.easycalendar.model.ScheduleViewModel;

public class CalendarActivity extends AppCompatActivity implements CalendarViewModel.CalendarNavigator, NavigationView.OnNavigationItemSelectedListener {

    private ActivityCalendarBinding binding;
    private CalendarAdapter calendarAdapter;
    private CalendarViewModel calendarViewModel;
    private long lastTimeBackPressed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_calendar);
        calendarViewModel = ViewModelProviders.of(this).get(CalendarViewModel.class);
        binding.setCalendarViewModel(calendarViewModel);
        calendarViewModel.setNavigator(this);
        setSupportActionBar(binding.contentCalendar.toolbar);
        calendarAdapter = new CalendarAdapter();
        binding.contentCalendar.schedule.setLayoutManager(new LinearLayoutManager(this));
        binding.contentCalendar.schedule.setAdapter(calendarAdapter);
        binding.contentCalendar.calendar.setCurrentDate(CalendarDay.today());
        binding.contentCalendar.calendar.setDateSelected(CalendarDay.today(), true);
        binding.contentCalendar.calendar.setOnDateChangedListener(calendarViewModel);
        calendarViewModel.getSchedules(LocalDate.now()).observe(this, newSchedules -> calendarAdapter.setData(newSchedules));
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.contentCalendar.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        binding.navView.setNavigationItemSelectedListener(this);
        binding.contentCalendar.fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailActivity.class);
//            intent.putExtra("date", binding.contentCalendar.calendar.getSelectedDate().toString());
            CalendarDay selectedDate = binding.contentCalendar.calendar.getSelectedDate();
            List<Integer> dateInfo = new ArrayList<Integer>();
            dateInfo.add(selectedDate.getYear());
            dateInfo.add(selectedDate.getMonth());
            dateInfo.add(selectedDate.getDay());
            intent.putIntegerArrayListExtra("date", (ArrayList<Integer>)dateInfo);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        calendarViewModel.getAllSchedules().observe(this, schedules -> {
            calendarViewModel.setData(schedules);
            binding.contentCalendar.calendar.addDecorator(calendarViewModel);
        });
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (System.currentTimeMillis() - lastTimeBackPressed < 1500) {
                finishAffinity();
                return;
            }
            Toast toast = Toast.makeText(this, "뒤로가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT);
            TextView v = toast.getView().findViewById(android.R.id.message);
            if (v != null) v.setGravity(Gravity.CENTER);
            toast.show();
            lastTimeBackPressed = System.currentTimeMillis();
        }
    }

    @Override
    public void onSelectedDayChange(LocalDate selectedDate) {
        calendarViewModel.getSchedules(selectedDate).observe(this, newSchedules -> calendarAdapter.setData(newSchedules));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent = null;

        switch (item.getItemId()) {

            case R.id.nav_schedule:
                intent = new Intent(this, ListActivity.class);
                break;
            case R.id.nav_dday:
                intent = new Intent(this, DdayActivity.class);
                break;
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        startActivity(intent);
        overridePendingTransition(0, 0);
//        finish();
        return true;
    }
}
