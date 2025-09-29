package fpt.fall2025.posetrainer.Analyzer;

import java.util.List;
import java.util.Map;

/**
 * Interface chung cho tất cả các analyzer bài tập
 * Cho phép CameraFragment sử dụng chung mà không cần biết loại bài tập cụ thể
 */
public interface ExerciseAnalyzerInterface {
    
    /**
     * Phân tích pose landmarks và trả về feedback
     * @param landmarks Danh sách toàn bộ landmarks từ MediaPipe (33 điểm)
     * @return Feedback object chứa thông tin phân tích
     */
    ExerciseFeedback analyze(List<Map<String, Float>> landmarks);
    
    /**
     * Lấy tên bài tập
     * @return Tên bài tập (squat, pushup, jumping_jack)
     */
    String getExerciseType();
    
    /**
     * Lấy danh sách landmarks cần thiết cho bài tập này
     * @return Mảng index của landmarks cần thiết (từ 33 landmarks MediaPipe)
     */
    int[] getRequiredLandmarks();
    
    /**
     * Lấy cấu hình threshold cho level hiện tại
     * @param level "beginner" hoặc "pro"
     * @return Map chứa các threshold values
     */
    Map<String, Object> getThresholds(String level);
    
    /**
     * Cập nhật threshold cho analyzer
     * @param thresholds Map chứa các threshold values mới
     */
    void updateThresholds(Map<String, Object> thresholds);
    
    /**
     * Reset analyzer về trạng thái ban đầu
     * Xóa tất cả counters và state
     */
    void reset();
}
