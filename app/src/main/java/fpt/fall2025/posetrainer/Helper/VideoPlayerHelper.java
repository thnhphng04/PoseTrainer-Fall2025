package fpt.fall2025.posetrainer.Helper;

import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.VideoView;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnCompletionListener;

/**
 * Helper class để load và play video với VideoView
 * Hỗ trợ tất cả các loại video URLs
 */
public class VideoPlayerHelper {
    private static final String TAG = "VideoPlayerHelper";
    
    /**
     * Load video vào VideoView với error handling tự động
     * Hỗ trợ:
     * - Direct video URLs từ internet
     * - Google Drive URLs
     * - Bất kỳ URL nào VideoView có thể load được
     * 
     * @param videoView VideoView để load video vào
     * @param videoUrl URL video (bất kỳ format nào)
     * @param onErrorCallback Callback khi có lỗi (null nếu không cần)
     */
    public static void loadVideo(VideoView videoView, String videoUrl, OnVideoErrorListener onErrorCallback) {
        if (videoView == null) {
            Log.w(TAG, "VideoView là null");
            return;
        }
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.w(TAG, "URL video rỗng hoặc null");
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL video rỗng hoặc null");
            }
            return;
        }
        
        // Sanitize URL
        String sanitizedUrl = VideoUrlHelper.sanitizeVideoUrl(videoUrl);
        
        if (sanitizedUrl == null) {
            Log.w(TAG, "URL video không hợp lệ: " + videoUrl);
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL video không hợp lệ: " + videoUrl);
            }
            return;
        }
        
        try {
            Log.d(TAG, "Đang tải video từ URL: " + sanitizedUrl);
            
            // Set video URI
            Uri videoUri = Uri.parse(sanitizedUrl);
            videoView.setVideoURI(videoUri);
            
            // Set error listener
            videoView.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "Lỗi phát video - what: " + what + ", extra: " + extra);
                    String errorMsg = "Lỗi phát video: ";
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                            errorMsg += "Lỗi không xác định";
                            break;
                        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                            errorMsg += "Server đã dừng";
                            break;
                        default:
                            errorMsg += "Mã lỗi: " + what;
                    }
                    
                    if (onErrorCallback != null) {
                        onErrorCallback.onError(errorMsg);
                    }
                    return true; // Error handled
                }
            });
            
            // Set prepared listener (video is ready to play)
            videoView.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d(TAG, "Video đã sẵn sàng, có thể phát");
                    // Optional: Auto-play video
                    // videoView.start();
                }
            });
            
            // Set completion listener (video finished playing)
            videoView.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "Video đã phát xong");
                }
            });
            
            // Start loading video (prepare async)
            videoView.requestFocus();
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tải video: " + e.getMessage(), e);
            if (onErrorCallback != null) {
                onErrorCallback.onError("Lỗi khi tải video: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load video với default error handling
     */
    public static void loadVideo(VideoView videoView, String videoUrl) {
        loadVideo(videoView, videoUrl, null);
    }
    
    /**
     * Start video playback
     */
    public static void playVideo(VideoView videoView) {
        if (videoView != null) {
            try {
                videoView.start();
                Log.d(TAG, "Đã bắt đầu phát video");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi bắt đầu phát video: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Pause video playback
     */
    public static void pauseVideo(VideoView videoView) {
        if (videoView != null && videoView.isPlaying()) {
            try {
                videoView.pause();
                Log.d(TAG, "Đã tạm dừng video");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tạm dừng video: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Stop video playback
     */
    public static void stopVideo(VideoView videoView) {
        if (videoView != null) {
            try {
                videoView.stopPlayback();
                Log.d(TAG, "Đã dừng video");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi dừng video: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Release video resources
     */
    public static void releaseVideo(VideoView videoView) {
        if (videoView != null) {
            try {
                stopVideo(videoView);
                videoView.setVideoURI(null);
                Log.d(TAG, "Đã giải phóng tài nguyên video");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi giải phóng tài nguyên video: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Interface for video error callbacks
     */
    public interface OnVideoErrorListener {
        void onError(String errorMessage);
    }
}

