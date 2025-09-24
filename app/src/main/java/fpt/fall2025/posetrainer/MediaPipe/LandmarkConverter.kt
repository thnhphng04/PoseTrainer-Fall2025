package fpt.fall2025.posetrainer.MediaPipe

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.*

/**
 * LandmarkConverter - Convert MediaPipe landmarks to analyzer format
 */
object LandmarkConverter {
    
    fun convertToAnalyzerFormat(poseResult: PoseLandmarkerResult): List<Map<String, Float>> {
        val landmarks = mutableListOf<Map<String, Float>>()
        
        poseResult.landmarks()?.firstOrNull()?.let { poseLandmarks ->
            poseLandmarks.forEach { landmark ->
                val landmarkMap = mapOf<String, Float>(
                    "x" to landmark.x(),
                    "y" to landmark.y(),
                    "z" to landmark.z(),
                    "visibility" to (landmark.visibility().orElse(0.0f))
                )
                landmarks.add(landmarkMap)
            }
        }
        
        return landmarks
    }
}
