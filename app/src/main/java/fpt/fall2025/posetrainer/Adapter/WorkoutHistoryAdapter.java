package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import fpt.fall2025.posetrainer.Activity.SessionResultActivity;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.R;

/**
 * Adapter để hiển thị danh sách lịch sử tập luyện trong WorkoutHistoryActivity
 */
public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.WorkoutHistoryViewHolder> {
    private ArrayList<Session> sessions;
    private Context context;

    public WorkoutHistoryAdapter(ArrayList<Session> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
    }

    /**
     * Cập nhật danh sách sessions mới
     */
    public void updateSessions(ArrayList<Session> newSessions) {
        this.sessions = newSessions != null ? newSessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkoutHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_workout_history, parent, false);
        return new WorkoutHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutHistoryViewHolder holder, int position) {
        Session session = sessions.get(position);
        
        // Hiển thị title của session
        if (session.getTitle() != null && !session.getTitle().isEmpty()) {
            holder.titleTxt.setText(session.getTitle());
        } else {
            holder.titleTxt.setText("Buổi tập " + (position + 1));
        }
        
        // Hiển thị thời gian bắt đầu (giờ:phút)
        if (session.getStartedAt() > 0) {
            // Convert seconds to milliseconds for Date constructor
            Date date = new Date(session.getStartedAt() * 1000);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            
            // Format giờ:phút
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.timeTxt.setText(timeFormat.format(date));
            
            // Format ngày tháng (ví dụ: "7 Th10")
            SimpleDateFormat dateFormat = new SimpleDateFormat("d 'Th'MM", Locale.getDefault());
            holder.dateTxt.setText(dateFormat.format(date));
        } else {
            holder.timeTxt.setText("--:--");
            holder.dateTxt.setText("--");
        }
        
        // Hiển thị thời lượng (duration)
        int durationMinutes = 0;
        if (session.getSummary() != null && session.getSummary().getDurationSec() > 0) {
            durationMinutes = session.getSummary().getDurationSec() / 60;
        } else if (session.getEndedAt() > 0 && session.getStartedAt() > 0) {
            // Fallback: tính từ startedAt và endedAt (cả hai đều là seconds)
            long durationSec = session.getEndedAt() - session.getStartedAt();
            durationMinutes = (int) (durationSec / 60);
        }
        holder.durationTxt.setText(durationMinutes + " phút Thời gian");
        
        // Hiển thị calories
        int calories = 0;
        if (session.getSummary() != null) {
            calories = session.getSummary().getEstKcal();
        }
        holder.caloriesTxt.setText(calories + " Calo");
        
        // Hiển thị thumbnail (sử dụng ảnh mặc định)
        holder.thumbnailImg.setImageResource(R.drawable.pic_1);
        
        // Xử lý click vào item để mở SessionResultActivity (chỉ hiển thị kết quả)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SessionResultActivity.class);
            intent.putExtra("sessionId", session.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    /**
     * ViewHolder cho WorkoutHistoryAdapter
     */
    public static class WorkoutHistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImg;
        TextView titleTxt, timeTxt, dateTxt, durationTxt, caloriesTxt;

        public WorkoutHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImg = itemView.findViewById(R.id.iv_thumbnail);
            titleTxt = itemView.findViewById(R.id.tv_title);
            timeTxt = itemView.findViewById(R.id.tv_time);
            dateTxt = itemView.findViewById(R.id.tv_date);
            durationTxt = itemView.findViewById(R.id.tv_duration);
            caloriesTxt = itemView.findViewById(R.id.tv_calories);
        }
    }
}

