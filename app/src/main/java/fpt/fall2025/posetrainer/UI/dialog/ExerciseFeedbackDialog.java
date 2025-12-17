package fpt.fall2025.posetrainer.UI.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.UUID;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Feedback;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.DAL.FeedbackDAO;
import fpt.fall2025.posetrainer.Util.GlideImageLoader;

/**
 * Dialog để gửi feedback về bài tập
 * Cho phép chọn bài tập và nhập nội dung feedback
 */
public class ExerciseFeedbackDialog extends DialogFragment {
    private static final String TAG = "ExerciseFeedbackDialog";
    
    private LinearLayout layoutSelectedExercise;
    private ImageView imgSelectedExercise;
    private TextView tvSelectedExerciseName;
    private TextView tvSelectedExerciseCategory;
    private TextView tvNoExerciseSelected;
    private Button btnSelectExercise;
    private EditText etFeedbackContent;
    private Button btnSubmit;
    private Button btnCancel;
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    
    private Exercise selectedExercise;
    private FeedbackDAO feedbackDAO;
    private FirebaseAuth mAuth;
    
    private static final String ARG_EXERCISE = "exercise";

    /**
     * Tạo instance mới của dialog với exercise đã chọn sẵn
     */
    public static ExerciseFeedbackDialog newInstance(Exercise exercise) {
        ExerciseFeedbackDialog dialog = new ExerciseFeedbackDialog();
        Bundle args = new Bundle();
        if (exercise != null) {
            args.putSerializable(ARG_EXERCISE, exercise);
        }
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_exercise_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        feedbackDAO = new FeedbackDAO();
        
        layoutSelectedExercise = view.findViewById(R.id.layout_selected_exercise);
        imgSelectedExercise = view.findViewById(R.id.img_selected_exercise);
        tvSelectedExerciseName = view.findViewById(R.id.tv_selected_exercise_name);
        tvSelectedExerciseCategory = view.findViewById(R.id.tv_selected_exercise_category);
        tvNoExerciseSelected = view.findViewById(R.id.tv_no_exercise_selected);
        btnSelectExercise = view.findViewById(R.id.btn_select_exercise);
        etFeedbackContent = view.findViewById(R.id.et_feedback_content);
        btnSubmit = view.findViewById(R.id.btn_submit);
        btnCancel = view.findViewById(R.id.btn_cancel);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutContent = view.findViewById(R.id.layout_content);
        
        // Setup click listeners
        btnSelectExercise.setOnClickListener(v -> showExerciseSelectionDialog());
        btnSubmit.setOnClickListener(v -> submitFeedback());
        btnCancel.setOnClickListener(v -> dismiss());
        
        // Load exercise from arguments if available
        if (getArguments() != null) {
            selectedExercise = (Exercise) getArguments().getSerializable(ARG_EXERCISE);
        }
        
        // Update UI
        updateSelectedExerciseUI();
    }
    
    /**
     * Hiển thị dialog chọn bài tập
     */
    private void showExerciseSelectionDialog() {
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            return;
        }
        
        androidx.appcompat.app.AppCompatActivity activity = 
            (androidx.appcompat.app.AppCompatActivity) getActivity();
        
        ExerciseSelectionDialog dialog = new ExerciseSelectionDialog();
        dialog.setOnExerciseSelectedListener(exercise -> {
            selectedExercise = exercise;
            updateSelectedExerciseUI();
        });
        dialog.show(getParentFragmentManager(), "ExerciseSelectionDialog");
    }
    
    /**
     * Cập nhật UI hiển thị bài tập đã chọn
     */
    private void updateSelectedExerciseUI() {
        if (selectedExercise != null && selectedExercise.getName() != null) {
            // Hiển thị layout bài tập đã chọn
            layoutSelectedExercise.setVisibility(View.VISIBLE);
            tvNoExerciseSelected.setVisibility(View.GONE);
            
            // Set tên bài tập
            tvSelectedExerciseName.setText(selectedExercise.getName());
            
            // Set category
            if (selectedExercise.getCategory() != null && !selectedExercise.getCategory().isEmpty()) {
                StringBuilder categoryText = new StringBuilder();
                for (int i = 0; i < selectedExercise.getCategory().size() && i < 2; i++) {
                    if (i > 0) categoryText.append(", ");
                    categoryText.append(selectedExercise.getCategory().get(i));
                }
                tvSelectedExerciseCategory.setText(categoryText.toString());
            } else {
                tvSelectedExerciseCategory.setText("");
            }
            
            // Load ảnh bài tập
            if (selectedExercise.getMedia() != null && selectedExercise.getMedia().getThumbnailUrl() != null) {
                String thumbnailUrl = selectedExercise.getMedia().getThumbnailUrl();
                GlideImageLoader.loadImage(getContext(), thumbnailUrl, imgSelectedExercise, R.drawable.pic_1_1, R.drawable.pic_1_1);
            } else {
                // Fallback to default image
                imgSelectedExercise.setImageResource(R.drawable.pic_1_1);
            }
        } else {
            // Ẩn layout bài tập đã chọn, hiển thị text "Chưa chọn"
            layoutSelectedExercise.setVisibility(View.GONE);
            tvNoExerciseSelected.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Gửi feedback
     */
    private void submitFeedback() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate
        if (selectedExercise == null) {
            Toast.makeText(getContext(), "Vui lòng chọn bài tập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String content = etFeedbackContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung góp ý", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo feedback object
        String feedbackId = "feedback_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        Feedback feedback = new Feedback();
        feedback.setId(feedbackId);
        feedback.setUid(currentUser.getUid());
        feedback.setType("exercise");
        feedback.setExerciseId(selectedExercise.getId());
        feedback.setExerciseName(selectedExercise.getName());
        feedback.setContent(content);
        feedback.setStatus("pending");
        feedback.setCreatedAt(System.currentTimeMillis() / 1000);
        feedback.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        // Show loading
        setLoading(true);
        
        // Save to Firestore
        feedbackDAO.save(feedback, task -> {
            setLoading(false);
            
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Gửi góp ý thành công! Cảm ơn bạn đã phản hồi.", Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Log.e(TAG, "Error saving feedback", task.getException());
                Toast.makeText(getContext(), "Lỗi khi gửi góp ý. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Set loading state
     */
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (layoutContent != null) {
            layoutContent.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!loading);
        }
        if (btnCancel != null) {
            btnCancel.setEnabled(!loading);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            // Set background đậm để không bị trong suốt
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Thêm dim background để làm nổi bật dialog
            getDialog().getWindow().setDimAmount(0.7f);
        }
    }
}

