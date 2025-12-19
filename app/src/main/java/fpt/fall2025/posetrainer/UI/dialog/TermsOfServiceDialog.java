package fpt.fall2025.posetrainer.UI.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import fpt.fall2025.posetrainer.R;

public class TermsOfServiceDialog extends Dialog {
    
    private ScrollView scrollView;
    private TextView tvTermsContent;
    private Button btnCancel;
    private Button btnAgree;
    private OnAgreeListener listener;
    
    public interface OnAgreeListener {
        void onAgree();
        void onCancel();
    }
    
    public TermsOfServiceDialog(@NonNull Context context) {
        super(context);
    }
    
    public void setOnAgreeListener(OnAgreeListener listener) {
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_terms_of_service);
        
        scrollView = findViewById(R.id.scroll_terms);
        tvTermsContent = findViewById(R.id.tv_terms_content);
        btnCancel = findViewById(R.id.btn_cancel);
        btnAgree = findViewById(R.id.btn_agree);
        
        // Set terms content
        String termsText = getTermsOfServiceText();
        tvTermsContent.setText(termsText);
        
        // Initially disable agree button and set white color
        btnAgree.setEnabled(false);
        btnAgree.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF)); // White
        btnAgree.setTextColor(0xFF101322); // Dark text
        
        // Detect scroll to bottom
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                checkScrollPosition();
            }
        });
        
        // Also check when content is loaded and after layout
        tvTermsContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                tvTermsContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Post to check after layout is complete
                scrollView.post(() -> checkScrollPosition());
            }
        });
        
        // Check initial position after dialog is shown
        scrollView.post(() -> checkScrollPosition());
        
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });
        
        btnAgree.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAgree();
            }
            dismiss();
        });
    }
    
    private void checkScrollPosition() {
        if (scrollView != null && tvTermsContent != null && scrollView.getChildCount() > 0) {
            View child = scrollView.getChildAt(0);
            if (child != null) {
                int scrollY = scrollView.getScrollY();
                int height = scrollView.getHeight();
                int childHeight = child.getMeasuredHeight();
                
                // Enable button when scrolled to bottom (with 100px tolerance for better UX)
                // Check if scroll position + visible height >= total content height
                boolean isAtBottom = (scrollY + height) >= (childHeight - 100);
                btnAgree.setEnabled(isAtBottom);
                
                // Thay đổi màu nút: trắng khi chưa scroll hết, xanh khi đã scroll hết
                if (isAtBottom) {
                    // Màu xanh khi đã scroll hết
                    btnAgree.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
                    btnAgree.setTextColor(0xFFFFFFFF); // White text
                } else {
                    // Màu trắng khi chưa scroll hết
                    btnAgree.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF)); // White
                    btnAgree.setTextColor(0xFF101322); // Dark text
                }
            }
        }
    }
    
    private String getTermsOfServiceText() {
        return "ĐIỀU KHOẢN SỬ DỤNG ỨNG DỤNG POSETRAINER\n\n" +
                "1. CHẤP NHẬN ĐIỀU KHOẢN\n\n" +
                "Bằng việc sử dụng ứng dụng PoseTrainer, bạn đồng ý tuân thủ và bị ràng buộc bởi các điều khoản sử dụng này. Nếu bạn không đồng ý với bất kỳ điều khoản nào, vui lòng không sử dụng ứng dụng.\n\n" +
                "2. MÔ TẢ DỊCH VỤ\n\n" +
                "PoseTrainer là một ứng dụng di động cung cấp các tính năng:\n" +
                "- Hướng dẫn và theo dõi các bài tập thể dục\n" +
                "- Tạo kế hoạch tập luyện cá nhân\n" +
                "- Cộng đồng chia sẻ và tương tác\n" +
                "- Theo dõi tiến độ tập luyện\n\n" +
                "3. ĐĂNG KÝ TÀI KHOẢN\n\n" +
                "Để sử dụng một số tính năng của ứng dụng, bạn cần đăng ký tài khoản. Bạn cam kết:\n" +
                "- Cung cấp thông tin chính xác, đầy đủ và cập nhật\n" +
                "- Bảo mật thông tin đăng nhập của bạn\n" +
                "- Chịu trách nhiệm cho mọi hoạt động diễn ra dưới tài khoản của bạn\n" +
                "- Thông báo ngay cho chúng tôi nếu phát hiện vi phạm bảo mật\n\n" +
                "4. QUYỀN SỞ HỮU TRÍ TUỆ\n\n" +
                "Tất cả nội dung trong ứng dụng, bao gồm nhưng không giới hạn ở văn bản, hình ảnh, logo, biểu tượng, phần mềm, đều thuộc quyền sở hữu của PoseTrainer hoặc các bên cấp phép. Bạn không được sao chép, phân phối, hoặc sử dụng nội dung này cho mục đích thương mại mà không có sự cho phép bằng văn bản.\n\n" +
                "5. SỬ DỤNG ỨNG DỤNG\n\n" +
                "Bạn đồng ý không sử dụng ứng dụng để:\n" +
                "- Vi phạm bất kỳ luật pháp hoặc quy định nào\n" +
                "- Gửi hoặc truyền nội dung bất hợp pháp, có hại, đe dọa, lạm dụng, quấy rối, phỉ báng, khiêu dâm, hoặc vi phạm quyền riêng tư\n" +
                "- Giả mạo danh tính hoặc đại diện sai về mối quan hệ của bạn với bất kỳ cá nhân hoặc tổ chức nào\n" +
                "- Can thiệp hoặc phá vỡ hoạt động của ứng dụng hoặc máy chủ\n\n" +
                "6. QUYỀN RIÊNG TƯ\n\n" +
                "Chúng tôi cam kết bảo vệ quyền riêng tư của bạn. Thông tin cá nhân của bạn sẽ được xử lý theo Chính sách Bảo mật của chúng tôi. Bằng việc sử dụng ứng dụng, bạn đồng ý với việc thu thập và sử dụng thông tin theo Chính sách Bảo mật.\n\n" +
                "7. TỪ CHỐI TRÁCH NHIỆM\n\n" +
                "Ứng dụng được cung cấp \"như hiện tại\" và \"như có sẵn\". Chúng tôi không đảm bảo rằng ứng dụng sẽ hoạt động không bị gián đoạn hoặc không có lỗi. Chúng tôi không chịu trách nhiệm cho bất kỳ thiệt hại nào phát sinh từ việc sử dụng hoặc không thể sử dụng ứng dụng.\n\n" +
                "8. GIỚI HẠN TRÁCH NHIỆM\n\n" +
                "Trong phạm vi tối đa được pháp luật cho phép, PoseTrainer và các đối tác, giám đốc, nhân viên, đại lý, nhà cung cấp không chịu trách nhiệm cho bất kỳ thiệt hại gián tiếp, ngẫu nhiên, đặc biệt, hậu quả hoặc thiệt hại phát sinh từ việc sử dụng hoặc không thể sử dụng ứng dụng.\n\n" +
                "9. THAY ĐỔI ĐIỀU KHOẢN\n\n" +
                "Chúng tôi có quyền sửa đổi các điều khoản này bất cứ lúc nào. Các thay đổi sẽ có hiệu lực ngay sau khi được đăng tải trên ứng dụng. Việc bạn tiếp tục sử dụng ứng dụng sau khi có thay đổi được coi là bạn đã chấp nhận các điều khoản mới.\n\n" +
                "10. CHẤM DỨT\n\n" +
                "Chúng tôi có quyền chấm dứt hoặc tạm ngưng quyền truy cập của bạn vào ứng dụng ngay lập tức, không cần thông báo trước, vì bất kỳ lý do nào, bao gồm nhưng không giới hạn ở vi phạm các điều khoản này.\n\n" +
                "11. LUẬT ÁP DỤNG\n\n" +
                "Các điều khoản này được điều chỉnh bởi và được giải thích theo pháp luật Việt Nam. Bất kỳ tranh chấp nào phát sinh từ hoặc liên quan đến các điều khoản này sẽ được giải quyết tại tòa án có thẩm quyền tại Việt Nam.\n\n" +
                "12. LIÊN HỆ\n\n" +
                "Nếu bạn có bất kỳ câu hỏi nào về các điều khoản này, vui lòng liên hệ với chúng tôi qua email: support@posetrainer.com\n\n" +
                "Bằng việc nhấn nút \"Đồng ý\" bên dưới, bạn xác nhận rằng bạn đã đọc, hiểu và đồng ý với tất cả các điều khoản sử dụng trên.";
    }
}

