package fpt.fall2025.posetrainer.Util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.ui.PlayerView;

/**
 * Helper class để load và play video với ExoPlayer
 * ExoPlayer hoạt động tốt hơn VideoView trên nhiều thiết bị
 */
public class ExoPlayerHelper {
    private static final String TAG = "ExoPlayerHelper";
    
    /**
     * Load video vào PlayerView với error handling tự động
     * Hỗ trợ:
     * - Direct video URLs từ internet
     * - Google Drive URLs
     * - Firebase Storage URLs
     * - Bất kỳ URL nào ExoPlayer có thể load được
     * 
     * @param context Context của activity/fragment
     * @param playerView PlayerView để load video vào
     * @param videoUrl URL video (bất kỳ format nào)
     * @param onErrorCallback Callback khi có lỗi (null nếu không cần)
     * @param onPreparedCallback Callback khi video đã sẵn sàng (null nếu không cần)
     * @return ExoPlayer instance để có thể control sau này
     */
    public static ExoPlayer loadVideo(Context context, PlayerView playerView, String videoUrl, 
                                      OnVideoErrorListener onErrorCallback, 
                                      OnVideoPreparedListener onPreparedCallback) {
        if (playerView == null) {
            Log.w(TAG, "PlayerView là null");
            return null;
        }
        
        if (context == null) {
            Log.w(TAG, "Context là null");
            return null;
        }
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL video rỗng hoặc null");
            }
            return null;
        }
        
        // Sanitize URL
        String sanitizedUrl = VideoUrlHelper.sanitizeVideoUrl(videoUrl);
        
        if (sanitizedUrl == null) {
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL video không hợp lệ: " + videoUrl);
            }
            return null;
        }
        
        try {
            Log.d(TAG, "=== BẮT ĐẦU TẢI VIDEO VỚI EXOPLAYER ===");
            Log.d(TAG, "URL: " + sanitizedUrl);
            Log.d(TAG, "Thiết bị: " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")");
            
            // Tạo RenderersFactory với cấu hình cho phép software decoder
            // Điều này giúp xử lý trường hợp hardware decoder không hoạt động
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true); // Cho phép fallback sang software decoder nếu hardware decoder fail
            
            Log.d(TAG, "Đã cấu hình ExoPlayer với decoder fallback enabled");
            
            // Tạo ExoPlayer instance với RenderersFactory tùy chỉnh
            ExoPlayer player = new ExoPlayer.Builder(context)
                    .setRenderersFactory(renderersFactory)
                    .build();
            
            Log.d(TAG, "ExoPlayer instance đã được tạo");
            
            // Tạo MediaItem từ URL với cấu hình để hỗ trợ seek tốt hơn
            Uri videoUri = Uri.parse(sanitizedUrl);
            MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                    .setUri(videoUri);
            
            // Cấu hình để hỗ trợ seek (ngay cả với streaming video)
            MediaItem mediaItem = mediaItemBuilder.build();
            
            // Set media item vào player
            player.setMediaItem(mediaItem);
            
            // Attach player vào PlayerView
            playerView.setPlayer(player);
            
            // Enable controls và seek
            playerView.setUseController(true);
            playerView.setControllerShowTimeoutMs(3000); // Hiển thị controls trong 3 giây
            playerView.setControllerAutoShow(true); // Tự động hiển thị khi click
            
            // Đảm bảo PlayerView có thể nhận touch events để seek
            playerView.setClickable(true);
            playerView.setFocusable(true);
            
            // Ẩn các nút không cần thiết (bao gồm rewind)
            hideUnnecessaryButtons(playerView);
            
            Log.d(TAG, "PlayerView đã được cấu hình: useController=true, clickable=true, focusable=true");
            
            // Set error listener
            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                    String errorMsg = "Lỗi phát video: " + error.getMessage();
                    Log.e(TAG, "=== LỖI PHÁT VIDEO ===");
                    Log.e(TAG, errorMsg);
                    Log.e(TAG, "URL: " + sanitizedUrl);
                    Log.e(TAG, "Thiết bị: " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")");
                    Log.e(TAG, "Error type: " + error.errorCode);
                    
                    // Kiểm tra xem có phải lỗi codec không
                    if (error.getCause() instanceof com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException) {
                        com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException decoderException = 
                            (com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException) error.getCause();
                        Log.e(TAG, "Lỗi decoder initialization");
                        Log.e(TAG, "Decoder exception: " + decoderException.getMessage());
                        Log.e(TAG, "ExoPlayer đã cố gắng fallback sang decoder khác nhưng vẫn thất bại");
                        Log.e(TAG, "Có thể thiết bị không hỗ trợ codec này hoặc cần cấu hình đặc biệt");
                    }
                    
                    if (onErrorCallback != null) {
                        onErrorCallback.onError(errorMsg);
                    }
                }
                
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_READY) {
                        Log.d(TAG, "=== VIDEO ĐÃ SẴN SÀNG ===");
                        long duration = player.getDuration();
                        long currentPosition = player.getCurrentPosition();
                        boolean isSeekable = player.isCurrentMediaItemSeekable();
                        Log.d(TAG, "Video có thể seek: " + isSeekable);
                        Log.d(TAG, "Vị trí hiện tại: " + (currentPosition / 1000) + " giây");
                        if (duration > 0) {
                            Log.d(TAG, "Thời lượng: " + (duration / 1000) + " giây");
                            Log.d(TAG, "✅ Video có duration cố định - seek bar sẽ hoạt động");
                        } else {
                            Log.w(TAG, "⚠️ Thời lượng: 0 (streaming video) - có thể không seek được");
                            Log.w(TAG, "⚠️ Video streaming không có duration cố định, seek bar có thể không hoạt động");
                        }
                        Log.d(TAG, "Thiết bị: " + android.os.Build.MODEL);
                        
                        
                        if (onPreparedCallback != null) {
                            onPreparedCallback.onPrepared();
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        Log.d(TAG, "Video đã phát xong");
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        Log.d(TAG, "Video đang buffer...");
                    } else if (playbackState == Player.STATE_IDLE) {
                        Log.d(TAG, "Video ở trạng thái idle");
                    }
                }
                
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (isPlaying) {
                        Log.d(TAG, "=== VIDEO ĐÃ BẮT ĐẦU PHÁT ===");
                        long currentPos = player.getCurrentPosition();
                        Log.d(TAG, "✅ Video đang phát tại vị trí: " + (currentPos / 1000) + " giây");
                        
                        // Kiểm tra xem video có bị reset về đầu không (sau khi seek)
                        // Nếu vị trí < 1 giây sau khi đã seek, có thể video bị reset
                        if (currentPos < 1000 && player.getDuration() > 10000) {
                            Log.w(TAG, "⚠️ Video có thể bị reset về đầu sau khi seek");
                        }
                    } else {
                        Log.d(TAG, "Video đã dừng hoặc pause");
                    }
                }
                
            });
            
            // Prepare player (load video)
            player.prepare();
            
            // Tự động phát video sau khi sẵn sàng
            player.setPlayWhenReady(true);
            
            Log.d(TAG, "ExoPlayer đã được setup và đang tải video...");
            
            return player;
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tải video: " + e.getMessage(), e);
            if (onErrorCallback != null) {
                onErrorCallback.onError("Lỗi khi tải video: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Load video với default error handling
     */
    public static ExoPlayer loadVideo(Context context, PlayerView playerView, String videoUrl) {
        return loadVideo(context, playerView, videoUrl, null, null);
    }
    
    /**
     * Load video với error callback
     */
    public static ExoPlayer loadVideo(Context context, PlayerView playerView, String videoUrl, 
                                      OnVideoErrorListener onErrorCallback) {
        return loadVideo(context, playerView, videoUrl, onErrorCallback, null);
    }
    
    /**
     * Release ExoPlayer resources
     */
    public static void releasePlayer(ExoPlayer player) {
        if (player != null) {
            try {
                player.stop();
                player.release();
                Log.d(TAG, "ExoPlayer đã được release");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi release ExoPlayer: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Pause video playback
     */
    public static void pauseVideo(ExoPlayer player) {
        if (player != null) {
            try {
                player.pause();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi pause video: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Resume video playback
     */
    public static void resumeVideo(ExoPlayer player) {
        if (player != null) {
            try {
                player.setPlayWhenReady(true);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi resume video: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Stop video playback
     */
    public static void stopVideo(ExoPlayer player) {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi stop video: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Seek video đến vị trí cụ thể (milliseconds)
     * @param player ExoPlayer instance
     * @param positionMs Vị trí muốn tua đến (tính bằng milliseconds)
     */
    public static void seekTo(ExoPlayer player, long positionMs) {
        if (player != null) {
            try {
                long duration = player.getDuration();
                if (duration > 0 && positionMs >= 0 && positionMs <= duration) {
                    player.seekTo(positionMs);
                    Log.d(TAG, "Đã tua video đến vị trí: " + (positionMs / 1000) + " giây");
                } else {
                    Log.w(TAG, "Vị trí tua không hợp lệ: " + positionMs + " (duration: " + duration + ")");
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tua video: " + e.getMessage(), e);
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
     * Ẩn các nút không cần thiết trong ExoPlayer controller
     */
    private static void hideUnnecessaryButtons(PlayerView playerView) {
        playerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // Ẩn rewind button
                    View rewButton = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_rew);
                    if (rewButton != null) {
                        rewButton.setVisibility(View.GONE);
                        rewButton.setEnabled(false);
                        rewButton.setClickable(false);
                    }
                    
                    // Ẩn fast-forward button
                    View ffwdButton = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_ffwd);
                    if (ffwdButton != null) {
                        ffwdButton.setVisibility(View.GONE);
                    }
                    
                    // Ẩn next button
                    View nextButton = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_next);
                    if (nextButton != null) {
                        nextButton.setVisibility(View.GONE);
                    }
                    
                    // Ẩn previous button
                    View prevButton = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_prev);
                    if (prevButton != null) {
                        prevButton.setVisibility(View.GONE);
                    }
                    
                    Log.d(TAG, "✅ Đã ẩn các nút không cần thiết (bao gồm rewind)");
                } catch (Exception e) {
                    Log.w(TAG, "Lỗi khi ẩn buttons: " + e.getMessage());
                }
            }
        }, 500);
    }
    
    
    /**
     * Interface for video prepared callbacks
     */
    public interface OnVideoPreparedListener {
        void onPrepared();
    }
}
