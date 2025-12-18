package fpt.fall2025.posetrainer.Util;

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
     * @param onPreparedCallback Callback khi video đã sẵn sàng (null nếu không cần)
     */
    public static void loadVideo(VideoView videoView, String videoUrl, OnVideoErrorListener onErrorCallback, OnVideoPreparedListener onPreparedCallback) {
        if (videoView == null) {
            Log.w(TAG, "VideoView là null");
            return;
        }
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL video rỗng hoặc null");
            }
            return;
        }
        
        // Sanitize URL
        String sanitizedUrl = VideoUrlHelper.sanitizeVideoUrl(videoUrl);
        
        if (sanitizedUrl == null) {
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL video không hợp lệ: " + videoUrl);
            }
            return;
        }
        
        try {
            Log.d(TAG, "=== BẮT ĐẦU TẢI VIDEO ===");
            Log.d(TAG, "URL: " + sanitizedUrl);
            Log.d(TAG, "Thiết bị: " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")");
            
            // Set video URI
            Uri videoUri = Uri.parse(sanitizedUrl);
            Log.d(TAG, "Đang set VideoURI: " + videoUri.toString());
            videoView.setVideoURI(videoUri);
            Log.d(TAG, "VideoURI đã được set, đang prepare video...");
            
            // Set error listener
            videoView.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    String errorMsg = "Lỗi phát video: ";
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                            errorMsg += "Lỗi không xác định (code: " + what + ", extra: " + extra + ")";
                            break;
                        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                            errorMsg += "Server đã dừng (code: " + what + ", extra: " + extra + ")";
                            break;
                        case MediaPlayer.MEDIA_ERROR_IO:
                            errorMsg += "Lỗi I/O - không thể tải video (code: " + what + ", extra: " + extra + ")";
                            break;
                        case MediaPlayer.MEDIA_ERROR_MALFORMED:
                            errorMsg += "Video không hợp lệ hoặc bị hỏng (code: " + what + ", extra: " + extra + ")";
                            break;
                        case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                            errorMsg += "Định dạng video không được hỗ trợ (code: " + what + ", extra: " + extra + ")";
                            break;
                        case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                            errorMsg += "Timeout khi tải video (code: " + what + ", extra: " + extra + ")";
                            break;
                        default:
                            errorMsg += "Mã lỗi: " + what + ", extra: " + extra;
                    }
                    
                    Log.e(TAG, "=== LỖI PHÁT VIDEO ===");
                    Log.e(TAG, errorMsg);
                    Log.e(TAG, "URL: " + sanitizedUrl);
                    Log.e(TAG, "Thiết bị: " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")");
                    Log.e(TAG, "MediaPlayer what: " + what + ", extra: " + extra);
                    
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
                    Log.d(TAG, "=== VIDEO ĐÃ SẴN SÀNG ===");
                    Log.d(TAG, "Kích thước: " + mp.getVideoWidth() + "x" + mp.getVideoHeight());
                    
                    int duration = mp.getDuration();
                    if (duration > 0) {
                        Log.d(TAG, "Thời lượng: " + (duration / 1000) + " giây");
                    } else {
                        Log.d(TAG, "Thời lượng: Đang tải (streaming video)");
                    }
                    
                    Log.d(TAG, "Thiết bị: " + android.os.Build.MODEL);
                    Log.d(TAG, "Video đã sẵn sàng phát. Click vào VideoView để hiển thị MediaController và phát video.");
                    
                    // Đảm bảo VideoView có thể nhận click events
                    videoView.setClickable(true);
                    videoView.setFocusable(true);
                    
                    // Thêm monitoring để detect khi video bắt đầu phát
                    startPlaybackMonitoring(videoView);
                    
                    // Gọi callback nếu có
                    if (onPreparedCallback != null) {
                        onPreparedCallback.onPrepared();
                    }
                }
            });
            
            // Set completion listener (video finished playing)
            videoView.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "Video đã phát xong");
                }
            });
            
            // Set info listener để theo dõi trạng thái phát video
            // Lưu ý: VideoView không có setOnInfoListener trực tiếp, 
            // nhưng MediaController sẽ tự động gọi start() khi người dùng click play
            // Chúng ta sẽ log trong playVideo() method khi được gọi
            
            // Start loading video (prepare async)
            videoView.requestFocus();
            
            // Đảm bảo VideoView có thể phát video trực tiếp
            // VideoView sẽ tự động sử dụng hardware acceleration nếu được bật trong AndroidManifest
            Log.d(TAG, "Đã bắt đầu tải video. VideoView sẽ tự động prepare video.");
            
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
        loadVideo(videoView, videoUrl, null, null);
    }
    
    /**
     * Load video với error callback
     */
    public static void loadVideo(VideoView videoView, String videoUrl, OnVideoErrorListener onErrorCallback) {
        loadVideo(videoView, videoUrl, onErrorCallback, null);
    }
    
    /**
     * Start video playback
     */
    public static void playVideo(VideoView videoView) {
        if (videoView != null) {
            try {
                Log.d(TAG, "=== BẮT ĐẦU PHÁT VIDEO ===");
                Log.d(TAG, "Đang gọi videoView.start()...");
                videoView.start();
                Log.d(TAG, "videoView.start() đã được gọi");
                
                // Kiểm tra sau một chút xem video có đang phát không
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (videoView.isPlaying()) {
                            Log.d(TAG, "✅ VIDEO ĐANG PHÁT THÀNH CÔNG!");
                        } else {
                            Log.w(TAG, "⚠️ Video chưa phát (có thể đang buffer hoặc có lỗi)");
                        }
                    }
                }, 1000); // Check sau 1 giây
            } catch (Exception e) {
                Log.e(TAG, "❌ Lỗi khi phát video: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "VideoView là null, không thể phát video");
        }
    }
    
    /**
     * Pause video playback
     */
    public static void pauseVideo(VideoView videoView) {
        if (videoView != null && videoView.isPlaying()) {
            try {
                videoView.pause();
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
            }
        }
    }
    
    /**
     * Interface for video error callbacks
     */
    public interface OnVideoErrorListener {
        void onError(String errorMessage);
    }
    
    /**
     * Interface for video prepared callbacks
     */
    public interface OnVideoPreparedListener {
        void onPrepared();
    }
    
    /**
     * Monitor video playback status để detect khi video bắt đầu phát
     */
    private static void startPlaybackMonitoring(VideoView videoView) {
        android.os.Handler handler = new android.os.Handler();
        Runnable monitorRunnable = new Runnable() {
            private boolean wasPlaying = false;
            
            @Override
            public void run() {
                if (videoView == null || videoView.getVisibility() != android.view.View.VISIBLE) {
                    return; // Stop monitoring if videoView is gone
                }
                
                boolean isPlaying = videoView.isPlaying();
                
                // Log khi video bắt đầu phát (chuyển từ không phát sang phát)
                if (isPlaying && !wasPlaying) {
                    Log.d(TAG, "=== VIDEO ĐÃ BẮT ĐẦU PHÁT ===");
                    Log.d(TAG, "✅ Video đang phát thành công!");
                }
                
                wasPlaying = isPlaying;
                
                // Continue monitoring
                handler.postDelayed(this, 500); // Check mỗi 500ms
            }
        };
        
        // Bắt đầu monitoring sau 1 giây
        handler.postDelayed(monitorRunnable, 1000);
    }
}

