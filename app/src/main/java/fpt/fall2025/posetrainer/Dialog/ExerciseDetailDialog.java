package fpt.fall2025.posetrainer.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import android.webkit.WebView;
import android.widget.MediaController;
import android.widget.VideoView;

import fpt.fall2025.posetrainer.Activity.ExerciseActivity;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Helper.GlideImageLoader;
import fpt.fall2025.posetrainer.Helper.VideoPlayerHelper;
import fpt.fall2025.posetrainer.Helper.VideoUrlHelper;
import fpt.fall2025.posetrainer.Helper.YouTubeWebViewHelper;
import fpt.fall2025.posetrainer.databinding.DialogExerciseDetailBinding;

/**
 * ExerciseDetailDialog - Dialog hiển thị chi tiết bài tập
 * Hiển thị tên, level, category, sets x reps, ảnh/video demo
 */
public class ExerciseDetailDialog extends Dialog {
    private static final String TAG = "ExerciseDetailDialog";

    private DialogExerciseDetailBinding binding;
    private Exercise exercise;
    private Context context;
    private int customSets = -1;
    private int customReps = -1;
    private String customDifficulty = null;

    public ExerciseDetailDialog(@NonNull Context context, Exercise exercise) {
        super(context);
        this.context = context;
        this.exercise = exercise;
    }

    public ExerciseDetailDialog(@NonNull Context context, Exercise exercise, int customSets, int customReps, String customDifficulty) {
        super(context);
        this.context = context;
        this.exercise = exercise;
        this.customSets = customSets;
        this.customReps = customReps;
        this.customDifficulty = customDifficulty;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove default title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = DialogExerciseDetailBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        // Set dialog properties
        // Get screen dimensions to set dialog size (90% of screen height for better scrolling)
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        android.graphics.Point size = new android.graphics.Point();
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getSize(size);
            int screenHeight = size.y;
            int dialogHeight = (int) (screenHeight * 0.85); // 85% of screen height
            
            // Set dialog window properties
            Window window = getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight);
                window.setBackgroundDrawableResource(android.R.color.transparent);
                
