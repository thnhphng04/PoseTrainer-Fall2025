package fpt.fall2025.posetrainer.Helper;

import android.content.Context;
import android.widget.ImageView;
import android.util.Log;
import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.R;

/**
 * Helper class để load images với Glide
 * Hỗ trợ tất cả các loại URL ảnh
 */
public class GlideImageLoader {
    private static final String TAG = "GlideImageLoader";
    
    /**
     * Load image vào ImageView với error handling tự động
     * Hỗ trợ:
     * - Local drawable resources (pic_*)
     * - Direct image URLs từ internet
     * - Google Drive URLs
     * - Google Image Search URLs
     * - Bất kỳ URL nào Glide có thể load được
     * 
     * @param context Context
     * @param imageUrl URL ảnh (bất kỳ format nào)
     * @param imageView ImageView để load ảnh vào
     * @param placeholderResId Placeholder resource ID (null để dùng default)
     * @param errorResId Error resource ID (null để dùng default)
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView, 
                                Integer placeholderResId, Integer errorResId) {
        if (context == null || imageView == null) {
            Log.w(TAG, "Context hoặc ImageView là null");
            return;
        }
        
        // Default placeholder và error image
        int placeholder = placeholderResId != null ? placeholderResId : R.drawable.pic_1_1;
        int error = errorResId != null ? errorResId : R.drawable.pic_1_1;
        
        // Sanitize URL
        String sanitizedUrl = ImageUrlHelper.sanitizeImageUrl(imageUrl);
        
        if (sanitizedUrl == null) {
            // URL không hợp lệ, load default image
            Log.w(TAG, "URL không hợp lệ, đang tải ảnh mặc định: " + imageUrl);
            try {
                Glide.with(context)
                        .load(placeholder)
                        .into(imageView);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải ảnh mặc định: " + e.getMessage());
            }
            return;
        }
        
        // Nếu là local drawable resource
        if (ImageUrlHelper.isLocalDrawable(sanitizedUrl)) {
            try {
                int resId = context.getResources().getIdentifier(
                    sanitizedUrl, "drawable", context.getPackageName()
                );
                if (resId != 0) {
                    Glide.with(context)
                            .load(resId)
                            .placeholder(placeholder)
                            .error(error)
                            .into(imageView);
                } else {
                    // Drawable không tồn tại, load default
                    Log.w(TAG, "Không tìm thấy drawable: " + sanitizedUrl);
                    Glide.with(context)
                            .load(placeholder)
                            .into(imageView);
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tải drawable local: " + e.getMessage());
                Glide.with(context)
                        .load(placeholder)
                        .into(imageView);
            }
            return;
        }
        
        // Load remote URL (bất kỳ URL nào từ internet)
        try {
            Glide.with(context)
                    .load(sanitizedUrl)
                    .placeholder(placeholder)
                    .error(error)
                    .skipMemoryCache(false) // Cache để tăng performance
                    .into(imageView);
            
            Log.d(TAG, "Đang tải ảnh từ URL: " + sanitizedUrl);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tải ảnh từ URL: " + sanitizedUrl + ", Lỗi: " + e.getMessage());
            // Fallback to error image
            try {
                Glide.with(context)
                        .load(error)
                        .into(imageView);
            } catch (Exception ex) {
                Log.e(TAG, "Lỗi khi tải ảnh lỗi: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Load image với default placeholder và error image
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        loadImage(context, imageUrl, imageView, null, null);
    }
}

