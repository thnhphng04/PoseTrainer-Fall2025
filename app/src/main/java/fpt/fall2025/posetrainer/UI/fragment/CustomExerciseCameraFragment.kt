package fpt.fall2025.posetrainer.UI.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import fpt.fall2025.posetrainer.Core.analyzer.core.CustomExerciseAnalyzer
import fpt.fall2025.posetrainer.Core.analyzer.core.ExerciseAnalyzerInterface
import fpt.fall2025.posetrainer.Core.analyzer.core.ExerciseFeedback
import fpt.fall2025.posetrainer.Core.mediapipe.LandmarkConverter
import fpt.fall2025.posetrainer.Core.mediapipe.PoseLandmarkerHelper
import fpt.fall2025.posetrainer.Domain.Exercise
import fpt.fall2025.posetrainer.Domain.Session
import fpt.fall2025.posetrainer.UI.activity.ExerciseActivity
import fpt.fall2025.posetrainer.UI.view.UnifiedOverlayView
import fpt.fall2025.posetrainer.databinding.FragmentCustomExerciseCameraBinding
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * CustomExerciseCameraFragment - Fragment chỉ xử lý custom exercises do user tạo bằng AI
 * Tách biệt với UnifiedCameraFragment để dễ quản lý và tùy chỉnh
 */
class CustomExerciseCameraFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "CustomExerciseCameraFragment"
    }

    private var _binding: FragmentCustomExerciseCameraBinding? = null
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
    private var session: Session? = null
    private var exerciseIndex: Int = 0

    // Exercise state management
    private var currentSet: Int = 1
    private var currentRep: Int = 0
    private var isExerciseActive: Boolean = false
    private var correctCount: Int = 0
    private var totalCorrectCount: Int = 0
    private var lastCorrectCount: Int = 0
    private var isUIReset: Boolean = false
    private var lastRepShown: Int = -1 // Track last rep we showed toast for - FIX TOAST ISSUE

    // Error tracking
    private val currentSetErrorCounts = mutableMapOf<String, Int>()
    private var lastFeedbackList: List<String> = emptyList()

    // Analyzer and overlay
    private var currentAnalyzer: ExerciseAnalyzerInterface? = null
    private var unifiedOverlayView: UnifiedOverlayView? = null
    private var lastFeedback: ExerciseFeedback? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady: Boolean = false
    private var isSpeakingFeedback: Boolean = false
    private var lastFeedbackSignature: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get exercise data from arguments
        arguments?.let { args ->
            exercise = args.getSerializable("exercise") as Exercise
            sets = args.getInt("sets", 3)
            reps = args.getInt("reps", 12)

            // Nhận currentSetNumber để tiếp tục từ set đúng
            currentSet = args.getInt("currentSetNumber", 1)
            Log.d(TAG, "=== CUSTOM EXERCISE CAMERA FRAGMENT ===")
            Log.d(TAG, "Received currentSetNumber: $currentSet")
            Log.d(TAG, "Exercise ID: ${exercise.id}")
            Log.d(TAG, "Exercise Name: ${exercise.name}")

            // Nhận session để biết trạng thái các set
            session = args.getSerializable("session") as? Session

            // Nhận exerciseIndex để tìm đúng PerExercise trong session
            exerciseIndex = args.getInt("exerciseIndex", 0)

            // Nhận isResume flag
            val isResume = args.getBoolean("isResume", false)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomExerciseCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Initialize analyzer - only CustomExerciseAnalyzer for custom exercises
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
                currentModel = PoseLandmarkerHelper.Companion.MODEL_POSE_LANDMARKER_FULL,
                currentDelegate = PoseLandmarkerHelper.Companion.DELEGATE_CPU,
                runningMode = RunningMode.LIVE_STREAM,
                context = requireContext(),
                poseLandmarkerHelperListener = this
            )
        }

        initTextToSpeech()
    }

    private fun initializeAnalyzer() {
        // This fragment only handles custom exercises, so always use CustomExerciseAnalyzer
        val mediaPipe = exercise.mediapipe
        Log.d(TAG, "========== INITIALIZING CUSTOM EXERCISE ANALYZER ==========")
        Log.d(TAG, "Exercise name: ${exercise.name}")
        Log.d(TAG, "Exercise ID: ${exercise.id}")
        Log.d(TAG, "MediaPipe object: ${mediaPipe != null}")
        Log.d(TAG, "AnalyzerType: ${mediaPipe?.analyzerType}")
        Log.d(TAG, "Config exists: ${mediaPipe?.config != null}")

        val config = mediaPipe?.config
        if (config != null) {
            try {
                Log.d(TAG, "Config type: ${config.javaClass.name}")
                Log.d(TAG, "Config toString: ${config.toString().take(200)}")

                // Convert config to Map<String, Any> if needed
                val configMap = when (config) {
                    is Map<*, *> -> {
                        Log.d(TAG, "Config is Map<*, *>, converting...")
                        // Convert Map<*, *> to Map<String, Any>
                        val result = mutableMapOf<String, Any>()
                        config.forEach { (key, value) ->
                            val keyStr = key?.toString() ?: ""
                            val valueAny = value ?: ""
                            result[keyStr] = valueAny
                            Log.d(TAG, "  - Added key: $keyStr, value type: ${valueAny.javaClass.simpleName}")
                        }
                        Log.d(TAG, "Converted config map size: ${result.size}")
                        result
                    }
                    else -> {
                        Log.e(TAG, "Config is not a Map, type: ${config.javaClass.name}")
                        null
                    }
                }

                if (configMap != null) {
                    Log.d(TAG, "Config keys: ${configMap.keys}")
                    Log.d(TAG, "Config has 'states': ${configMap.containsKey("states")}")
                    Log.d(TAG, "Config has 'thresholds': ${configMap.containsKey("thresholds")}")
                    Log.d(TAG, "Config has 'stateSequence': ${configMap.containsKey("stateSequence")}")

                    currentAnalyzer = CustomExerciseAnalyzer(configMap)
                    Log.d(TAG, "✅ CustomExerciseAnalyzer initialized successfully!")
                } else {
                    Log.e(TAG, "❌ Failed to convert config to Map<String, Any>")
                    Toast.makeText(context, "Lỗi: Không thể load config bài tập", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing CustomExerciseAnalyzer: ${e.message}", e)
                e.printStackTrace()
                Toast.makeText(context, "Lỗi khởi tạo analyzer: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e(TAG, "❌ Config is null for CustomExerciseAnalyzer")
            Toast.makeText(context, "Lỗi: Config bài tập không tồn tại", Toast.LENGTH_LONG).show()
        }
    }

    private fun setOverlayViewForExercise() {
        // Remove existing overlay view
        binding.overlayContainer.removeAllViews()

        // Tạo unified overlay view cho custom exercises
        unifiedOverlayView = UnifiedOverlayView(requireContext(), null)

        // Add overlay view to the layout
        unifiedOverlayView?.let { overlayView ->
            if (overlayView.parent == null) {
                binding.overlayContainer.addView(overlayView)
                Log.d(TAG, "Added unified overlay view to container")
            }
        }

        Log.d(TAG, "Set unified overlay view for custom exercise: ${exercise.name}")
    }

    private fun setupUI() {
        // Set exercise name
        binding.tvExerciseType.text = exercise.name

        // Find the first incomplete set if resuming
        findFirstIncompleteSet()

        // Set initial set info
        updateSetInfo()

        // Set initial reps info
        updateRepsInfo()

        // Set initial correct count
        updateCorrectCount()
    }

    /**
     * Find the first incomplete set when resuming exercise
     */
    private fun findFirstIncompleteSet() {
        Log.d(TAG, "=== FIND FIRST INCOMPLETE SET ===")
        Log.d(TAG, "Current set from ExerciseActivity: $currentSet")

        // Check if current set is already completed or skipped
        val currentSetStatus = getSetStatus(currentSet)
        Log.d(TAG, "Current set $currentSet status: $currentSetStatus")

        if (currentSetStatus == "completed" || currentSetStatus == "skipped") {
            Log.d(TAG, "Current set is $currentSetStatus, finding next incomplete set")

            // Find first incomplete set
            for (setNumber in 1..sets) {
                val setStatus = getSetStatus(setNumber)
                Log.d(TAG, "Set $setNumber status: $setStatus")

                if (setStatus == "incomplete") {
                    currentSet = setNumber
                    Log.d(TAG, "Found first incomplete set: $currentSet")
                    break
                }
            }
        } else {
            Log.d(TAG, "Current set $currentSet is incomplete, no need to change")
        }

        Log.d(TAG, "Final current set: $currentSet")
        Log.d(TAG, "=== END FIND FIRST INCOMPLETE SET ===")
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
        // Get set status from session
        val setStatus = getSetStatus(currentSet)
        val statusText = when (setStatus) {
            "completed" -> "✓"
            "skipped" -> "⏭"
            "incomplete" -> ""
            else -> ""
        }

        Log.d(TAG, "updateSetInfo: currentSet=$currentSet, setStatus=$setStatus, statusText=$statusText")
        binding.tvSetInfo.text = "Set $currentSet/$sets $statusText"
    }

    /**
     * Get status of a specific set from session
     */
    private fun getSetStatus(setNumber: Int): String {
        return session?.let { session ->
            val perExerciseList = session.perExercise
            // Get currentExerciseNo from ExerciseActivity
            val currentExerciseNo = (activity as? ExerciseActivity)?.let { activity ->
                // Get exerciseNo from intent or use exerciseIndex + 1 as fallback
                activity.intent.getIntExtra("exerciseNo", exerciseIndex + 1)
            } ?: (exerciseIndex + 1)

            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            val setsList = currentPerExercise?.getSets() ?: ArrayList()
            val targetSet = setsList.find { it.getSetNo() == setNumber }
            val state = targetSet?.getState() ?: "incomplete"

            Log.d(TAG, "getSetStatus($setNumber): exerciseIndex=$exerciseIndex, currentExerciseNo=$currentExerciseNo, exerciseNo=${currentPerExercise?.getExerciseNo()}, setState=$state")
            state
        } ?: "incomplete"
    }

    private fun updateRepsInfo() {
        binding.tvRepsInfo.text = "Reps: $currentRep/$reps"
    }

    private fun updateCorrectCount() {
        // Update UI with current correct count for this set
        binding.tvCorrectCount.text = correctCount.toString()
    }

    private fun startExercise() {
        // Check if current set is already completed or skipped
        val currentSetStatus = getSetStatus(currentSet)
        if (currentSetStatus == "completed" || currentSetStatus == "skipped") {
            // Find next incomplete set
            var nextSet = currentSet + 1
            while (nextSet <= sets) {
                val setStatus = getSetStatus(nextSet)
                if (setStatus == "incomplete") {
                    currentSet = nextSet
                    break
                }
                nextSet++
            }

            // If no incomplete set found, all sets are completed
            if (nextSet > sets) {
                Toast.makeText(requireContext(), "All sets are completed!", Toast.LENGTH_SHORT).show()
                return
            }

            // Update UI after moving to new set
            updateSetInfo()
            updateRepsInfo()
        }

        // Start exercise (either current set or moved set)
        startExerciseInternal()
    }

    private fun startExerciseInternal() {
        // Reset analyzer first to clear any previous feedback
        currentAnalyzer?.reset()

        // Reset all counters first
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        lastRepShown = -1 // Reset last rep shown - FIX TOAST ISSUE

        // Reset error tracking for new set
        resetErrorTracking()

        // Reset UI counts to 0 when starting
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"

        // Update UI
        binding.btnStartStop.text = "Reset"
        updateRepsInfo()

        // Set flags
        isUIReset = true
        isExerciseActive = true

        // Force UI update after a short delay to ensure it's not overridden by onResults
        binding.root.post {
            binding.tvCorrectCount.text = "0"
            binding.tvIncorrectCount.text = "0"
        }

        Toast.makeText(requireContext(), "Set $currentSet started! Perform $reps reps", Toast.LENGTH_SHORT).show()
    }

    private fun stopExercise() {
        isExerciseActive = false

        // Reset current set progress
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        lastRepShown = -1 // Reset last rep shown - FIX TOAST ISSUE

        // Reset error tracking when stopping
        resetErrorTracking()

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
        isExerciseActive = false

        // Cập nhật session với kết quả set vừa hoàn thành (bao gồm errorCounts)
        (activity as? ExerciseActivity)?.updateSessionAfterSet(
            setNumber = currentSet,
            correctReps = correctCount,
            targetReps = reps,
            skipped = false,
            errorCounts = currentSetErrorCounts.toMap() // Convert to immutable map
        )

        // Force reload session to get latest data
        reloadSessionFromActivity()

        // Add to total correct count
        totalCorrectCount += correctCount

        // FIX: Reset analyzer để đảm bảo correctCount/incorrectCount được reset cho set mới
        currentAnalyzer?.reset()

        // Reset for next set
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        lastRepShown = -1 // Reset last rep shown - FIX TOAST ISSUE

        // Reset error tracking for next set
        resetErrorTracking()

        // Reset UI counts to 0 when set completed
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"

        // Update UI
        binding.btnStartStop.text = "Start"
        updateRepsInfo() // Make sure reps info is updated

        // Move to next set (simple logic)
        if (currentSet < sets) {
            // Move to next set
            currentSet++
            updateSetInfo()

            // Ensure UI counts are 0 for new set
            binding.tvCorrectCount.text = "0"
            binding.tvIncorrectCount.text = "0"

            // FIX: Set isUIReset flag để đảm bảo UI được reset đúng cách
            isUIReset = true

            // Show continue message
            Toast.makeText(requireContext(), "Set ${currentSet - 1} completed! Ready for Set $currentSet", Toast.LENGTH_LONG).show()
        } else {
            // All sets completed
            completeExercise()
        }
    }

    /**
     * Reload session from ExerciseActivity to get latest data
     */
    private fun reloadSessionFromActivity() {
        (activity as? ExerciseActivity)?.let { exerciseActivity ->
            // Get updated session from activity
            val updatedSession = exerciseActivity.getCurrentSession()
            if (updatedSession != null) {
                session = updatedSession
            }
        }
    }

    private fun continueToNextSet() {
        // Find next incomplete set
        var nextSet = currentSet + 1
        while (nextSet <= sets) {
            val setStatus = getSetStatus(nextSet)
            if (setStatus == "incomplete") {
                break
            }
            nextSet++
        }

        if (nextSet <= sets) {
            currentSet = nextSet
            currentRep = 0
            correctCount = 0
            lastCorrectCount = 0
            lastRepShown = -1 // Reset last rep shown - FIX TOAST ISSUE

            // FIX: Reset analyzer để đảm bảo correctCount/incorrectCount được reset cho set mới
            currentAnalyzer?.reset()

            // Reset UI counts to 0 for new set
            binding.tvCorrectCount.text = "0"
            binding.tvIncorrectCount.text = "0"

            // FIX: Set isUIReset flag để đảm bảo UI được reset đúng cách
            isUIReset = true

            updateSetInfo()
            updateRepsInfo()

            // Ensure UI counts remain 0 for new set
            binding.tvCorrectCount.text = "0"
            binding.tvIncorrectCount.text = "0"

            Toast.makeText(requireContext(), "Ready for Set $currentSet", Toast.LENGTH_SHORT).show()
        } else {
            // All sets completed
            completeExercise()
        }
    }

    private fun completeExercise() {
        Toast.makeText(requireContext(), "Exercise completed! Total reps: $totalCorrectCount", Toast.LENGTH_LONG).show()

        // Notify parent activity that exercise is completed
        (activity as? ExerciseActivity)?.onExerciseCompleted()
    }

    private fun showSkipOptions() {
        val options = arrayOf("Skip Set", "Skip Exercise")

        AlertDialog.Builder(requireContext())
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
        // Cập nhật session với set bị skip (bao gồm errorCounts nếu có)
        (activity as? ExerciseActivity)?.updateSessionAfterSet(
            setNumber = currentSet,
            correctReps = correctCount,
            targetReps = reps,
            skipped = true,
            errorCounts = currentSetErrorCounts.toMap() // Convert to immutable map
        )

        // Stop current exercise if active
        if (isExerciseActive) {
            isExerciseActive = false
            binding.btnStartStop.text = "Start"
        }

        // Reset current set progress
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        lastRepShown = -1 // Reset last rep shown - FIX TOAST ISSUE

        // Reset error tracking for next set
        resetErrorTracking()

        // Reset UI counts
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"

        // Move to next set (simple logic)
        if (currentSet < sets) {
            // Move to next set
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
        // Stop current exercise if active
        if (isExerciseActive) {
            isExerciseActive = false
            binding.btnStartStop.text = "Start"
        }

        // Show confirmation
        AlertDialog.Builder(requireContext())
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
        // Gọi skipExercise() để cập nhật session
        (activity as? ExerciseActivity)?.skipExercise()

        // Notify parent activity that exercise is completed (skipped)
        // This will trigger the rest screen flow
        (activity as? ExerciseActivity)?.onExerciseCompleted()
    }

    // Method called when rep is detected - Logic giống UnifiedCameraFragment
    private fun onRepDetected() {
        // Only count reps when exercise is active (Start button pressed)
        if (isExerciseActive) {
            currentRep++
            updateRepsInfo()

            // Show progress message - Logic giống UnifiedCameraFragment
            if (currentRep < reps) {
                Toast.makeText(requireContext(), "Rep $currentRep/$reps", Toast.LENGTH_SHORT).show()
            }

            // Complete set only when reaching target reps
            if (currentRep >= reps) {
                completeSet()
            }
        }
    }

    // Method called when correct form is detected - Logic giống UnifiedCameraFragment
    private fun onCorrectFormDetected() {
        // Only count correct form when exercise is active (Start button pressed)
        if (isExerciseActive) {
            // Don't increment correctCount here - it's already updated from feedback.correctCount in onResults()
            // Just update UI and show feedback
            updateCorrectCount()

            // Show form feedback - Logic giống UnifiedCameraFragment
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
        shutdownTextToSpeech()
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
            // Log.e(TAG, "Use case binding failed", exc)
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

                     // Update UI with correct/incorrect counts - Logic giống UnifiedCameraFragment
                     lastFeedback?.let { feedback ->
                         // Only update UI counts when exercise is active (Start button pressed)
                         if (isExerciseActive) {
                             // Phát âm thanh trước khi track errors (để có thể phát hiện lỗi mới)
                             maybeSpeakFeedback(feedback)

                             // Track errors from feedback (chỉ track khi exercise active)
                             // Phải gọi sau maybeSpeakFeedback để lastFeedbackList vẫn chứa feedback của frame trước
                             trackErrors(feedback)

                            // Don't update UI immediately after reset - wait for meaningful feedback
                            // Logic giống UnifiedCameraFragment
                            if (!isUIReset || (feedback.correctCount > 0 || feedback.incorrectCount > 0)) {
                                // Update our internal counters based on analyzer feedback
                                correctCount = feedback.correctCount

                                // FIX: Always update incorrectCount from feedback to ensure UI reflects analyzer state
                                // incorrectCount is tracked by analyzer, not by fragment

                                // Update UI with our internal counters
                                binding.tvCorrectCount.text = correctCount.toString()
                                binding.tvIncorrectCount.text = feedback.incorrectCount.toString()

                                // Clear the reset flag after first meaningful update
                                if (isUIReset) {
                                    isUIReset = false
                                }
                            }

                            // Simple rep detection - only count when correct count increases
                            // Logic giống UnifiedCameraFragment
                            if (feedback.correctCount > lastCorrectCount) {
                                onRepDetected()
                            }

                            // FIX: Also detect when incorrect count increases (rep completed with errors)
                            val lastIncorrectCount = lastFeedback?.incorrectCount ?: 0
                            if (feedback.incorrectCount > lastIncorrectCount) {
                                Log.d(TAG, "❌ Incorrect rep detected! incorrectCount: $lastIncorrectCount -> ${feedback.incorrectCount}")
                                // Don't call onRepDetected() here - it's already counted in analyzer
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
        poseResult: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        feedback: ExerciseFeedback?
    ) {
        // Cập nhật unified overlay view cho custom exercises
        unifiedOverlayView?.setResults(
            poseResult,
            imageHeight,
            imageWidth,
            RunningMode.LIVE_STREAM,
            feedback
        )
    }

    private fun initTextToSpeech() {
        // Thử khởi tạo với Google TTS engine trước
        val googleTtsIntent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
        val resolveInfo = requireContext().packageManager.resolveActivity(googleTtsIntent, 0)

        // Nếu có Google TTS, thử dùng nó
        val engine = if (resolveInfo != null) {
            "com.google.android.tts"
        } else {
            null // Dùng engine mặc định
        }

        textToSpeech = TextToSpeech(requireContext(), { status ->
            if (status == TextToSpeech.SUCCESS) {
                val vietnamese = Locale("vi", "VN")
                val result = textToSpeech?.setLanguage(vietnamese)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Vietnamese not supported, falling back to English")
                    textToSpeech?.setLanguage(Locale.US)
                }
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeakingFeedback = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeakingFeedback = false
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeakingFeedback = false
                    }
                })
                isTtsReady = true
                Log.d(TAG, "TextToSpeech initialized successfully")
            } else {
                Log.e(TAG, "TextToSpeech init failed: $status. TTS feature will be disabled.")
                isTtsReady = false
                // Nếu không có TTS engine, tính năng phát âm thanh sẽ bị tắt
                // Feedback vẫn hiển thị trên màn hình
            }
        }, engine)
    }

    private fun shutdownTextToSpeech() {
        if (isTtsReady && textToSpeech != null) {
            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down TTS: ${e.message}")
            }
        }
        textToSpeech = null
        isTtsReady = false
        isSpeakingFeedback = false
        lastFeedbackSignature = null
    }

    private fun maybeSpeakFeedback(feedback: ExerciseFeedback) {
        if (!isTtsReady || textToSpeech == null || isSpeakingFeedback) {
            return
        }

        // Chỉ đọc khi exercise đang active (giống trackErrors)
        if (!isExerciseActive) {
            return
        }

        // Không đọc khi camera bị lệch (cameraWarning = true)
        if (feedback.isCameraWarning) {
            return
        }

        val currentMessages = feedback.feedbackList ?: emptyList()
        if (currentMessages.isEmpty()) {
            return
        }

        // Chỉ đọc lỗi mới xuất hiện và không phải lỗi về camera
        // Filter ra các errors có chứa "Camera" (case-insensitive)
        val newErrors = currentMessages.filter { errorMessage ->
            val isNotBlank = errorMessage.isNotBlank()
            val isNew = errorMessage !in lastFeedbackList
            val isNotCameraError = !errorMessage.contains("Camera", ignoreCase = true)
            isNotBlank && isNew && isNotCameraError
        }

        if (newErrors.isEmpty()) {
            return
        }

        // Nối tất cả lỗi mới lại bằng dấu phẩy
        val textToSpeak = newErrors.joinToString(", ").trim()
        if (textToSpeak.isEmpty()) {
            return
        }

        val utteranceId = "feedback-${System.currentTimeMillis()}"
        val speakResult =
            textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (speakResult == TextToSpeech.ERROR) {
            isSpeakingFeedback = false
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.Companion.GPU_ERROR) {
                // Log.e(TAG, "GPU error, switching to CPU")
            }
        }
    }

    /**
     * Track errors from ExerciseFeedback - chỉ đếm khi lỗi mới xuất hiện
     * Không đếm errors khi cameraWarning = true (camera lệch, feedback không chính xác)
     * Và filter ra các errors liên quan đến camera
     */
    private fun trackErrors(feedback: ExerciseFeedback) {
        if (!isExerciseActive) return // Chỉ track khi đang tập

        // Không track errors khi camera bị lệch (cameraWarning = true)
        if (feedback.isCameraWarning) {
            // Reset lastFeedbackList để không track errors từ frame camera lệch
            lastFeedbackList = emptyList()
            return
        }

        val currentErrors = feedback.feedbackList ?: emptyList()

        // Chỉ đếm lỗi mới xuất hiện và không phải lỗi về camera
        // Filter ra các errors có chứa "Camera" (case-insensitive)
        val newErrors = currentErrors.filter { errorMessage ->
            val isNotBlank = errorMessage.isNotBlank()
            val isNew = errorMessage !in lastFeedbackList
            val isNotCameraError = !errorMessage.contains("Camera", ignoreCase = true)
            isNotBlank && isNew && isNotCameraError
        }

        newErrors.forEach { errorMessage ->
            currentSetErrorCounts[errorMessage] =
                currentSetErrorCounts.getOrDefault(errorMessage, 0) + 1
            Log.d(TAG, "Error tracked: $errorMessage (count: ${currentSetErrorCounts[errorMessage]})")
        }

        // Cập nhật cho frame tiếp theo (bao gồm cả camera errors để tránh đếm lại)
        lastFeedbackList = currentErrors
    }

    /**
     * Reset error tracking khi bắt đầu set mới hoặc stop exercise
     */
    private fun resetErrorTracking() {
        currentSetErrorCounts.clear()
        lastFeedbackList = emptyList()
        Log.d(TAG, "Error tracking reset")
    }
}