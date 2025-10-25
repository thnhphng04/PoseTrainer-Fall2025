package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

import fpt.fall2025.posetrainer.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtEmail;
    private Button btnReset, btnOpenGmail, btnBackLogin;
    private ProgressBar progress;
    private LinearLayout postActions;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtEmail     = findViewById(R.id.edtEmail);
        btnReset     = findViewById(R.id.btnReset);
        btnOpenGmail = findViewById(R.id.btnOpenGmail);
        btnBackLogin = findViewById(R.id.btnBackLogin);
        progress     = findViewById(R.id.progress);
        postActions  = findViewById(R.id.postActions);
        mAuth        = FirebaseAuth.getInstance();

        btnReset.setOnClickListener(v -> onSendClicked());
        btnOpenGmail.setOnClickListener(v -> openEmailApp());
        btnBackLogin.setOnClickListener(v -> backToLogin());
    }

    private void onSendClicked() {
        String email = edtEmail.getText().toString().trim().toLowerCase(Locale.ROOT);

        if (email.isEmpty()) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Địa chỉ email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        setLoading(true);

        // Thử đọc provider nhưng không chặn UX
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    List<String> methods = result.getSignInMethods();
                    Log.d("FORGOT", "signInMethods=" + methods);

                    boolean onlySocialNoPassword = methods != null
                            && !methods.contains("password")
                            && (methods.contains("google.com")
                            || methods.contains("facebook.com")
                            || methods.contains("apple.com"));

                    if (onlySocialNoPassword) {
                        setLoading(false);
                        Toast.makeText(this,
                                "Email này đăng nhập bằng Google/nhà cung cấp khác, không có mật khẩu để đặt lại.\n"
                                        + "Hãy đăng nhập Google hoặc tạo mật khẩu trong phần cài đặt tài khoản.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        sendResetEmail(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("FORGOT", "fetchSignInMethods failed: " + e.getMessage());
                    // Không cản trở UX
                    sendResetEmail(email);
                });
    }

    private void sendResetEmail(String email) {
        mAuth.setLanguageCode("vi");

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Đã gửi hướng dẫn đặt lại mật khẩu.\n" +
                                    "Hãy kiểm tra Hộp thư đến và cả Thư rác (Spam).",
                            Toast.LENGTH_LONG).show();
                    // Hiện các nút sau khi gửi
                    postActions.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Gửi email thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnReset.setEnabled(!loading);
        edtEmail.setEnabled(!loading);
    }

    private void openEmailApp() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        } catch (Exception ignored) {}

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng email trên thiết bị",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void backToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
