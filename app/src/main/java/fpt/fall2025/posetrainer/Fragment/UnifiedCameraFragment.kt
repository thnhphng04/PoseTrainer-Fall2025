package fpt.fall2025.posetrainer.Fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fpt.fall2025.posetrainer.Analyzer.ExerciseAnalyzerInterface
import fpt.fall2025.posetrainer.Analyzer.ExerciseFeedback
import fpt.fall2025.posetrainer.Analyzer.JumpingJackAnalyzer
import fpt.fall2025.posetrainer.Analyzer.PushUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SquatAnalyzer
import fpt.fall2025.posetrainer.Domain.Exercise
import fpt.fall2025.posetrainer.MediaPipe.LandmarkConverter
import fpt.fall2025.posetrainer.MediaPipe.PoseLandmarkerHelper
import fpt.fall2025.posetrainer.R
import fpt.fall2025.posetrainer.Activity.ExerciseActivity
import fpt.fall2025.posetrainer.View.JumpingJackOverlayView
import fpt.fall2025.posetrainer.View.PushUpOverlayView
import fpt.fall2025.posetrainer.View.SquatOverlayView
import fpt.fall2025.posetrainer.databinding.FragmentUnifiedCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * UnifiedCameraFragment - Fragment chung cho tất cả bài tập
 * Dựa trên logic từ project nghiên cứu
 */
class UnifiedCameraFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "UnifiedCameraFragment"
    }

    private var _binding: FragmentUnifiedCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    // Exercise data
    private lateinit var exercise: Exercise
    private var sets: Int = 3
    private var reps: Int = 12
    private var exerciseType: String = ""

    // Exercise state management
    private var currentSet: Int = 1
    private var currentRep: Int = 0
    private var isExerciseActive: Boolean = false
    private var correctCount: Int = 0
    private var totalCorrectCount: Int = 0
    private var lastCorrectCount: Int = 0

    // Analyzer and overlay
    private var currentAnalyzer: ExerciseAnalyzerInterface? = null
    private var currentOverlayView: View? = null
    private var lastFeedback: ExerciseFeedback? = null

    // Specialized overlay views
    private var squatOverlayView: SquatOverlayView? = null
    private var pushUpOverlayView: PushUpOverlayView? = null
    private var jumpingJackOverlayView: JumpingJackOverlayView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get exercise data from arguments
        arguments?.let { args ->
            exercise = args.getSerializable("exercise") as Exercise
            sets = args.getInt("sets", 3)
            reps = args.getInt("reps", 12)
            exerciseType = exercise.name.lowercase()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnifiedCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Initialize analyzer based on exercise type
        initializeAnalyzer()

        // Initialize overlay view
        setOverlayViewForExercise()

        // Setup UI
        setupUI()
        
        // Add camera switch button
        binding.btnSwitchCamera?.setOnClickListener {
            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            setUpCamera()
        }
        
        // Setup exercise controls
        setupExerciseControls()

        // Wait for the views to be properly laid out
        binding.viewFinder.post {
            setUpCamera()
        }

        // Create the PoseLandmarkerHelper
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                minPoseDetectionConfidence = 0.5f,
                minPoseTrackingConfidence = 0.5f,
                minPosePresenceConfidence = 0.5f,
                currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                currentDelegate = PoseLandmarkerHelper.DELEGATE_CPU,
                runningMode = RunningMode.LIVE_STREAM,
                context = requireContext(),
                poseLandmarkerHelperListener = this
            )
        }
    }

    private fun initializeAnalyzer() {
        currentAnalyzer = when (exerciseType) {
            "squat", "bodyweight squat" -> SquatAnalyzer()
            "push-up", "pushup" -> PushUpAnalyzer()
            "jumping jack", "jumpingjack" -> JumpingJackAnalyzer()
            else -> SquatAnalyzer() // Default
        }
    }

    private fun setOverlayViewForExercise() {
        // Remove existing overlay view
        binding.overlayContainer.removeAllViews()

        currentOverlayView = when (exerciseType) {
            "squat", "bodyweight squat" -> {
                squatOverlayView = SquatOverlayView(requireContext(), null)
                squatOverlayView
            }
             "push-up", "pushup" -> {
                 pushUpOverlayView = PushUpOverlayView(requireContext(), null)
                 pushUpOverlayView
             }
            "jumping jack", "jumpingjack" -> {
                jumpingJackOverlayView = JumpingJackOverlayView(requireContext(), null)
                jumpingJackOverlayView
            }
            else -> {
                squatOverlayView = SquatOverlayView(requireContext(), null)
                squatOverlayView
            }
        }

        // Add overlay view to the layout
        currentOverlayView?.let { overlayView ->
            if (overlayView.parent == null) {
                binding.overlayContainer.addView(overlayView)
                Log.d(TAG, "Added overlay view to container: ${overlayView.javaClass.simpleName}")
            } else {
                Log.d(TAG, "Overlay view already has parent: ${overlayView.javaClass.simpleName}")
            }
        }

        Log.d(TAG, "Set overlay view for exercise: $exerciseType")
        Log.d(TAG, "Current overlay view: ${currentOverlayView?.javaClass?.simpleName}")
        Log.d(TAG, "Overlay container child count: ${binding.overlayContainer.childCount}")
        Log.d(TAG, "Overlay container size: ${binding.overlayContainer.width}x${binding.overlayContainer.height}")
    }
    
    private fun setupUI() {
        // Set exercise name
        binding.tvExerciseType.text = exercise.name
        
        // Set initial set info
        updateSetInfo()
        
        // Set initial reps info
        updateRepsInfo()
        
        // Set initial correct count
        updateCorrectCount()
    }
    
    private fun setupExerciseControls() {
        // Back button
        binding.btnBack.setOnClickListener {
            activity?.finish()
        }
        
        // Start/Stop button
        binding.btnStartStop.setOnClickListener {
            if (isExerciseActive) {
                stopExercise()
            } else {
                startExercise()
            }
        }
        
        // Skip button
        binding.btnSkip.setOnClickListener {
            showSkipOptions()
        }
    }
    
    private fun updateSetInfo() {
        binding.tvSetInfo.text = "Set $currentSet/$sets"
    }
    
    private fun updateRepsInfo() {
        binding.tvRepsInfo.text = "Reps: $currentRep/$reps"
    }
    
    private fun updateCorrectCount() {
        // Update correct count in the existing correct count display
        Log.d(TAG, "Correct: $totalCorrectCount")
    }
    
    private fun startExercise() {
        Log.d(TAG, "Starting exercise")
        isExerciseActive = true
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        
        // Reset UI counts to 0 when starting
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"
        
        // Update UI
        binding.btnStartStop.text = "Reset"
        updateRepsInfo()
        updateCorrectCount()
        
        Toast.makeText(requireContext(), "Exercise started! Perform $reps reps", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopExercise() {
        Log.d(TAG, "Stopping exercise - cancelling current set")
        isExerciseActive = false
        
        // Reset current set progress
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        
        // Reset UI counts to 0 when stopping
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"
        
        // Update UI
        binding.btnStartStop.text = "Start"
        updateRepsInfo()
        updateCorrectCount()
        
        Toast.makeText(requireContext(), "Set reset. Click Start to begin again", Toast.LENGTH_SHORT).show()
    }
    
    private fun completeSet() {
        Log.d(TAG, "Set $currentSet completed")
        isExerciseActive = false
        
        // Add to total correct count
        totalCorrectCount += correctCount
        
        // Reset for next set
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        
        // Reset UI counts to 0 when set completed
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"
        
        // Update UI
        binding.btnStartStop.text = "Start"
        updateCorrectCount()
        
        if (currentSet < sets) {
            // Move to next set
            currentSet++
            updateSetInfo()
            
            // Show continue message
            Toast.makeText(requireContext(), "Set ${currentSet - 1} completed! Ready for Set $currentSet", Toast.LENGTH_LONG).show()
        } else {
            // All sets completed
            completeExercise()
        }
    }
    
    private fun continueToNextSet() {
        if (currentSet < sets) {
            currentSet++
            currentRep = 0
            correctCount = 0
            
            updateSetInfo()
            updateRepsInfo()
            updateCorrectCount()
            
            Toast.makeText(requireContext(), "Ready for Set $currentSet", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun completeExercise() {
        Log.d(TAG, "Exercise completed")
        Toast.makeText(requireContext(), "Exercise completed! Total reps: $totalCorrectCount", Toast.LENGTH_LONG).show()
        
        // Notify parent activity that exercise is completed
        (activity as? ExerciseActivity)?.onExerciseCompleted()
    }
    
    private fun showSkipOptions() {
        val options = arrayOf("Skip Set", "Skip Exercise")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Skip Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> skipCurrentSet()
                    1 -> skipCurrentExercise()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun skipCurrentSet() {
        Log.d(TAG, "Skipping current set: $currentSet")
        
        // Stop current exercise if active
        if (isExerciseActive) {
            isExerciseActive = false
            binding.btnStartStop.text = "Start"
        }
        
        // Reset current set progress
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        
        // Reset UI counts
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"
        
        // Move to next set or complete exercise
        if (currentSet < sets) {
            currentSet++
            updateSetInfo()
            updateRepsInfo()
            Toast.makeText(requireContext(), "Set ${currentSet - 1} skipped. Ready for Set $currentSet", Toast.LENGTH_SHORT).show()
        } else {
            // All sets completed
            completeExercise()
        }
    }
    
    private fun skipCurrentExercise() {
        Log.d(TAG, "Skipping current exercise")
        
        // Stop current exercise if active
        if (isExerciseActive) {
            isExerciseActive = false
            binding.btnStartStop.text = "Start"
        }
        
        // Show confirmation
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Skip Exercise")
            .setMessage("Are you sure you want to skip this exercise?")
            .setPositiveButton("Yes, Skip") { _, _ ->
                // Skip exercise and go to rest screen
                skipExerciseAndGoToRest()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun skipExerciseAndGoToRest() {
        Log.d(TAG, "Skipping exercise and going to rest screen")
        
        // Notify parent activity that exercise is completed (skipped)
        // This will trigger the rest screen flow
        (activity as? ExerciseActivity)?.onExerciseCompleted()
    }
    
    // Method called when rep is detected
    private fun onRepDetected() {
        // Only count reps when exercise is active (Start button pressed)
        if (isExerciseActive) {
            currentRep++
            updateRepsInfo()
            
            // Show progress message
            if (currentRep < reps) {
                Toast.makeText(requireContext(), "Rep $currentRep/$reps", Toast.LENGTH_SHORT).show()
            }
            
            // Complete set only when reaching target reps
            if (currentRep >= reps) {
                completeSet()
            }
        }
    }
    
    // Method called when correct form is detected
    private fun onCorrectFormDetected() {
        // Only count correct form when exercise is active (Start button pressed)
        if (isExerciseActive) {
            correctCount++
            updateCorrectCount()
            
            // Show form feedback
            Toast.makeText(requireContext(), "Good form! ($correctCount)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundExecutor.execute {
            if (::poseLandmarkerHelper.isInitialized) {
                if (poseLandmarkerHelper.isClose()) {
                    poseLandmarkerHelper.setupPoseLandmarker()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::poseLandmarkerHelper.isInitialized) {
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()

        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    detectPose(image)
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_binding != null) {
                val poseResult = resultBundle.results.firstOrNull()
                val allLandmarks = poseResult?.landmarks()?.firstOrNull()

                if (allLandmarks != null) {
                    // Convert landmarks to analyzer format
                    val landmarks = LandmarkConverter.convertToAnalyzerFormat(poseResult)
                    
                     // Analyze with current analyzer
                     currentAnalyzer?.let { analyzer ->
                         lastFeedback = analyzer.analyze(landmarks)
                     }

                     // Update UI with correct/incorrect counts
                     lastFeedback?.let { feedback ->
                         // Only update UI counts when exercise is active (Start button pressed)
                         if (isExerciseActive) {
                             binding.tvCorrectCount.text = feedback.correctCount.toString()
                             binding.tvIncorrectCount.text = feedback.incorrectCount.toString()
                             
                             // Simple rep detection - only count when correct count increases
                             if (feedback.correctCount > lastCorrectCount) {
                                 onRepDetected()
                             }
                             
                             // Handle correct form detection - only count when correct count increases
                             if (feedback.correctCount > lastCorrectCount) {
                                 onCorrectFormDetected()
                             }
                             
                             // Update last correct count to prevent duplicate counting
                             lastCorrectCount = feedback.correctCount
                         } else {
                             // When not active, show 0 counts
                             binding.tvCorrectCount.text = "0"
                             binding.tvIncorrectCount.text = "0"
                         }
                     }

                     // Update overlay view with PoseLandmarkerResult
                     updateSpecializedOverlayView(poseResult, resultBundle.inputImageHeight, resultBundle.inputImageWidth, lastFeedback)
                }
            }
        }
    }

    private fun updateSpecializedOverlayView(
        poseResult: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        feedback: ExerciseFeedback?
    ) {
        Log.d(TAG, "Updating overlay view for exercise: $exerciseType")
        Log.d(TAG, "Image size: ${imageWidth}x$imageHeight")
        Log.d(TAG, "Pose landmarks count: ${poseResult.landmarks().firstOrNull()?.size ?: 0}")

        when (exerciseType) {
            "squat", "bodyweight squat" -> {
                Log.d(TAG, "Updating SquatOverlayView")
                squatOverlayView?.setResults(poseResult, imageHeight, imageWidth, com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM, feedback)
            }
            "push-up", "pushup" -> {
                Log.d(TAG, "Updating PushUpOverlayView")
                pushUpOverlayView?.setResults(poseResult, imageHeight, imageWidth, com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM, feedback)
            }
            "jumping jack", "jumpingjack" -> {
                Log.d(TAG, "Updating JumpingJackOverlayView")
                jumpingJackOverlayView?.setResults(poseResult, imageHeight, imageWidth, com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM, feedback)
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                Log.e(TAG, "GPU error, switching to CPU")
            }
        }
    }
}
