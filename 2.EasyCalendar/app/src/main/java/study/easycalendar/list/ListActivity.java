package study.easycalendar.list;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import study.easycalendar.DdayActivity;
import study.easycalendar.DetailActivity;
import study.easycalendar.R;
import study.easycalendar.RecyclerItemClickListener;
import study.easycalendar.adapter.ListAdapter;
import study.easycalendar.model.Schedule;
import study.easycalendar.model.local.AppDatabase;
import study.easycalendar.model.local.DatabaseHandler;
import study.easycalendar.model.local.ScheduleDao;

public class ListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ScheduleDao dao;
    private ArrayList<Schedule> arrayList;
    private ListAdapter adapter;
    private Schedule schedule;
    DatabaseHandler databaseHandler;
    private List<Schedule> scheduleListFromDB = new ArrayList<Schedule>();
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        executorService = Executors.newSingleThreadExecutor();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.schedule_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new MyItemDecoration());

        dao = AppDatabase.getInstance(this).scheduleDao();

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this,
                recyclerView, new RecyclerItemClickListener.OnItemClickListener() {

            @Override
            public void onItemClick(View view, int position) {
                schedule = arrayList.get(position);
                Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
                intent.putExtra("id", schedule.id);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {
                schedule = arrayList.get(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(ListActivity.this);
                builder.setTitle("삭제");
                builder.setMessage("삭제하시겠습니까?");
                builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //executorService 이용
//                        executorService.execute(() -> dao.deleteSchedule(schedule));

                        //DatabaseHandler 이용
                        DatabaseHandler.getInstance().deleteSchedule(schedule);

                        //삭제 후 리사이클러뷰 업데이트
                        loadNotes();
                        adapter.notifyDataSetChanged();

                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            }
        }));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return true;
    }

    private void loadNotes() {
        arrayList = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                scheduleListFromDB = DatabaseHandler.getInstance().getSchedulesList();
                arrayList.addAll(scheduleListFromDB);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }).start();

        adapter = new ListAdapter(this, arrayList);
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }


    class MyItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        //항목하나하나당 한번씩 호출되서, 항목 하나하나를 다양하게 구현
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
        }
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
        } else if( id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

}
