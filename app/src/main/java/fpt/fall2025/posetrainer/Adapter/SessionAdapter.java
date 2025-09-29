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
import java.util.Date;
import java.util.Locale;

import fpt.fall2025.posetrainer.Activity.SessionActivity;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.R;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {
    private ArrayList<Session> sessions;
    private Context context;

    public SessionAdapter(ArrayList<Session> sessions) {
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessions.get(position);
        
        // Set session title - get from workout template
        holder.titleTxt.setText("Session " + session.getId());
        
        // Set date
        if (session.getStartedAt() > 0) {
            // Convert seconds to milliseconds for Date constructor
            Date date = new Date(session.getStartedAt() * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.dateTxt.setText(sdf.format(date));
        } else {
            holder.dateTxt.setText("Not started");
        }
        
        // Set duration
        if (session.getEndedAt() > 0 && session.getStartedAt() > 0) {
            // Both startedAt and endedAt are in seconds
            long duration = session.getEndedAt() - session.getStartedAt();
            int minutes = (int) (duration / 60); // Convert seconds to minutes
            holder.durationTxt.setText(minutes + " min");
        } else {
            holder.durationTxt.setText("In progress");
        }
        
        // Set progress
        if (session.getPerExercise() != null) {
            int completed = 0;
            int total = session.getPerExercise().size();
            
            for (Session.PerExercise perExercise : session.getPerExercise()) {
                if ("completed".equals(perExercise.getState())) {
                    completed++;
                }
            }
            
            holder.progressTxt.setText(completed + "/" + total + " completed");
        } else {
            holder.progressTxt.setText("0/0 completed");
        }
        
        // Set default image
        holder.imageView.setImageResource(R.drawable.pic_1);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SessionActivity.class);
            intent.putExtra("sessionId", session.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTxt, dateTxt, durationTxt, progressTxt;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            dateTxt = itemView.findViewById(R.id.dateTxt);
            durationTxt = itemView.findViewById(R.id.durationTxt);
            progressTxt = itemView.findViewById(R.id.progressTxt);
        }
    }
}
