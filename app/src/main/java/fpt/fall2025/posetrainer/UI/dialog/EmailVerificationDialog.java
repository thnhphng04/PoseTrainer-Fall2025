package fpt.fall2025.posetrainer.UI.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseTooManyRequestsException;

import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;

/**
 * Dialog để thông báo và xử lý xác minh email
 * Hiển thị sau khi đăng ký tài khoản thành công
 */
public class EmailVerificationDialog extends DialogFragment {
    private static final String TAG = "EmailVerificationDialog";
    private static final String ARG_EMAIL = "email";
    
    private TextView tvMessage;
    private TextView tvEmail;
    private Button btnResend;
    private Button btnVerified;
    private ImageButton btnClose;
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    
    private String email;
    private AuthService authService;
    private OnVerificationCompleteListener listener;
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private Handler resendCooldownHandler;
    private Runnable resendCooldownRunnable;
    private static final long POLLING_INTERVAL = 3000; // 3 giây
    private static final int MAX_POLLING_ATTEMPTS = 60; // Tối đa 3 phút
    private static final long RESEND_COOLDOWN = 60000; // 60 giây cooldown sau khi rate limit
    private int pollingAttempts = 0;
    private boolean isResendDisabled = false;
    private boolean isEmailVerified = false; // Flag để track xem email đã được xác minh chưa

    /**
     * Interface để callback khi người dùng đã xác minh hoặc đóng dialog
     */
    public interface OnVerificationCompleteListener {
        void onVerified();
        void onDialogClosed(); // Khi dialog bị đóng mà chưa xác minh
    }

