package fpt.fall2025.posetrainer.Helper;

import android.util.Log;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class để xử lý và validate video URLs
 * Hỗ trợ TẤT CẢ các loại URL:
 * - Direct video URLs từ internet bất kỳ (http/https)
 * - Google Drive public videos
 * - YouTube URLs (convert sang direct stream URL nếu có thể)
 * - Bất kỳ URL nào VideoView có thể load được
 */
public class VideoUrlHelper {
    private static final String TAG = "VideoUrlHelper";
    
    // Pattern để extract FILE_ID từ Google Drive URL
    private static final Pattern GOOGLE_DRIVE_FILE_ID_PATTERN = 
        Pattern.compile("(?:/d/|id=)([a-zA-Z0-9_-]{25,})");
    
    // Pattern để extract video ID từ YouTube URL
    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = 
        Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})");
    
    /**
     * Sanitize và convert video URL thành format có thể load được
     * Hỗ trợ TẤT CẢ các loại URL video
     * 
     * @param url URL gốc (bất kỳ format nào)
     * @return URL đã được sanitize hoặc null nếu không hợp lệ
     */
    public static String sanitizeVideoUrl(String url) {
        if (url == null || url.isEmpty() || url.trim().isEmpty()) {
            Log.w(TAG, "URL video rỗng hoặc null");
            return null;
        }
        
        url = url.trim();
        
        // 1. Xử lý Google Drive URLs - convert sang direct video URL
        if (isGoogleDriveUrl(url)) {
            String directUrl = convertGoogleDriveToDirectVideoUrl(url);
            if (directUrl != null) {
                Log.d(TAG, "Đã chuyển đổi Google Drive URL: " + url + " -> " + directUrl);
                return directUrl;
            }
            Log.w(TAG, "Không thể chuyển đổi Google Drive URL: " + url);
            return null;
        }
        
        // 2. Xử lý YouTube URLs - convert sang direct stream URL (nếu có thể)
        if (isYouTubeUrl(url)) {
            String directUrl = convertYouTubeToDirectUrl(url);
            if (directUrl != null) {
                Log.d(TAG, "Đã chuyển đổi YouTube URL: " + url + " -> " + directUrl);
                return directUrl;
            }
            // YouTube URLs không thể convert thành direct stream URL dễ dàng
            // VideoView không hỗ trợ YouTube URLs trực tiếp
            Log.w(TAG, "YouTube URLs không được hỗ trợ để phát trực tiếp. Nên sử dụng YouTube Player API.");
            return null;
        }
        
        // 3. Validate và return direct video URL (bất kỳ URL nào từ internet)
        if (isValidVideoUrl(url)) {
            Log.d(TAG, "URL video hợp lệ: " + url);
            return url;
        }
        
        // 4. Nếu không match pattern nào, nhưng là HTTP/HTTPS URL, vẫn thử load
        // (VideoView sẽ tự xử lý và fallback nếu không load được)
        if (isHttpUrl(url)) {
            Log.d(TAG, "URL HTTP/HTTPS (sẽ thử tải): " + url);
            return url;
        }
        
        Log.w(TAG, "URL video không hợp lệ hoặc không được hỗ trợ: " + url);
        return null;
    }
    
    /**
     * Kiểm tra xem URL có phải là Google Drive URL không
     */
    public static boolean isGoogleDriveUrl(String url) {
        return url != null && (
            url.contains("drive.google.com") || 
            url.contains("docs.google.com")
        );
    }
    
    /**
     * Convert Google Drive sharing link thành direct video URL
     * 
     * Formats hỗ trợ:
     * 1. https://drive.google.com/file/d/FILE_ID/view?usp=sharing
     * 2. https://drive.google.com/open?id=FILE_ID
     * 3. https://drive.google.com/uc?export=download&id=FILE_ID (đã là direct URL)
     * 4. https://docs.google.com/uc?id=FILE_ID
     * 
     * @param googleDriveUrl Google Drive sharing link hoặc direct URL
     * @return Direct video URL hoặc null nếu không thể extract FILE_ID
     */
    public static String convertGoogleDriveToDirectVideoUrl(String googleDriveUrl) {
        if (googleDriveUrl == null || googleDriveUrl.isEmpty()) {
            return null;
        }
        
        // Nếu đã là direct URL format, return as is
        if (googleDriveUrl.contains("uc?export=download") || googleDriveUrl.contains("uc?id=")) {
            // Đảm bảo format đúng
            if (googleDriveUrl.contains("export=download")) {
                return googleDriveUrl;
            } else if (googleDriveUrl.contains("uc?id=")) {
                // Convert docs.google.com format
                return googleDriveUrl.replace("uc?id=", "uc?export=download&id=");
            }
        }
        
        // Extract FILE_ID từ URL
        String fileId = extractGoogleDriveFileId(googleDriveUrl);
        if (fileId == null || fileId.isEmpty()) {
            Log.w(TAG, "Không thể trích xuất FILE_ID từ Google Drive URL: " + googleDriveUrl);
            return null;
        }
        
        // Tạo direct video URL
        // Format: https://drive.google.com/uc?export=download&id=FILE_ID
        // Note: VideoView có thể load được format này nếu file là video và public
        String directUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
        Log.d(TAG, "Đã chuyển đổi Google Drive URL: " + googleDriveUrl + " -> " + directUrl);
        
        return directUrl;
    }
    
    /**
     * Extract FILE_ID từ Google Drive URL
     */
    private static String extractGoogleDriveFileId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        Matcher matcher = GOOGLE_DRIVE_FILE_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            String fileId = matcher.group(1);
            Log.d(TAG, "Đã trích xuất FILE_ID: " + fileId);
            return fileId;
        }
        
        return null;
    }
    
    /**
     * Kiểm tra xem URL có phải là YouTube URL không
     */
    public static boolean isYouTubeUrl(String url) {
        return url != null && (
            url.contains("youtube.com") || 
            url.contains("youtu.be")
        );
    }
    
    /**
     * Convert YouTube URL sang embed URL để load trong WebView
     * 
     * @param youtubeUrl YouTube URL (watch, youtu.be, embed format)
     * @return YouTube embed URL hoặc null nếu không thể extract video ID
     */
    public static String convertYouTubeToEmbedUrl(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.isEmpty()) {
            return null;
        }
        
        // Extract video ID
        String videoId = extractYouTubeVideoId(youtubeUrl);
        if (videoId == null || videoId.isEmpty()) {
            Log.w(TAG, "Không thể trích xuất video ID từ YouTube URL: " + youtubeUrl);
            return null;
        }
        
        // Tạo YouTube embed URL
        // Format: https://www.youtube.com/embed/VIDEO_ID?autoplay=0&rel=0
        String embedUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=0&rel=0&modestbranding=1";
        Log.d(TAG, "Đã chuyển đổi YouTube URL sang embed URL: " + youtubeUrl + " -> " + embedUrl);
        
        return embedUrl;
    }
    
    /**
     * Convert YouTube URL sang direct stream URL
     * Note: YouTube không cho phép direct streaming dễ dàng
     * VideoView không hỗ trợ YouTube URLs trực tiếp
     * Cần dùng YouTube Player API hoặc YouTube IFrame Player
     * 
     * @param youtubeUrl YouTube URL
     * @return null (YouTube URLs không được hỗ trợ trực tiếp)
     * @deprecated Use convertYouTubeToEmbedUrl() instead for WebView playback
     */
    @Deprecated
    public static String convertYouTubeToDirectUrl(String youtubeUrl) {
        return convertYouTubeToEmbedUrl(youtubeUrl);
    }
    
    /**
     * Extract video ID từ YouTube URL
     */
    public static String extractYouTubeVideoId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            String videoId = matcher.group(1);
            Log.d(TAG, "Đã trích xuất YouTube video ID: " + videoId);
            return videoId;
        }
        
        return null;
    }
    
    /**
     * Kiểm tra xem URL có hợp lệ để load video không
     * Chấp nhận các format:
     * - HTTP/HTTPS URLs với video extension (.mp4, .webm, .3gp, .mkv, .avi)
     * - Google Drive direct URLs
     * - Bất kỳ HTTP/HTTPS URL nào (VideoView sẽ tự xử lý)
     */
    public static boolean isValidVideoUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Check if it's a valid HTTP/HTTPS URL
        // Pattern 1: URLs với video extension
        Pattern videoExtensionPattern = Pattern.compile(
            "^(https?://).+\\.(mp4|webm|3gp|mkv|avi|mov|flv|wmv)(\\?.*)?$",
            Pattern.CASE_INSENSITIVE
        );
        
        // Pattern 2: Google Drive direct URL
        Pattern googleDrivePattern = Pattern.compile(
            "^(https?://).*drive\\.google\\.com/uc\\?export=download",
            Pattern.CASE_INSENSITIVE
        );
        
        // Pattern 3: Bất kỳ HTTP/HTTPS URL nào (VideoView sẽ tự xử lý)
        Pattern httpPattern = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);
        
        return videoExtensionPattern.matcher(url).matches() ||
               googleDrivePattern.matcher(url).matches() ||
               httpPattern.matcher(url).matches();
    }
    
    /**
     * Kiểm tra xem URL có phải là HTTP/HTTPS URL không
     * (Bất kỳ URL nào từ internet)
     */
    public static boolean isHttpUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        Pattern pattern = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(url).matches();
    }
    
    /**
     * Kiểm tra xem URL có phải là video file không (dựa vào extension)
     */
    public static boolean isVideoFile(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        Pattern pattern = Pattern.compile(
            "\\.(mp4|webm|3gp|mkv|avi|mov|flv|wmv)(\\?.*)?$",
            Pattern.CASE_INSENSITIVE
        );
        return pattern.matcher(url).matches();
    }
}

