package study.easycalendar.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import study.easycalendar.R;
import study.easycalendar.model.Schedule;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<Schedule> arrayList = new ArrayList<>();
    private Schedule schedule;
    // 생성자
    public ListAdapter(Context context, ArrayList<Schedule> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

//    @Override
//    public int getItemViewType(int position) {
//        return super.getItemViewType(position);
//        return arrayList.get(position).get
//    }

    // ViewGroup 인 parent 클래스에서 context 를 받고, inflate 하여 새로운 View 객체를 생성하고, 뷰홀더를 생성한다
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new MyViewHolder(itemView);
    }

    // holder 의 View 안의 데이터를 변경하는 작업을 한다
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        final Schedule schedule = arrayList.get(position);

        holder.title.setText(schedule.getTitle());
        holder.memo.setText(schedule.getMemo());
        holder.date.setText(schedule.getStartDate().toString());
        holder.day.setText(schedule.getStartDate().toString().substring(8, 10)+"일");
    }
//
//    public void setSchedule(Schedule schedule) {
//        this.schedule = schedule;
//    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    private Schedule getItem(int position) {
        return arrayList.get(position);
    }

    // findViewByID 기능
    class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title, memo, date, day;

        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            memo = view.findViewById(R.id.memo);
            date = view.findViewById(R.id.startDate);
            day = view.findViewById(R.id.day);

//            int position = getAdapterPosition();
//
//            Log.w("RRRRR", String.valueOf(position));
//
//            schedule = arrayList.get(position);
//
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Intent intent = new Intent(context, DetailActivity.class);
//                    intent.putExtra("id", schedule.getId());
//                    context.startActivity(intent);
//                }
//            });
        }
    }
}
