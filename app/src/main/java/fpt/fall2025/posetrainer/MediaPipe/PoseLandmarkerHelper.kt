package fpt.fall2025.posetrainer.MediaPipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * PoseLandmarkerHelper - Kotlin version của MediaPipe integration
 * Chuyển đổi từ Java sang Kotlin để tương thích với MediaPipe
 */
class PoseLandmarkerHelper(
    var minPoseDetectionConfidence: Float = DEFAULT_POSE_DETECTION_CONFIDENCE,
    var minPoseTrackingConfidence: Float = DEFAULT_POSE_TRACKING_CONFIDENCE,
    var minPosePresenceConfidence: Float = DEFAULT_POSE_PRESENCE_CONFIDENCE,
    var currentModel: Int = MODEL_POSE_LANDMARKER_FULL,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    // this listener is only used when running in RunningMode.LIVE_STREAM
    val poseLandmarkerHelperListener: LandmarkerListener? = null
) {
    
    private var poseLandmarker: PoseLandmarker? = null
    
    // Constants
    companion object {
        const val TAG = "PoseLandmarkerHelper"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_POSES = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        const val MODEL_POSE_LANDMARKER_FULL = 0
        const val MODEL_POSE_LANDMARKER_LITE = 1
        const val MODEL_POSE_LANDMARKER_HEAVY = 2
    }
    
    init {
        setupPoseLandmarker()
    }
    
    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
    
    fun isClose(): Boolean {
        return poseLandmarker == null
    }
    
    fun setupPoseLandmarker() {
        try {
            // Set general pose landmarker options
            val baseOptionBuilder = BaseOptions.builder()
            
            // Use the specified hardware for running the model
            when (currentDelegate) {
                DELEGATE_CPU -> {
                    baseOptionBuilder.setDelegate(Delegate.CPU)
                }
                DELEGATE_GPU -> {
                    baseOptionBuilder.setDelegate(Delegate.GPU)
                }
            }
            
            // Set model
            val modelName = when (currentModel) {
                MODEL_POSE_LANDMARKER_FULL -> "pose_landmarker_full.task"
                MODEL_POSE_LANDMARKER_LITE -> "pose_landmarker_lite.task"
                MODEL_POSE_LANDMARKER_HEAVY -> "pose_landmarker_heavy.task"
                else -> "pose_landmarker_full.task"
            }
            
            baseOptionBuilder.setModelAssetPath(modelName)
            
            // Check if runningMode is consistent with listener
            if (runningMode == RunningMode.LIVE_STREAM && poseLandmarkerHelperListener == null) {
                throw IllegalStateException(
                    "listener must be set when runningMode is LIVE_STREAM."
                )
            }
            
            val baseOptions = baseOptionBuilder.build()
            
            // Create pose landmarker options
            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                .setMinTrackingConfidence(minPoseTrackingConfidence)
                .setMinPosePresenceConfidence(minPosePresenceConfidence)
                .setRunningMode(runningMode)
            
            // Set listeners for LIVE_STREAM mode
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }
            
            val options = optionsBuilder.build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            
        } catch (e: IllegalStateException) {
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for details"
            )
            Log.e(TAG, "MediaPipe failed to load the task with error: ${e.message}")
        } catch (e: RuntimeException) {
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for details", GPU_ERROR
            )
            Log.e(TAG, "Image classifier failed to load model with error: ${e.message}")
        }
    }
    
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream while not using RunningMode.LIVE_STREAM"
            )
        }
        
        val frameTime = SystemClock.uptimeMillis()
        
        // Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()
        
        val matrix = Matrix()
        // Rotate the frame received from the camera
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        
        // Flip image if user uses front camera
        if (isFrontCamera) {
            matrix.postScale(
                -1f, 1f,
                imageProxy.width.toFloat(),
                imageProxy.height.toFloat()
            )
        }
        
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )
        
        // Convert to MPImage and run inference
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        detectAsync(mpImage, frameTime)
    }
    
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        poseLandmarker?.detectAsync(mpImage, frameTime)
    }
    
    fun detectImage(image: Bitmap): ResultBundle? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage while not using RunningMode.IMAGE"
            )
        }
        
        val startTime = SystemClock.uptimeMillis()
        
        // Convert to MPImage
        val mpImage = BitmapImageBuilder(image).build()
        
        // Run detection
        val landmarkResult = poseLandmarker?.detect(mpImage)
        if (landmarkResult != null) {
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                listOf(landmarkResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }
        
        poseLandmarkerHelperListener?.onError("Pose Landmarker failed to detect.")
        return null
    }
    
    private fun returnLivestreamResult(result: PoseLandmarkerResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()
        
        poseLandmarkerHelperListener?.onResults(ResultBundle(
            listOf(result),
            inferenceTime,
            input.height,
            input.width
        ))
    }
    
    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }
    
    // Properties are already accessible directly
    
    // Result Bundle data class
    data class ResultBundle(
        val results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int
    )
    
    // Listener interface
    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
}