    /**
     * Tạo instance mới của dialog với email
     */
    public static EmailVerificationDialog newInstance(String email) {
        EmailVerificationDialog dialog = new EmailVerificationDialog();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Set listener để nhận callback
     */
    public void setOnVerificationCompleteListener(OnVerificationCompleteListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_email_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authService = new AuthService();
        
        // Lấy email từ arguments
        Bundle args = getArguments();
        if (args != null) {
            email = args.getString(ARG_EMAIL, "");
        }
        
        // Bind views
        tvMessage = view.findViewById(R.id.tv_message);
        tvEmail = view.findViewById(R.id.tv_email);
        btnResend = view.findViewById(R.id.btn_resend);
        btnVerified = view.findViewById(R.id.btn_verified);
        btnClose = view.findViewById(R.id.btn_close);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutContent = view.findViewById(R.id.layout_content);
        
        // Ẩn nút bỏ qua (nếu có trong layout)
        View btnSkip = view.findViewById(R.id.btn_skip);
        if (btnSkip != null) {
            btnSkip.setVisibility(android.view.View.GONE);
        }
        
        // Nút X để đóng dialog
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                stopPolling();
                stopResendCooldown();
                // Kiểm tra xem email đã được xác minh chưa
                if (!isEmailVerified) {
                    // Chưa xác minh, gọi callback để xóa tài khoản
                    if (listener != null) {
                        listener.onDialogClosed();
                    }
                }
                dismiss();
            });
        }
        
        // Hiển thị email
        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
        } else {
            FirebaseUser user = authService.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                email = user.getEmail();
                tvEmail.setText(email);
            } else {
                tvEmail.setVisibility(android.view.View.GONE);
            }
        }
        
        // Nút gửi lại email
        btnResend.setOnClickListener(v -> resendVerificationEmail());
        
        // Nút đã xác minh
        btnVerified.setOnClickListener(v -> checkEmailVerification());
        
        // Bắt đầu polling để tự động kiểm tra email verification
        startPolling();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
        stopResendCooldown();
        
        // Nếu dialog bị destroy mà chưa xác minh, gọi callback
        // Chỉ gọi nếu chưa được xác minh (tránh gọi khi đã xác minh thành công)
        if (!isEmailVerified && listener != null) {
            listener.onDialogClosed();
        }
    }
    
    /**
     * Bắt đầu polling để tự động kiểm tra email verification
     */
    private void startPolling() {
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingAttempts = 0;
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (pollingAttempts >= MAX_POLLING_ATTEMPTS) {
                    // Đã hết thời gian polling, dừng lại
                    stopPolling();
                    return;
                }
                
                pollingAttempts++;
                checkEmailVerificationSilently();
                
                // Lên lịch kiểm tra tiếp
                if (pollingHandler != null) {
                    pollingHandler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        };
        
        // Bắt đầu polling sau 3 giây đầu tiên
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }
    
    /**
     * Dừng polling
     */
    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingHandler = null;
            pollingRunnable = null;
        }
    }
    
    /**
     * Kiểm tra email verification một cách im lặng (không hiển thị loading)
     */
    private void checkEmailVerificationSilently() {
        FirebaseUser user = authService.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    authService.refreshUser();
                    FirebaseUser reloadedUser = authService.getCurrentUser();
                    if (reloadedUser != null && reloadedUser.isEmailVerified()) {
                        // Email đã được xác minh!
                        isEmailVerified = true;
                        stopPolling();
                        if (listener != null) {
                            listener.onVerified();
                        }
                        dismiss();
                    }
                }
            });
        }
    }

    /**
     * Gửi lại email xác minh
     */
    private void resendVerificationEmail() {
        if (isResendDisabled) {
            Toast.makeText(getContext(),
                    "Vui lòng đợi một chút trước khi gửi lại email. Firebase đã tạm thời chặn yêu cầu do quá nhiều lần thử.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        
        setLoading(true);
        
        authService.sendEmailVerification(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(getContext(),
                        "Đã gửi lại email xác minh. Vui lòng kiểm tra hộp thư đến và cả thư rác (Spam).",
                        Toast.LENGTH_LONG).show();
            } else {
                Exception exception = task.getException();
                String errorMessage;
                
                // Xử lý lỗi rate limit
                if (exception instanceof com.google.firebase.FirebaseTooManyRequestsException) {
                    errorMessage = "Firebase đã tạm thời chặn yêu cầu do quá nhiều lần thử. Vui lòng đợi 1-2 phút trước khi thử lại.";
                    // Disable nút gửi lại trong 60 giây
                    disableResendButton();
                } else {
                    errorMessage = "Không thể gửi email: ";
                    if (exception != null) {
                        errorMessage += exception.getMessage();
                    }
                }
                
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error resending verification email", exception);
            }
        });
    }
    
    /**
     * Disable nút gửi lại trong một khoảng thời gian (cooldown)
     */
    private void disableResendButton() {
        isResendDisabled = true;
        btnResend.setEnabled(false);
        btnResend.setText("Đang chờ... (60s)");
        
        resendCooldownHandler = new Handler(Looper.getMainLooper());
        final int[] countdown = {60};
        
        resendCooldownRunnable = new Runnable() {
            @Override
            public void run() {
                countdown[0]--;
                if (countdown[0] > 0) {
                    btnResend.setText("Đang chờ... (" + countdown[0] + "s)");
                    resendCooldownHandler.postDelayed(this, 1000);
                } else {
                    // Hết cooldown, enable lại nút
                    isResendDisabled = false;
                    btnResend.setEnabled(true);
                    btnResend.setText("Gửi lại email");
                }
            }
        };
        
        resendCooldownHandler.postDelayed(resendCooldownRunnable, 1000);
    }
    
    /**
     * Dừng cooldown timer
     */
    private void stopResendCooldown() {
        if (resendCooldownHandler != null && resendCooldownRunnable != null) {
            resendCooldownHandler.removeCallbacks(resendCooldownRunnable);
            resendCooldownHandler = null;
            resendCooldownRunnable = null;
        }
    }

    /**
     * Kiểm tra email đã được xác minh chưa (manual check)
     */
    private void checkEmailVerification() {
        setLoading(true);
        stopPolling(); // Dừng polling khi user check manually
        
        // Reload user để lấy trạng thái mới nhất
        FirebaseUser user = authService.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(reloadTask -> {
                if (reloadTask.isSuccessful()) {
                    // Sau khi reload, lấy lại user từ authService để đảm bảo có data mới nhất
                    authService.refreshUser();
                    FirebaseUser reloadedUser = authService.getCurrentUser();
                    if (reloadedUser != null && reloadedUser.isEmailVerified()) {
                        isEmailVerified = true;
                        setLoading(false);
                        Toast.makeText(getContext(), "Email đã được xác minh thành công!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onVerified();
                        }
                        dismiss();
                    } else {
                        setLoading(false);
                        Toast.makeText(getContext(),
                                "Email chưa được xác minh. Vui lòng kiểm tra email và nhấp vào liên kết xác minh.",
                                Toast.LENGTH_LONG).show();
                        // Tiếp tục polling
                        startPolling();
                    }
                } else {
                    setLoading(false);
                    Toast.makeText(getContext(),
                            "Không thể kiểm tra trạng thái xác minh. Vui lòng thử lại.",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error reloading user", reloadTask.getException());
                    // Tiếp tục polling
                    startPolling();
                }
            });
        } else {
            setLoading(false);
            Toast.makeText(getContext(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hiển thị/ẩn loading state
     */
    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(android.view.View.VISIBLE);
            layoutContent.setVisibility(android.view.View.GONE);
            btnResend.setEnabled(false);
            btnVerified.setEnabled(false);
        } else {
            progressBar.setVisibility(android.view.View.GONE);
            layoutContent.setVisibility(android.view.View.VISIBLE);
            // Chỉ enable nút resend nếu không đang trong cooldown
            btnResend.setEnabled(!isResendDisabled);
            btnVerified.setEnabled(true);
        }
    }
}

