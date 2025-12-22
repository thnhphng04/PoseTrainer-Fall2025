package fpt.fall2025.posetrainer.UI.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.ExerciseUser;
import fpt.fall2025.posetrainer.R;

public class CreateCustomExerciseActivity extends AppCompatActivity {
    private static final String TAG = "CreateCustomExercise";

    private EditText edtExerciseName;
    private EditText edtExerciseDescription;
    private EditText edtNumberOfSteps;
    private EditText edtCommonErrors;
    private Spinner spinnerExerciseType;
    private CheckBox cbCheckArms;
    private CheckBox cbCheckLegs;
    private CheckBox cbCheckBody;
    private CheckBox cbCheckHips;
    private ImageView ivImage1, ivImage2, ivImage3, ivImage4, ivImage5, ivImage6;
    private LinearLayout layoutImage1, layoutImage2, layoutImage3, layoutImage4, layoutImage5, layoutImage6;
    private Button btnCreateExercise;
    private ImageButton btnBack;
    private ProgressBar progress;
    private TextView tvStatus;
    private TextView tvImageUploadTitle;
    private TextView tvImageUploadHint;

    private static final int MAX_IMAGES = 10; // Tăng từ 6 lên 10 để hỗ trợ nhiều bước hơn
    private static final int MIN_IMAGES = 4;
    private Uri[] imageUris = new Uri[MAX_IMAGES];
    private String[] imageBase64 = new String[MAX_IMAGES];
    private int numberOfSteps = 6; // Số bước mặc định

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;