                // Enable hardware acceleration for video playback
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                );
            }
        } else {
            // Fallback: use WRAP_CONTENT
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Make dialog cancelable when clicking outside
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        if (exercise == null) {
            Toast.makeText(context, "No exercise data provided", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        setupUI();
        updateExerciseUI();

        // Debug binding
        Log.d(TAG, "Ràng buộc closeBtn: " + (binding.closeBtn != null ? "KHÔNG NULL" : "NULL"));
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Close button with null check and enhanced debugging
        if (binding.closeBtn != null) {
            Log.d(TAG, "Đã tìm thấy nút đóng, đang thiết lập listener");

            // Ensure the button is clickable and focusable
            binding.closeBtn.setClickable(true);
            binding.closeBtn.setFocusable(true);
            binding.closeBtn.setFocusableInTouchMode(true);

            binding.closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Nút đóng đã được nhấn!");
                    dismiss();
                }
            });

            // Background selector is already set in XML

            Log.d(TAG, "Đã thiết lập listener cho nút đóng thành công");
        } else {
            Log.e(TAG, "Nút đóng là null!");
        }
    }

    /**
     * Update UI with exercise data
     */
    private void updateExerciseUI() {
        if (exercise == null) return;

        // Set exercise name
        binding.exerciseNameTxt.setText(exercise.getName());

        // Set level
        binding.exerciseLevelTxt.setText(exercise.getLevel());

        // Set category (first category if available)
        if (exercise.getCategory() != null && !exercise.getCategory().isEmpty()) {
            binding.exerciseCategoryTxt.setText(exercise.getCategory().get(0));
        } else {
            binding.exerciseCategoryTxt.setText("General");
        }

        // Set sets x reps - use custom config if available, otherwise use default
        int sets, reps;
        if (customSets > 0 && customReps > 0) {
            sets = customSets;
            reps = customReps;
        } else if (exercise.getDefaultConfig() != null) {
            sets = exercise.getDefaultConfig().getSets();
            reps = exercise.getDefaultConfig().getReps();
        } else {
            sets = 3;
            reps = 12;
        }
        String setsReps = sets + " x " + reps;
        binding.exerciseSetsRepsTxt.setText(setsReps);

        // Set muscles (if available)
        if (exercise.getMuscles() != null && !exercise.getMuscles().isEmpty()) {
            StringBuilder musclesText = new StringBuilder();
            for (int i = 0; i < exercise.getMuscles().size(); i++) {
                if (i > 0) musclesText.append(", ");
                musclesText.append(exercise.getMuscles().get(i));
            }
            binding.exerciseMusclesTxt.setText(musclesText.toString());
        } else {
            binding.exerciseMusclesTxt.setText("Full body");
        }

        // Set equipment (if available)
        if (exercise.getEquipment() != null && !exercise.getEquipment().isEmpty()) {
            StringBuilder equipmentText = new StringBuilder();
            for (int i = 0; i < exercise.getEquipment().size(); i++) {
                if (i > 0) equipmentText.append(", ");
                equipmentText.append(exercise.getEquipment().get(i));
            }
            binding.exerciseEquipmentTxt.setText(equipmentText.toString());
        } else {
            binding.exerciseEquipmentTxt.setText("No equipment");
        }

        // Load media (demo video or thumbnail)
        loadExerciseMedia();
    }

    /**
     * Load exercise media (video or image)
     */
    private void loadExerciseMedia() {
        if (exercise.getMedia() != null) {
            // Try to load demo video first
            if (exercise.getMedia().getDemoVideoUrl() != null && !exercise.getMedia().getDemoVideoUrl().isEmpty()) {
                String videoUrl = exercise.getMedia().getDemoVideoUrl();
                Log.d(TAG, "Đang tải video demo từ URL: " + videoUrl);
                
                // Hide image view and placeholder
                binding.exerciseImageView.setVisibility(View.GONE);
                binding.mediaPlaceholderTxt.setVisibility(View.GONE);
                
                // Check if it's a YouTube URL
                if (VideoUrlHelper.isYouTubeUrl(videoUrl)) {
                    // Load YouTube video in WebView
                    Log.d(TAG, "Đã phát hiện URL YouTube, đang tải trong WebView");
                    Log.d(TAG, "Trạng thái hiển thị WebView trước: " + (binding.exerciseWebView.getVisibility() == View.VISIBLE ? "HIỂN THỊ" : "ẨN"));
                    
                    // Hide other views
                    binding.exerciseVideoView.setVisibility(View.GONE);
                    binding.exerciseImageView.setVisibility(View.GONE);
                    binding.mediaPlaceholderTxt.setVisibility(View.GONE);
                    
                    // Show WebView
                    binding.exerciseWebView.setVisibility(View.VISIBLE);
                    binding.exerciseWebView.bringToFront(); // Bring WebView to front
                    Log.d(TAG, "Trạng thái hiển thị WebView sau: " + (binding.exerciseWebView.getVisibility() == View.VISIBLE ? "HIỂN THỊ" : "ẨN"));
                    Log.d(TAG, "Kích thước WebView: width=" + binding.exerciseWebView.getWidth() + ", height=" + binding.exerciseWebView.getHeight());
                    
                    // Post to ensure WebView is laid out before loading
                    binding.exerciseWebView.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Bố cục WebView: width=" + binding.exerciseWebView.getWidth() + ", height=" + binding.exerciseWebView.getHeight());
                            YouTubeWebViewHelper.loadYouTubeVideo(binding.exerciseWebView, videoUrl,
                                new YouTubeWebViewHelper.OnYouTubeErrorListener() {
                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e(TAG, "Lỗi tải video YouTube: " + errorMessage);
                                        // Hide WebView on error
                                        binding.exerciseWebView.setVisibility(View.GONE);
                                        
                                        // Try to show thumbnail as fallback
                                        if (exercise.getMedia().getThumbnailUrl() != null && 
                                            !exercise.getMedia().getThumbnailUrl().isEmpty()) {
                                            String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
                                            GlideImageLoader.loadImage(context, thumbnailUrl, binding.exerciseImageView);
                                            binding.exerciseImageView.setVisibility(View.VISIBLE);
                                            binding.mediaPlaceholderTxt.setVisibility(View.GONE);
                                            Log.d(TAG, "Chuyển sang hiển thị thumbnail: " + thumbnailUrl);
                                        } else {
                                            // Show error message
                                            binding.mediaPlaceholderTxt.setText("Video unavailable");
                                            binding.mediaPlaceholderTxt.setVisibility(View.VISIBLE);
                                        }
                                    }
                                });
                        }
                    });
                } else {
                    // Load direct video in VideoView (Google Drive, direct URLs, etc.)
                    Log.d(TAG, "Đã phát hiện URL video trực tiếp, đang tải trong VideoView");
                    binding.exerciseWebView.setVisibility(View.GONE);
                    binding.exerciseVideoView.setVisibility(View.VISIBLE);
                    
                    // Setup MediaController for video controls (play, pause, seek)
                    MediaController mediaController = new MediaController(context);
                    mediaController.setAnchorView(binding.exerciseVideoView);
                    binding.exerciseVideoView.setMediaController(mediaController);
                    
                    // Load video với error handling
                    VideoPlayerHelper.loadVideo(binding.exerciseVideoView, videoUrl, 
                        new VideoPlayerHelper.OnVideoErrorListener() {
                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "Lỗi tải video: " + errorMessage);
                                // Hide video view on error
                                binding.exerciseVideoView.setVisibility(View.GONE);
                                
                                // Try to show thumbnail as fallback
                                if (exercise.getMedia().getThumbnailUrl() != null && 
                                    !exercise.getMedia().getThumbnailUrl().isEmpty()) {
                                    String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
                                    GlideImageLoader.loadImage(context, thumbnailUrl, binding.exerciseImageView);
                                    binding.exerciseImageView.setVisibility(View.VISIBLE);
                                    binding.mediaPlaceholderTxt.setVisibility(View.GONE);
                                    Log.d(TAG, "Chuyển sang hiển thị thumbnail: " + thumbnailUrl);
                                } else {
                                    // Show error message
                                    binding.mediaPlaceholderTxt.setText("Video unavailable");
                                    binding.mediaPlaceholderTxt.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    
                    // Auto-play video (optional - bạn có thể remove dòng này nếu không muốn auto-play)
                    // VideoPlayerHelper.playVideo(binding.exerciseVideoView);
                }
            }
            // Try thumbnail if no video - sử dụng GlideImageLoader để hỗ trợ tất cả các loại URL
            else if (exercise.getMedia().getThumbnailUrl() != null && !exercise.getMedia().getThumbnailUrl().isEmpty()) {
                String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
                // GlideImageLoader tự động xử lý: Google Drive, Google Image Search, direct URLs, local drawables
                GlideImageLoader.loadImage(context, thumbnailUrl, binding.exerciseImageView);
                binding.exerciseWebView.setVisibility(View.GONE);
                binding.exerciseVideoView.setVisibility(View.GONE);
                binding.exerciseImageView.setVisibility(View.VISIBLE);
                binding.mediaPlaceholderTxt.setVisibility(View.GONE);
                Log.d(TAG, "URL thumbnail: " + thumbnailUrl);
            }
            // No media available
            else {
                binding.exerciseWebView.setVisibility(View.GONE);
                binding.exerciseVideoView.setVisibility(View.GONE);
                binding.exerciseImageView.setVisibility(View.GONE);
                binding.mediaPlaceholderTxt.setText("No media available");
                binding.mediaPlaceholderTxt.setVisibility(View.VISIBLE);
            }
        } else {
            // No media object
            binding.exerciseWebView.setVisibility(View.GONE);
            binding.exerciseVideoView.setVisibility(View.GONE);
            binding.exerciseImageView.setVisibility(View.GONE);
            binding.mediaPlaceholderTxt.setText("No media available");
            binding.mediaPlaceholderTxt.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void dismiss() {
        // Stop video playback và cleanup WebView khi dialog dismiss
        if (binding.exerciseVideoView != null) {
            VideoPlayerHelper.stopVideo(binding.exerciseVideoView);
        }
        if (binding.exerciseWebView != null) {
            YouTubeWebViewHelper.cleanup(binding.exerciseWebView);
        }
        super.dismiss();
    }

    /**
     * Static method to show dialog
     */
    public static void show(Context context, Exercise exercise) {
        try {
            ExerciseDetailDialog dialog = new ExerciseDetailDialog(context, exercise);
            dialog.show();
            Log.d(TAG, "Đã hiển thị dialog thành công");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi hiển thị dialog: " + e.getMessage(), e);
        }
    }

    /**
     * Static method to show dialog with custom config
     */
    public static void show(Context context, Exercise exercise, int customSets, int customReps, String customDifficulty) {
        try {
            ExerciseDetailDialog dialog = new ExerciseDetailDialog(context, exercise, customSets, customReps, customDifficulty);
            dialog.show();
            Log.d(TAG, "Đã hiển thị dialog thành công với cấu hình tùy chỉnh");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi hiển thị dialog: " + e.getMessage(), e);
        }
    }
}
