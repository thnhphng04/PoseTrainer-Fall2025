package fpt.fall2025.posetrainer.Helper;

import android.util.Log;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class để xử lý và validate image URLs
 * Hỗ trợ TẤT CẢ các loại URL:
 * - Direct image URLs từ internet bất kỳ (http/https)
 * - Google Drive public images
 * - Google Image Search URLs
 * - Local drawable resources
 * - Bất kỳ URL nào Glide có thể load được
 */
public class ImageUrlHelper {
    private static final String TAG = "ImageUrlHelper";
    
    // Pattern để extract FILE_ID từ Google Drive URL
    private static final Pattern GOOGLE_DRIVE_FILE_ID_PATTERN = 
        Pattern.compile("(?:/d/|id=)([a-zA-Z0-9_-]{25,})");
    
    // Pattern để extract direct image URL từ Google Image Search
    private static final Pattern GOOGLE_IMAGE_SEARCH_PATTERN = 
        Pattern.compile("imgurl=([^&]+)");
    
    /**
     * Sanitize và convert image URL thành format có thể load được
     * Hỗ trợ TẤT CẢ các loại URL ảnh
     * 
     * @param url URL gốc (bất kỳ format nào)
     * @return URL đã được sanitize hoặc null nếu không hợp lệ
     */
    public static String sanitizeImageUrl(String url) {
        if (url == null || url.isEmpty() || url.trim().isEmpty()) {
            Log.w(TAG, "URL rỗng hoặc null");
            return null;
        }
        
        url = url.trim();
        
        // 1. Nếu là local drawable resource, return as is
        if (isLocalDrawable(url)) {
            Log.d(TAG, "Tài nguyên drawable local: " + url);
            return url;
        }
        
        // 2. Xử lý Google Drive URLs - convert sang direct URL
        if (isGoogleDriveUrl(url)) {
            String directUrl = convertGoogleDriveToDirectUrl(url);
            if (directUrl != null) {
                Log.d(TAG, "Đã chuyển đổi Google Drive URL: " + url + " -> " + directUrl);
                return directUrl;
            }
            Log.w(TAG, "Không thể chuyển đổi Google Drive URL: " + url);
            return null;
        }
        
        // 3. Xử lý Google Image Search URLs - extract direct URL
        if (isGoogleImageSearchUrl(url)) {
            String directUrl = extractDirectUrlFromGoogleImageSearch(url);
            if (directUrl != null) {
                Log.d(TAG, "Đã trích xuất URL trực tiếp từ Google Image Search: " + url + " -> " + directUrl);
                return directUrl;
            }
            Log.w(TAG, "Không thể trích xuất URL từ Google Image Search: " + url);
            return null;
        }
        
        // 4. Validate và return direct image URL (bất kỳ URL nào từ internet)
        if (isValidImageUrl(url)) {
            Log.d(TAG, "URL ảnh hợp lệ: " + url);
            return url;
        }
        
        // 5. Nếu không match pattern nào, nhưng là HTTP/HTTPS URL, vẫn thử load
        // (Glide sẽ tự xử lý và fallback nếu không load được)
        if (isHttpUrl(url)) {
            Log.d(TAG, "URL HTTP/HTTPS (sẽ thử tải): " + url);
            return url;
        }
        
        Log.w(TAG, "URL ảnh không hợp lệ hoặc không được hỗ trợ: " + url);
        return null;
    }
    
