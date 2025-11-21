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
import fpt.fall2025.posetrainer.Analyzer.ExerciseAnalyzerInterface
import fpt.fall2025.posetrainer.Analyzer.ExerciseFeedback
import fpt.fall2025.posetrainer.Analyzer.JumpingJackAnalyzer
import fpt.fall2025.posetrainer.Analyzer.PushUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SquatAnalyzer
import fpt.fall2025.posetrainer.Analyzer.BurpeeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.HighKneeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.MountainClimberAnalyzer
import fpt.fall2025.posetrainer.Analyzer.LegRaiseAnalyzer
import fpt.fall2025.posetrainer.Analyzer.RussianTwistAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SitUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SitUpTwistAnalyzer
import fpt.fall2025.posetrainer.Analyzer.KneePushUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.LungeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SideLungeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.JumpingSquatAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SquatJackAnalyzer
import fpt.fall2025.posetrainer.Analyzer.PileSquatAnalyzer
import fpt.fall2025.posetrainer.Analyzer.OneLegPushUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.JumpingPushUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.FloorTricepDipAnalyzer
import fpt.fall2025.posetrainer.Analyzer.PlankLegUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.StraightArmPlankToPikeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.VUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SupermanAnalyzer
import fpt.fall2025.posetrainer.Analyzer.ScissorAnalyzer
import fpt.fall2025.posetrainer.Analyzer.LegInOutAnalyzer
import fpt.fall2025.posetrainer.Analyzer.InOutAnalyzer
import fpt.fall2025.posetrainer.Analyzer.GluteKickBackAnalyzer
import fpt.fall2025.posetrainer.Analyzer.DonkeyKickAnalyzer
import fpt.fall2025.posetrainer.Analyzer.OneLegBridgeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.ButtBridgeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.ButtKickAnalyzer
import fpt.fall2025.posetrainer.Analyzer.HipHingeAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SideLyingLegLiftAnalyzer
import fpt.fall2025.posetrainer.Analyzer.PilatesLegPullAnalyzer
import fpt.fall2025.posetrainer.Analyzer.InchWormAnalyzer
import fpt.fall2025.posetrainer.Analyzer.InchWormPushUpAnalyzer
import fpt.fall2025.posetrainer.Analyzer.ArmRaiseAnalyzer
import fpt.fall2025.posetrainer.Analyzer.SideArmRaiseAnalyzer
import fpt.fall2025.posetrainer.Analyzer.ArmSwingAnalyzer
import fpt.fall2025.posetrainer.Analyzer.ClapOverHeadAnalyzer
import fpt.fall2025.posetrainer.Domain.Exercise
import fpt.fall2025.posetrainer.MediaPipe.LandmarkConverter
import fpt.fall2025.posetrainer.MediaPipe.PoseLandmarkerHelper
import fpt.fall2025.posetrainer.Activity.ExerciseActivity
import fpt.fall2025.posetrainer.View.UnifiedOverlayView
import fpt.fall2025.posetrainer.databinding.FragmentUnifiedCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
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
    private var session: fpt.fall2025.posetrainer.Domain.Session? = null
    private var exerciseIndex: Int = 0

    // Exercise state management
    private var currentSet: Int = 1
    private var currentRep: Int = 0
    private var isExerciseActive: Boolean = false
    private var correctCount: Int = 0
    private var totalCorrectCount: Int = 0
    private var lastCorrectCount: Int = 0
    private var isUIReset: Boolean = false

    // Analyzer and overlay
    private var currentAnalyzer: ExerciseAnalyzerInterface? = null
    private var unifiedOverlayView: UnifiedOverlayView? = null
    private var lastFeedback: ExerciseFeedback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get exercise data from arguments
        arguments?.let { args ->
            exercise = args.getSerializable("exercise") as Exercise
            sets = args.getInt("sets", 3)
            reps = args.getInt("reps", 12)
            
            // Null check cho exercise.id
            exerciseType = exercise.id?.lowercase() ?: "ex_squat"
            
            if (exercise.id == null) {
                Log.e(TAG, "Exercise ID is null! Using default squat")
            }
            
            // Nhận currentSetNumber để tiếp tục từ set đúng
            currentSet = args.getInt("currentSetNumber", 1)
            Log.d(TAG, "=== UNIFIED CAMERA FRAGMENT ===")
            Log.d(TAG, "Received currentSetNumber: $currentSet")
            Log.d(TAG, "Exercise ID: ${exercise.id}")
            Log.d(TAG, "Exercise Type: $exerciseType")
            
            // Nhận session để biết trạng thái các set
            session = args.getSerializable("session") as? fpt.fall2025.posetrainer.Domain.Session
            
            // Nhận exerciseIndex để tìm đúng PerExercise trong session
            exerciseIndex = args.getInt("exerciseIndex", 0)
            // Log.d(TAG, "ExerciseIndex: $exerciseIndex")
            
            // Nhận isResume flag
            val isResume = args.getBoolean("isResume", false)
            // Log.d(TAG, "Is Resume: $isResume")
            
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
            "ex_squat" -> SquatAnalyzer()
            "ex_pushup" -> PushUpAnalyzer()
            "ex_jumping_jack" -> JumpingJackAnalyzer()
            "ex_burpee" -> BurpeeAnalyzer()
            "ex_high_knee" -> HighKneeAnalyzer()
            "ex_mountain_climber" -> MountainClimberAnalyzer()
            "ex_leg_raise" -> LegRaiseAnalyzer()
            "ex_russian_twist" -> RussianTwistAnalyzer()
            "ex_sit_up" -> SitUpAnalyzer()
            "ex_sit_up_twist" -> SitUpTwistAnalyzer()
            "ex_knee_pushup" -> KneePushUpAnalyzer()
            "ex_lunge" -> LungeAnalyzer()
            "ex_side_lunge" -> SideLungeAnalyzer()
            "ex_jumping_squat" -> JumpingSquatAnalyzer()
            "ex_squat_jack" -> SquatJackAnalyzer()
            "ex_pile_squat" -> PileSquatAnalyzer()
            "ex_one_leg_pushup" -> OneLegPushUpAnalyzer()
            "ex_jumping_pushup" -> JumpingPushUpAnalyzer()
            "ex_floor_tricep_dip" -> FloorTricepDipAnalyzer()
            "ex_plank_leg_up" -> PlankLegUpAnalyzer()
            "ex_straight_arm_plank_to_pike" -> StraightArmPlankToPikeAnalyzer()
            "ex_v_up" -> VUpAnalyzer()
            "ex_superman" -> SupermanAnalyzer()
            "ex_scissor" -> ScissorAnalyzer()
            "ex_leg_in_out" -> LegInOutAnalyzer()
            "ex_in_out" -> InOutAnalyzer()
            "ex_glute_kickback" -> GluteKickBackAnalyzer()
            "ex_donkey_kick" -> DonkeyKickAnalyzer()
            "ex_one_leg_bridge" -> OneLegBridgeAnalyzer()
            "ex_butt_bridge" -> ButtBridgeAnalyzer()
            "ex_butt_kick" -> ButtKickAnalyzer()
            "ex_hip_hinge" -> HipHingeAnalyzer()
            "ex_side_lying_leg_lift" -> SideLyingLegLiftAnalyzer()
            "ex_pilates_leg_pull" -> PilatesLegPullAnalyzer()
            "ex_inch_worm" -> InchWormAnalyzer()
            "ex_inch_worm_pushup" -> InchWormPushUpAnalyzer()
            "ex_arm_raise" -> ArmRaiseAnalyzer()
            "ex_side_arm_raise" -> SideArmRaiseAnalyzer()
            "ex_arm_swing" -> ArmSwingAnalyzer()
            "ex_clap_overhead" -> ClapOverHeadAnalyzer()
            else -> SquatAnalyzer() // Default
        }
    }

    private fun setOverlayViewForExercise() {
        // Remove existing overlay view
        binding.overlayContainer.removeAllViews()

        // Tạo unified overlay view chung cho tất cả bài tập
        unifiedOverlayView = UnifiedOverlayView(requireContext(), null)

        // Add overlay view to the layout
        unifiedOverlayView?.let { overlayView ->
            if (overlayView.parent == null) {
                binding.overlayContainer.addView(overlayView)
                Log.d(TAG, "Added unified overlay view to container")
            }
        }

        Log.d(TAG, "Set unified overlay view for exercise: $exerciseType")
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
        // Update correct count in the existing correct count display
        // Log.d(TAG, "updateCorrectCount: correctCount=$correctCount, totalCorrectCount=$totalCorrectCount")
        
        // Update UI with current correct count for this set
        binding.tvCorrectCount.text = correctCount.toString()
        // Log.d(TAG, "Updated tvCorrectCount to: ${binding.tvCorrectCount.text}")
    }
    
    private fun startExercise() {
        // Log.d(TAG, "Starting exercise for Set $currentSet")
        
        // Check if current set is already completed or skipped
        val currentSetStatus = getSetStatus(currentSet)
        if (currentSetStatus == "completed" || currentSetStatus == "skipped") {
            // Log.d(TAG, "Set $currentSet is $currentSetStatus, finding next incomplete set")
            
            // Find next incomplete set
            var nextSet = currentSet + 1
            while (nextSet <= sets) {
                val setStatus = getSetStatus(nextSet)
                if (setStatus == "incomplete") {
                    currentSet = nextSet
                    // Log.d(TAG, "Moved to Set $currentSet")
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
        // Log.d(TAG, "Starting exercise for Set $currentSet (internal)")
        
        // Reset analyzer first to clear any previous feedback
        currentAnalyzer?.reset()
        // Log.d(TAG, "Analyzer reset completed")
        
        // Reset all counters first
        currentRep = 0
        correctCount = 0
        lastCorrectCount = 0
        
        // Reset UI counts to 0 when starting
        binding.tvCorrectCount.text = "0"
        binding.tvIncorrectCount.text = "0"
        
        // Update UI
        binding.btnStartStop.text = "Reset"
        updateRepsInfo()
        
        // Set flags
        isUIReset = true
        isExerciseActive = true
        
        // Log.d(TAG, "Set $currentSet started - UI counts reset to 0, isExerciseActive=$isExerciseActive, isUIReset=$isUIReset")
        
        // Force UI update after a short delay to ensure it's not overridden by onResults
        binding.root.post {
            binding.tvCorrectCount.text = "0"
            binding.tvIncorrectCount.text = "0"
            // Log.d(TAG, "Forced UI update: tvCorrectCount=${binding.tvCorrectCount.text}, tvIncorrectCount=${binding.tvIncorrectCount.text}")
        }
        
        Toast.makeText(requireContext(), "Set $currentSet started! Perform $reps reps", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopExercise() {
        // Log.d(TAG, "Stopping exercise - cancelling current set")
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
        // Log.d(TAG, "Set $currentSet completed")
        isExerciseActive = false
        
        // Cập nhật session với kết quả set vừa hoàn thành
        (activity as? ExerciseActivity)?.updateSessionAfterSet(
            setNumber = currentSet,
            correctReps = correctCount,
            targetReps = reps,
            skipped = false
        )
        
        // Force reload session to get latest data
        reloadSessionFromActivity()
        
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
        updateRepsInfo() // Make sure reps info is updated
        
        // Move to next set (simple logic)
        if (currentSet < sets) {
            // Move to next set
            currentSet++
            updateSetInfo()
            
            // Ensure UI counts are 0 for new set
            binding.tvCorrectCount.text = "0"
            binding.tvIncorrectCount.text = "0"
            
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
        // Log.d(TAG, "Reloading session from ExerciseActivity")
        (activity as? ExerciseActivity)?.let { exerciseActivity ->
            // Get updated session from activity
            val updatedSession = exerciseActivity.getCurrentSession()
            if (updatedSession != null) {
                session = updatedSession
                // Log.d(TAG, "Session reloaded from activity")
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
            
            // Reset UI counts to 0 for new set
            binding.tvCorrectCount.text = "0"
            binding.tvIncorrectCount.text = "0"
            
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
        // Log.d(TAG, "Exercise completed")
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
        // Log.d(TAG, "Skipping current set: $currentSet")
        
        // Cập nhật session với set bị skip
        (activity as? ExerciseActivity)?.updateSessionAfterSet(
            setNumber = currentSet,
            correctReps = correctCount,
            targetReps = reps,
            skipped = true
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
        // Log.d(TAG, "Skipping current exercise")
        
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
        // Log.d(TAG, "Skipping exercise and going to rest screen")
        
        // Gọi skipExercise() để cập nhật session
        (activity as? ExerciseActivity)?.skipExercise()
        
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
            
            // Log.d(TAG, "Rep detected: $currentRep/$reps for Set $currentSet")
            
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
            // Log.d(TAG, "onCorrectFormDetected: correctCount increased to $correctCount")
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

                     // Update UI with correct/incorrect counts
                     lastFeedback?.let { feedback ->
                         // Only update UI counts when exercise is active (Start button pressed)
                         if (isExerciseActive) {
                             // Don't update UI immediately after reset - wait for meaningful feedback
                             if (!isUIReset || (feedback.correctCount > 0 || feedback.incorrectCount > 0)) {
                                 // Update our internal counters based on analyzer feedback
                                 correctCount = feedback.correctCount
                                 
                                 // Update UI with our internal counters
                                 binding.tvCorrectCount.text = correctCount.toString()
                                 binding.tvIncorrectCount.text = feedback.incorrectCount.toString()
                                 
                                 // Log.d(TAG, "onResults: isExerciseActive=true, correctCount=$correctCount, feedback.correctCount=${feedback.correctCount}, isUIReset=$isUIReset")
                                 
                                 // Clear the reset flag after first meaningful update
                                 if (isUIReset) {
                                     isUIReset = false
                                 }
                             }
                             
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
                             // Log.d(TAG, "onResults: isExerciseActive=false, UI counts set to 0")
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
        // Cập nhật unified overlay view chung cho tất cả bài tập
        unifiedOverlayView?.setResults(
            poseResult, 
            imageHeight, 
            imageWidth, 
            com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM, 
            feedback
        )
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                // Log.e(TAG, "GPU error, switching to CPU")
            }
        }
    }
}