    private int currentImageIndex = -1;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null && currentImageIndex >= 0 && currentImageIndex < MAX_IMAGES) {
                            imageUris[currentImageIndex] = uri;
                            loadImageToView(uri, currentImageIndex);
                            convertImageToBase64(uri, currentImageIndex);
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_custom_exercise);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        edtExerciseName = findViewById(R.id.edtExerciseName);
        edtExerciseDescription = findViewById(R.id.edtExerciseDescription);
        edtNumberOfSteps = findViewById(R.id.edtNumberOfSteps);
        edtCommonErrors = findViewById(R.id.edtCommonErrors);
        spinnerExerciseType = findViewById(R.id.spinnerExerciseType);
        cbCheckArms = findViewById(R.id.cbCheckArms);
        cbCheckLegs = findViewById(R.id.cbCheckLegs);
        cbCheckBody = findViewById(R.id.cbCheckBody);
        cbCheckHips = findViewById(R.id.cbCheckHips);
        ivImage1 = findViewById(R.id.ivImage1);
        ivImage2 = findViewById(R.id.ivImage2);
        ivImage3 = findViewById(R.id.ivImage3);
        ivImage4 = findViewById(R.id.ivImage4);
        ivImage5 = findViewById(R.id.ivImage5);
        ivImage6 = findViewById(R.id.ivImage6);
        layoutImage1 = findViewById(R.id.layoutImage1);
        layoutImage2 = findViewById(R.id.layoutImage2);
        layoutImage3 = findViewById(R.id.layoutImage3);
        layoutImage4 = findViewById(R.id.layoutImage4);
        layoutImage5 = findViewById(R.id.layoutImage5);
        layoutImage6 = findViewById(R.id.layoutImage6);
        btnCreateExercise = findViewById(R.id.btnCreateExercise);
        btnBack = findViewById(R.id.btnBack);
        progress = findViewById(R.id.progress);
        tvStatus = findViewById(R.id.tvStatus);
        tvImageUploadTitle = findViewById(R.id.tvImageUploadTitle);
        tvImageUploadHint = findViewById(R.id.tvImageUploadHint);
        
        // Setup exercise type spinner
        String[] exerciseTypes = {
            "Push-up / Plank (Tay)",
            "Squat / Lunge (Chân)",
            "Pull-up / Row (Kéo)",
            "Core / Abs (Bụng)",
            "Cardio / Jump (Nhảy)",
            "Stretch / Yoga (Kéo giãn)",
            "Khác"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exerciseTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExerciseType.setAdapter(adapter);
        
        // Set default number of steps
        updateImageViewsVisibility(6);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        layoutImage1.setOnClickListener(v -> selectImage(0));
        layoutImage2.setOnClickListener(v -> selectImage(1));
        layoutImage3.setOnClickListener(v -> selectImage(2));
        layoutImage4.setOnClickListener(v -> selectImage(3));
        layoutImage5.setOnClickListener(v -> selectImage(4));
        layoutImage6.setOnClickListener(v -> selectImage(5));

        btnCreateExercise.setOnClickListener(v -> createExercise());
        
        // Listen for number of steps changes
        edtNumberOfSteps.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateNumberOfSteps();
            }
        });
        
        // Also listen for text changes
        edtNumberOfSteps.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateNumberOfSteps();
            }
        });
    }
    
    private void updateNumberOfSteps() {
        try {
            String stepsText = edtNumberOfSteps.getText().toString().trim();
            if (stepsText.isEmpty()) {
                return;
            }
            
            int steps = Integer.parseInt(stepsText);
            if (steps < MIN_IMAGES || steps > MAX_IMAGES) {
                Toast.makeText(this, "Số bước phải từ " + MIN_IMAGES + " đến " + MAX_IMAGES, Toast.LENGTH_SHORT).show();
                edtNumberOfSteps.setText(String.valueOf(numberOfSteps));
                return;
            }
            
            numberOfSteps = steps;
            updateImageViewsVisibility(steps);
            Log.d(TAG, "Number of steps updated to: " + steps);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid number format: " + e.getMessage());
            edtNumberOfSteps.setText(String.valueOf(numberOfSteps));
        }
    }
    
    private void updateImageViewsVisibility(int steps) {
        // Update title and hint - Thêm hướng dẫn về góc camera side view
        tvImageUploadTitle.setText("Upload " + steps + " ảnh mô tả các trạng thái của động tác *");
        tvImageUploadHint.setText("⚠️ QUAN TRỌNG: Chụp ảnh từ góc NGANG NGƯỜI (side view - từ bên cạnh), KHÔNG phải chính diện.\n" +
                "Upload " + steps + " ảnh theo thứ tự các trạng thái của động tác. Mỗi ảnh mô tả một bước trong quá trình thực hiện động tác.\n" +
                "Đảm bảo camera nhìn thấy toàn bộ cơ thể người tập từ góc ngang để AI có thể phân tích chính xác.");
        
        // Show/hide image views based on number of steps
        layoutImage1.setVisibility(steps >= 1 ? View.VISIBLE : View.GONE);
        layoutImage2.setVisibility(steps >= 2 ? View.VISIBLE : View.GONE);
        layoutImage3.setVisibility(steps >= 3 ? View.VISIBLE : View.GONE);
        layoutImage4.setVisibility(steps >= 4 ? View.VISIBLE : View.GONE);
        layoutImage5.setVisibility(steps >= 5 ? View.VISIBLE : View.GONE);
        layoutImage6.setVisibility(steps >= 6 ? View.VISIBLE : View.GONE);
        
        // TODO: If steps > 6, we need to dynamically create more ImageViews
        // For now, we'll limit to 6 visible images, but accept up to 10 in the array
    }

    private void selectImage(int index) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1001);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
                return;
            }
        }

        currentImageIndex = index;
        pickImageLauncher.launch("image/*");
    }

    private void loadImageToView(Uri uri, int index) {
        ImageView imageView = null;
        switch (index) {
            case 0: imageView = ivImage1; break;
            case 1: imageView = ivImage2; break;
            case 2: imageView = ivImage3; break;
            case 3: imageView = ivImage4; break;
            case 4: imageView = ivImage5; break;
            case 5: imageView = ivImage6; break;
        }
        if (imageView != null) {
            Glide.with(this).load(uri).into(imageView);
        }
    }

    private void convertImageToBase64(Uri uri, int index) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: " + uri);
                Toast.makeText(this, "Không thể đọc ảnh. Vui lòng chọn ảnh khác.", Toast.LENGTH_SHORT).show();
                if (inputStream != null) {
                    inputStream.close();
                }
                return;
            }
            
            // Resize để giảm kích thước (max 600px để giảm kích thước base64)
            int maxSize = 600;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Bitmap resizedBitmap = bitmap;
            
            if (width > maxSize || height > maxSize) {
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = (int) (width * scale);
                int newHeight = (int) (height * scale);
                resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                // Recycle original bitmap if we created a new one
                if (resizedBitmap != bitmap) {
                    bitmap.recycle();
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // Use lower quality (70) to reduce size further
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            imageBase64[index] = base64;
            
            Log.d(TAG, "Image " + (index + 1) + " converted: " + imageBytes.length + " bytes, base64 length: " + base64.length());
            
            // Clean up
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to base64: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createExercise() {
        String exerciseName = edtExerciseName.getText().toString().trim();
        String exerciseDescription = edtExerciseDescription.getText().toString().trim();

        // Validate
        if (exerciseName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên bài tập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get number of steps
        try {
            String stepsText = edtNumberOfSteps.getText().toString().trim();
            if (stepsText.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số bước/trạng thái của động tác", Toast.LENGTH_SHORT).show();
                return;
            }
            numberOfSteps = Integer.parseInt(stepsText);
            if (numberOfSteps < MIN_IMAGES || numberOfSteps > MAX_IMAGES) {
                Toast.makeText(this, "Số bước phải từ " + MIN_IMAGES + " đến " + MAX_IMAGES, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số bước không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if number of images matches number of steps
        int selectedCount = 0;
        List<String> validImages = new ArrayList<>();
        for (int i = 0; i < numberOfSteps; i++) {
            if (imageBase64[i] != null && !imageBase64[i].isEmpty() && imageBase64[i].length() >= 100) {
                selectedCount++;
                validImages.add(imageBase64[i]);
                Log.d(TAG, "Image " + (i + 1) + " is valid, size: " + imageBase64[i].length() + " chars");
            }
        }

        if (selectedCount < numberOfSteps) {
            Toast.makeText(this, "Vui lòng chọn đủ " + numberOfSteps + " ảnh tương ứng với " + numberOfSteps + " bước của động tác", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setLoading(true);
        tvStatus.setText("Đang phân tích " + selectedCount + " ảnh với AI...");
        tvStatus.setVisibility(View.VISIBLE);

        Log.d(TAG, "Total valid images: " + selectedCount + " out of " + MAX_IMAGES);

        // Get additional information
        String exerciseType = spinnerExerciseType.getSelectedItem().toString();
        String commonErrors = edtCommonErrors.getText().toString().trim();
        
        // Build body parts to check
        List<String> bodyPartsToCheck = new ArrayList<>();
        if (cbCheckArms.isChecked()) bodyPartsToCheck.add("arms");
        if (cbCheckLegs.isChecked()) bodyPartsToCheck.add("legs");
        if (cbCheckBody.isChecked()) bodyPartsToCheck.add("body");
        if (cbCheckHips.isChecked()) bodyPartsToCheck.add("hips");
        
        // Call Firebase Function to analyze images with Gemini
        Map<String, Object> data = new HashMap<>();
        data.put("exerciseName", exerciseName);
        data.put("exerciseDescription", exerciseDescription);
        data.put("images", validImages); // Only send valid images
        data.put("numberOfSteps", numberOfSteps); // Send number of steps to AI
        data.put("exerciseType", exerciseType); // Send exercise type
        data.put("bodyPartsToCheck", bodyPartsToCheck); // Send body parts to check
        if (!commonErrors.isEmpty()) {
            data.put("commonErrors", commonErrors); // Send common errors if provided
        }
        data.put("uid", user.getUid());
        
        Log.d(TAG, "Preparing to call Firebase Function");
        Log.d(TAG, "Data keys: " + data.keySet().toString());

        Log.d(TAG, "Calling Firebase Function generateExerciseConfig");
        Log.d(TAG, "Exercise name: " + exerciseName);
        Log.d(TAG, "Images count: " + (imageBase64 != null ? imageBase64.length : 0));
        
        functions.getHttpsCallable("generateExerciseConfig")
                .call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            String errorMessage = "Unknown error";
                            
                            if (e instanceof FirebaseFunctionsException) {
                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                FirebaseFunctionsException.Code code = ffe.getCode();
                                errorMessage = ffe.getMessage();
                                
                                Log.e(TAG, "Function error - Code: " + code + ", Message: " + errorMessage);
                                Log.e(TAG, "Error details: " + ffe.getDetails());
                                
                                // Provide user-friendly error messages
                                if (code == FirebaseFunctionsException.Code.NOT_FOUND) {
                                    errorMessage = "Function chưa được deploy. Vui lòng liên hệ admin.";
                                } else if (code == FirebaseFunctionsException.Code.UNAUTHENTICATED) {
                                    errorMessage = "Lỗi xác thực. Vui lòng đăng nhập lại.";
                                } else if (code == FirebaseFunctionsException.Code.INVALID_ARGUMENT) {
                                    errorMessage = "Dữ liệu không hợp lệ: " + errorMessage;
                                } else if (code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED) {
                                    errorMessage = "Quá thời gian chờ. Vui lòng thử lại.";
                                } else {
                                    errorMessage = "Lỗi phân tích ảnh: " + errorMessage;
                                }
                            } else if (e != null) {
                                errorMessage = e.getMessage();
                                Log.e(TAG, "Exception: " + e.getClass().getName() + " - " + errorMessage);
                                e.printStackTrace();
                            }
                            
                            setLoading(false);
                            tvStatus.setVisibility(View.GONE);
                            Toast.makeText(CreateCustomExerciseActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            HttpsCallableResult result = task.getResult();
                            if (result == null || result.getData() == null) {
                                Log.e(TAG, "Result is null or data is null");
                                setLoading(false);
                                tvStatus.setVisibility(View.GONE);
                                Toast.makeText(CreateCustomExerciseActivity.this, 
                                        "Không nhận được phản hồi từ server", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Map<String, Object> resultData = (Map<String, Object>) result.getData();
                            Log.d(TAG, "Result data keys: " + (resultData != null ? resultData.keySet().toString() : "null"));
                            
                            if (resultData != null && resultData.containsKey("config")) {
                                Object configObj = resultData.get("config");
                                if (configObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> config = (Map<String, Object>) configObj;
                                    Log.d(TAG, "Config received successfully");
                                    saveExerciseToFirebase(exerciseName, exerciseDescription, config, user.getUid());
                                } else {
                                    Log.e(TAG, "Config is not a Map, type: " + (configObj != null ? configObj.getClass().getName() : "null"));
                                    setLoading(false);
                                    tvStatus.setVisibility(View.GONE);
                                    Toast.makeText(CreateCustomExerciseActivity.this, 
                                            "Định dạng config không đúng", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e(TAG, "Result data does not contain 'config' key");
                                setLoading(false);
                                tvStatus.setVisibility(View.GONE);
                                Toast.makeText(CreateCustomExerciseActivity.this, 
                                        "Không nhận được config từ AI. Response: " + resultData, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing result: " + e.getMessage(), e);
                            setLoading(false);
                            tvStatus.setVisibility(View.GONE);
                            Toast.makeText(CreateCustomExerciseActivity.this, 
                                    "Lỗi xử lý kết quả: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveExerciseToFirebase(String name, String description, Map<String, Object> config, String uid) {
        tvStatus.setText("Đang lưu bài tập...");

        String exerciseId = "custom_" + uid + "_" + System.currentTimeMillis();
        
        // Tạo ExerciseUser object
        ExerciseUser exerciseUser = new ExerciseUser();
        exerciseUser.setId(exerciseId);
        exerciseUser.setName(name);
        exerciseUser.setSlug(name.toLowerCase().replace(" ", "_"));
        exerciseUser.setLevel("beginner");
        exerciseUser.setPublic(true); // FIX: Set isPublic = true để có thể tập được như các bài tập mẫu
        exerciseUser.setUpdatedAt(System.currentTimeMillis() / 1000);
        exerciseUser.setUid(uid); // Set uid để Firestore rules có thể kiểm tra

        // Set MediaPipe config
        ExerciseUser.MediaPipe mediaPipe = new ExerciseUser.MediaPipe();
        mediaPipe.setAnalyzerType("CustomExerciseAnalyzer");
        mediaPipe.setVersion("1.0");
        mediaPipe.setConfig(config);
        exerciseUser.setMediapipe(mediaPipe);

        // Set default config
        ExerciseUser.DefaultConfig defaultConfig = new ExerciseUser.DefaultConfig();
        defaultConfig.setSets(3);
        defaultConfig.setReps(12);
        defaultConfig.setRestSec(30);
        defaultConfig.setDifficulty("beginner");
        exerciseUser.setDefaultConfig(defaultConfig);

        // Set media (thumbnail from first image)
        ExerciseUser.Media media = new ExerciseUser.Media();
        // Thumbnail sẽ được lưu sau nếu cần
        exerciseUser.setMedia(media);

        // Convert ExerciseUser to Map để đảm bảo field names đúng
        Map<String, Object> exerciseMap = new HashMap<>();
        exerciseMap.put("id", exerciseId);
        exerciseMap.put("name", name);
        exerciseMap.put("slug", exerciseUser.getSlug());
        exerciseMap.put("level", "beginner");
        exerciseMap.put("isPublic", true); // FIX: Set isPublic = true để có thể tập được như các bài tập mẫu
        exerciseMap.put("uid", uid); // Field uid để Firestore rules kiểm tra
        exerciseMap.put("updatedAt", System.currentTimeMillis() / 1000);
        
        // Convert MediaPipe to Map
        Map<String, Object> mediaPipeMap = new HashMap<>();
        mediaPipeMap.put("analyzerType", "CustomExerciseAnalyzer");
        mediaPipeMap.put("version", "1.0");
        mediaPipeMap.put("config", config);
        exerciseMap.put("mediapipe", mediaPipeMap);
        
        // Convert DefaultConfig to Map
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("sets", 3);
        defaultConfigMap.put("reps", 12);
        defaultConfigMap.put("restSec", 30);
        defaultConfigMap.put("difficulty", "beginner");
        exerciseMap.put("defaultConfig", defaultConfigMap);
        
        // Convert Media to Map
        Map<String, Object> mediaMap = new HashMap<>();
        exerciseMap.put("media", mediaMap);
        
        Log.d(TAG, "Saving ExerciseUser with uid: " + uid + ", isPublic: true");
        Log.d(TAG, "ExerciseUser map keys: " + exerciseMap.keySet().toString());
        
        // Save to Firestore - collection exerciseUser để tách biệt với exercises public
        db.collection("exerciseUser")
                .document(exerciseId)
                .set(exerciseMap)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Tạo bài tập thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Error saving exercise: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi lưu bài tập: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnCreateExercise.setEnabled(!loading);
        if (!loading) {
            tvStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, user can select image again
        } else {
            Toast.makeText(this, "Cần quyền truy cập ảnh để tạo bài tập", Toast.LENGTH_SHORT).show();
        }
    }
}

