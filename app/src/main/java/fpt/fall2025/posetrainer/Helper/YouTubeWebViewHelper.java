package fpt.fall2025.posetrainer.Helper;

import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Helper class để load YouTube videos trong WebView
 */
public class YouTubeWebViewHelper {
    private static final String TAG = "YouTubeWebViewHelper";
    
    /**
     * Load YouTube video vào WebView
     * 
     * @param webView WebView để load video
     * @param youtubeUrl YouTube URL (watch, youtu.be, embed format)
     * @param onErrorCallback Callback khi có lỗi (null nếu không cần)
     */
    public static void loadYouTubeVideo(WebView webView, String youtubeUrl, OnYouTubeErrorListener onErrorCallback) {
        if (webView == null) {
            Log.w(TAG, "WebView là null");
            return;
        }
        
        if (youtubeUrl == null || youtubeUrl.isEmpty()) {
            Log.w(TAG, "URL YouTube rỗng hoặc null");
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL YouTube rỗng hoặc null");
            }
            return;
        }
        
        // Convert YouTube URL thành embed URL
        String embedUrl = VideoUrlHelper.convertYouTubeToEmbedUrl(youtubeUrl);
        if (embedUrl == null) {
            Log.e(TAG, "Không thể chuyển đổi YouTube URL sang embed URL: " + youtubeUrl);
            if (onErrorCallback != null) {
                onErrorCallback.onError("URL YouTube không hợp lệ: " + youtubeUrl);
            }
            return;
        }
        
        try {
            Log.d(TAG, "Đang tải video YouTube trong WebView: " + embedUrl);
            
            // Configure WebView settings
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true); // Required for YouTube embed
            webSettings.setDomStorageEnabled(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false); // Allow autoplay (optional)
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(false);
            webSettings.setSupportZoom(false);
            webSettings.setAllowFileAccess(false);
            webSettings.setAllowContentAccess(false);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            
            // Set user agent để tránh YouTube block
            String userAgent = webSettings.getUserAgentString();
            webSettings.setUserAgentString(userAgent);
            
            // Enable hardware acceleration (important for video playback)
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
            
            // Set background color (tránh màn hình trắng)
            webView.setBackgroundColor(0x00000000); // Transparent
            webView.setBackgroundResource(android.R.color.transparent);
            
            // Set WebViewClient để handle page navigation
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // Don't override URL loading, let WebView handle it
                    Log.d(TAG, "WebView đang điều hướng đến: " + url);
                    return false;
                }
                
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "WebView đã tải xong trang: " + url);
                }
                
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "Lỗi WebView: " + description + " (mã: " + errorCode + ") URL: " + failingUrl);
                    if (onErrorCallback != null) {
                        onErrorCallback.onError("Lỗi WebView: " + description);
                    }
                }
                
                @Override
                public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                    Log.e(TAG, "Lỗi HTTP WebView: " + errorResponse.getStatusCode() + " URL: " + request.getUrl());
                }
            });
            
            // Set WebChromeClient để handle video playback
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    Log.d(TAG, "Tiến trình tải WebView: " + newProgress + "%");
                    if (newProgress == 100) {
                        Log.d(TAG, "Trang video YouTube đã tải thành công");
                    }
                }
                
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    Log.d(TAG, "Console WebView: " + consoleMessage.message());
                    return true; // Message handled
                }
            });
            
            // Sử dụng HTML với iframe để đảm bảo YouTube load đúng cách
            String videoId = VideoUrlHelper.extractYouTubeVideoId(youtubeUrl);
            if (videoId != null) {
                // Tạo HTML với iframe để embed YouTube video
                String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <style>\n" +
                    "        body {\n" +
                    "            margin: 0;\n" +
                    "            padding: 0;\n" +
                    "            background-color: #000;\n" +
                    "        }\n" +
                    "        iframe {\n" +
                    "            width: 100%;\n" +
                    "            height: 100%;\n" +
                    "            border: 0;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <iframe src=\"https://www.youtube.com/embed/" + videoId + "?autoplay=0&rel=0&modestbranding=1&playsinline=1\"\n" +
                    "            frameborder=\"0\"\n" +
                    "            allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\"\n" +
                    "            allowfullscreen>\n" +
                    "    </iframe>\n" +
                    "</body>\n" +
                    "</html>";
                
                // Load HTML thay vì URL trực tiếp
                webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null);
                Log.d(TAG, "Đang tải video YouTube sử dụng HTML iframe");
            } else {
                // Fallback: load URL trực tiếp
                webView.loadUrl(embedUrl);
                Log.d(TAG, "Đang tải video YouTube sử dụng URL trực tiếp");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tải video YouTube: " + e.getMessage(), e);
            if (onErrorCallback != null) {
                onErrorCallback.onError("Lỗi khi tải video YouTube: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load YouTube video với default error handling
     */
    public static void loadYouTubeVideo(WebView webView, String youtubeUrl) {
        loadYouTubeVideo(webView, youtubeUrl, null);
    }
    
    /**
     * Clean up WebView resources
     */
    public static void cleanup(WebView webView) {
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.clearHistory();
                webView.clearCache(true);
                Log.d(TAG, "Đã dọn dẹp WebView");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi dọn dẹp WebView: " + e.getMessage());
            }
        }
    }
    
    /**
     * Interface for YouTube error callbacks
     */
    public interface OnYouTubeErrorListener {
        void onError(String errorMessage);
    }
}