    /**
     * Kiểm tra xem URL có phải là local drawable resource không
     */
    public static boolean isLocalDrawable(String url) {
        return url != null && url.startsWith("pic_");
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
     * Convert Google Drive sharing link thành direct image URL
     * 
     * Formats hỗ trợ:
     * 1. https://drive.google.com/file/d/FILE_ID/view?usp=sharing
     * 2. https://drive.google.com/open?id=FILE_ID
     * 3. https://drive.google.com/uc?export=view&id=FILE_ID (đã là direct URL)
     * 4. https://docs.google.com/uc?id=FILE_ID
     * 
     * @param googleDriveUrl Google Drive sharing link hoặc direct URL
     * @return Direct image URL hoặc null nếu không thể extract FILE_ID
     */
    public static String convertGoogleDriveToDirectUrl(String googleDriveUrl) {
        if (googleDriveUrl == null || googleDriveUrl.isEmpty()) {
            return null;
        }
        
        // Nếu đã là direct URL format, return as is
        if (googleDriveUrl.contains("uc?export=view") || googleDriveUrl.contains("uc?id=")) {
            // Đảm bảo format đúng
            if (googleDriveUrl.contains("export=view")) {
                return googleDriveUrl;
            } else if (googleDriveUrl.contains("uc?id=")) {
                // Convert docs.google.com format
                return googleDriveUrl.replace("uc?id=", "uc?export=view&id=");
            }
        }
        
        // Extract FILE_ID từ URL
        String fileId = extractGoogleDriveFileId(googleDriveUrl);
        if (fileId == null || fileId.isEmpty()) {
            Log.w(TAG, "Không thể trích xuất FILE_ID từ Google Drive URL: " + googleDriveUrl);
            return null;
        }
        
        // Tạo direct image URL
        // Format: https://drive.google.com/uc?export=view&id=FILE_ID
        String directUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
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
     * Kiểm tra xem URL có phải là Google Image Search URL không
     */
    public static boolean isGoogleImageSearchUrl(String url) {
        return url != null && url.contains("google.com/imgres");
    }
    
    /**
     * Extract direct image URL từ Google Image Search URL
     */
    public static String extractDirectUrlFromGoogleImageSearch(String googleImageSearchUrl) {
        if (googleImageSearchUrl == null || googleImageSearchUrl.isEmpty()) {
            return null;
        }
        
        try {
            Matcher matcher = GOOGLE_IMAGE_SEARCH_PATTERN.matcher(googleImageSearchUrl);
            if (matcher.find()) {
                String encodedUrl = matcher.group(1);
                // URL decode
                String decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8");
                Log.d(TAG, "Đã trích xuất URL trực tiếp từ Google Image Search: " + decodedUrl);
                return decodedUrl;
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi trích xuất URL từ Google Image Search: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Kiểm tra xem URL có hợp lệ để load image không
     * Chấp nhận các format:
     * - HTTP/HTTPS URLs với image extension (.jpg, .jpeg, .png, .gif, .webp, .bmp)
     * - Google Drive direct URLs
     * - Bất kỳ HTTP/HTTPS URL nào (Glide sẽ tự xử lý)
     */
    public static boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Check if it's a valid HTTP/HTTPS URL
        // Pattern 1: URLs với image extension
        Pattern imageExtensionPattern = Pattern.compile(
            "^(https?://).+\\.(jpg|jpeg|png|gif|webp|bmp|svg)(\\?.*)?$",
            Pattern.CASE_INSENSITIVE
        );
        
        // Pattern 2: Google Drive direct URL
        Pattern googleDrivePattern = Pattern.compile(
            "^(https?://).*drive\\.google\\.com/uc\\?export=view",
            Pattern.CASE_INSENSITIVE
        );
        
        // Pattern 3: Bất kỳ HTTP/HTTPS URL nào (Glide sẽ tự xử lý)
        Pattern httpPattern = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);
        
        return imageExtensionPattern.matcher(url).matches() ||
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
     * Get Google Drive thumbnail URL (smaller size, faster load)
     * 
     * @param fileId Google Drive FILE_ID
     * @param width Thumbnail width (default: 1000)
     * @param height Thumbnail height (default: 1000)
     * @return Thumbnail URL
     */
    public static String getGoogleDriveThumbnailUrl(String fileId, int width, int height) {
        if (fileId == null || fileId.isEmpty()) {
            return null;
        }
        return "https://drive.google.com/thumbnail?id=" + fileId + "&sz=w" + width + "-h" + height;
    }
    
    /**
     * Get Google Drive thumbnail URL với kích thước mặc định
     */
    public static String getGoogleDriveThumbnailUrl(String fileId) {
        return getGoogleDriveThumbnailUrl(fileId, 1000, 1000);
    }
}

