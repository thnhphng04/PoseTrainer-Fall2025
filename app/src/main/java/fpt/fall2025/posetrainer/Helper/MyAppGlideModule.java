package fpt.fall2025.posetrainer.Helper;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Glide module để cấu hình Glide cho ứng dụng
 * Giúp tránh cảnh báo về thiếu AppGlideModule
 */
@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    
    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        super.registerComponents(context, glide, registry);
    }
    
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}

