package fpt.fall2025.posetrainer.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import fpt.fall2025.posetrainer.Domain.Exercise
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate
import fpt.fall2025.posetrainer.Domain.Session
import fpt.fall2025.posetrainer.Fragment.UnifiedCameraFragment
import fpt.fall2025.posetrainer.Manager.WorkoutSessionManager
import fpt.fall2025.posetrainer.R
import fpt.fall2025.posetrainer.Service.FirebaseService

/**
 * ExerciseActivity - Activity để host camera fragment cho từng bài tập
 */
class ExerciseActivity : AppCompatActivity() {
    
    private lateinit var exercise: Exercise
    private var currentExercise: Exercise? = null
    private var sets: Int = 3
    private var reps: Int = 12
    private var workoutTemplate: WorkoutTemplate? = null
    private lateinit var sessionManager: WorkoutSessionManager
    private var currentExerciseNo: Int = 1 // exerciseNo từ intent
    private var totalExercises: Int = 1
    // nextExercise no longer needed - we load all exercises dynamically
    private var isRestMode: Boolean = false
    private var allExercises: ArrayList<Exercise> = ArrayList()
    private var currentSession: Session? = null
    private var currentSetNumber: Int = 1 // Set hiện tại cần tiếp tục
    private var isResume: Boolean = false // Flag để phân biệt resume vs new workout
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
    
    /**
     * Setup back button behavior - giữ nguyên state của exercise
     */
    private fun setupBackButton() {
        // Không cần làm gì đặc biệt, để mặc định finish() activity
        // Exercise state sẽ vẫn là "doing" và được lưu trong session
    }
    
    override fun onBackPressed() {
        // Đơn giản finish activity, không thay đổi exercise state
        // Session đã được lưu real-time nên không cần lưu thêm
        // Set result code để WorkoutActivity biết user đã back
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)
        
        // Full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Xử lý nút Back - không thay đổi state, chỉ finish activity
        setupBackButton()
        
        // Get data from intent
        val sessionId = intent.getStringExtra("sessionId")
        val exerciseNo = intent.getIntExtra("exerciseNo", 1)
        
        if (sessionId == null) {
            Log.e("ExerciseActivity", "SessionId is null from intent")
            finish()
            return
        }
        
        // Set current exercise number
        currentExerciseNo = exerciseNo
        
        // Load session data from Firebase
        loadSessionAndExerciseData(sessionId, exerciseNo)
    }

    private fun loadSessionAndExerciseData(sessionId: String, exerciseNo: Int) {
        Log.d("ExerciseActivity", "Loading session data for ID: $sessionId, exerciseNo: $exerciseNo")
        
        FirebaseService.getInstance().loadSessionById(sessionId, object : FirebaseService.OnSessionLoadedListener {
            override fun onSessionLoaded(session: Session?) {
                if (session != null) {
                    currentSession = session
                    Log.d("ExerciseActivity", "Session loaded successfully")
                    
                    // Find current exercise from session data
                    findCurrentExerciseFromSession(session, exerciseNo)
                } else {
                    Log.e("ExerciseActivity", "Failed to load session")
                    finish()
                }
            }

            override fun onError(error: String?) {
                Log.e("ExerciseActivity", "Error loading session: $error")
                finish()
            }
        })
    }

    private fun findCurrentExerciseFromSession(session: Session, exerciseNo: Int) {
        Log.d("ExerciseActivity", "Finding exercise from session data")
        
        if (session.perExercise == null || session.perExercise!!.isEmpty()) {
            Log.e("ExerciseActivity", "No exercises found in session")
            finish()
            return
        }
        
        // Find the PerExercise with matching exerciseNo
        var targetPerExercise: Session.PerExercise? = null
        for (perExercise in session.perExercise!!) {
            if (perExercise.exerciseNo == exerciseNo) {
                targetPerExercise = perExercise
                break
            }
        }
        
        if (targetPerExercise == null) {
            Log.e("ExerciseActivity", "No PerExercise found with exerciseNo: $exerciseNo")
            finish()
            return
        }
        
        Log.d("ExerciseActivity", "Found PerExercise: ${targetPerExercise.exerciseId}")
        
        // Create a mock Exercise from PerExercise data
        val exercise = Exercise().apply {
            id = targetPerExercise.exerciseId
            name = when (targetPerExercise.exerciseId) {
                "ex_jumping_jack" -> "Jumping Jack"
                "ex_pushup" -> "Push-Up"
                "ex_squat" -> "Bodyweight Squat"
                else -> "Exercise ${targetPerExercise.exerciseNo}"
            }
            // Set default config from PerExercise sets data
            val sets = targetPerExercise.sets?.size ?: 3
            val reps = targetPerExercise.sets?.firstOrNull()?.targetReps ?: 12
            defaultConfig = Exercise.DefaultConfig(sets, reps, 90, targetPerExercise.difficultyUsed ?: "beginner")
        }
        
        Log.d("ExerciseActivity", "Created exercise: ${exercise.name}")
        allExercises = arrayListOf(exercise)
        currentExercise = exercise
        
        // Initialize exercise
        initializeExercise(exercise)
    }
    
    
    private fun initializeExercise(exercise: Exercise) {
        // Set the current exercise
        currentExercise = exercise
        
        // Get sets/reps from session data
        val sessionSets = getSetsFromSession()
        val sessionReps = getRepsFromSession()
        
        Log.d("ExerciseActivity", "Exercise initialized: ${exercise.name}, Sets: $sessionSets, Reps: $sessionReps")
        
        // Load camera fragment directly
        val fragment = UnifiedCameraFragment()
        val args = Bundle()
        args.putSerializable("exercise", exercise)
        args.putSerializable("session", currentSession)
        args.putInt("sets", sessionSets)
        args.putInt("reps", sessionReps)
        fragment.arguments = args
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }


    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera()
            } else {
                finish() // Close activity if permission denied
            }
        }
    }
    
    private fun initializeCamera() {
        // Always show exercise fragment directly
        showExerciseFragment()
    }
    
    private fun showExerciseFragment() {
        android.util.Log.d("ExerciseActivity", "=== SHOW EXERCISE FRAGMENT ===")
        android.util.Log.d("ExerciseActivity", "Is Resume: $isResume")
        
        // Nếu là resume, reload session từ Firebase để đảm bảo có data mới nhất
        if (isResume) {
            android.util.Log.d("ExerciseActivity", "Resume mode: Reloading session from Firebase")
            reloadSessionFromFirebase()
            return // Exit early, fragment will be shown after session reload
        }
        
        // Log session state BEFORE any modifications
        currentSession?.let { session ->
            val perExerciseList = session.perExercise
            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            currentPerExercise?.let { perExercise ->
                android.util.Log.d("ExerciseActivity", "BEFORE: Exercise state: ${perExercise.getState()}")
                val setsList = perExercise.getSets() ?: ArrayList()
                setsList.forEach { setData ->
                    android.util.Log.d("ExerciseActivity", "BEFORE: Set ${setData.getSetNo()}: state=${setData.getState()}, correctReps=${setData.getCorrectReps()}")
                }
                
                // Chỉ update state nếu exercise chưa bắt đầu
                if (perExercise.getState() == "not_started") {
                    android.util.Log.d("ExerciseActivity", "Exercise not started, updating state to 'doing'")
                    updateExerciseState("doing")
                } else {
                    android.util.Log.d("ExerciseActivity", "Exercise already started: ${perExercise.getState()}, no need to update")
                }
            }
        }
        
        // Chỉ tạo session manager mới khi KHÔNG phải resume
        if (!isResume) {
            android.util.Log.d("ExerciseActivity", "New workout: Creating WorkoutSessionManager")
        // Create new session manager - we need to load exercises first
        // For now, create with empty exercises list
        val exercises = ArrayList<Exercise>()
        exercises.add(exercise) // Add current exercise
        sessionManager = WorkoutSessionManager(workoutTemplate, exercises)
        sessionManager.startWorkout()
        } else {
            android.util.Log.d("ExerciseActivity", "Resume workout: NOT creating WorkoutSessionManager")
            // Khi resume, không tạo session manager mới để tránh ghi đè session data
        }
        
        // Log session state AFTER creating WorkoutSessionManager
        currentSession?.let { session ->
            val perExerciseList = session.perExercise
            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            currentPerExercise?.let { perExercise ->
                android.util.Log.d("ExerciseActivity", "AFTER: Exercise state: ${perExercise.getState()}")
                val setsList = perExercise.getSets() ?: ArrayList()
                setsList.forEach { setData ->
                    android.util.Log.d("ExerciseActivity", "AFTER: Set ${setData.getSetNo()}: state=${setData.getState()}, correctReps=${setData.getCorrectReps()}")
                }
            }
        }
        
        // Show the exercise fragment (reuse existing logic)
        showExerciseFragmentInternal()
    }
    
    
    /**
     * Internal method to show exercise fragment (extracted from showExerciseFragment)
     */
    private fun showExerciseFragmentInternal() {
        // Create and add UnifiedCameraFragment
        val cameraFragment = UnifiedCameraFragment()
        
        // Get sets/reps from session data
        val sessionSets = getSetsFromSession()
        val sessionReps = getRepsFromSession()
        
        // Pass data to fragment
        val args = Bundle().apply {
            putSerializable("exercise", exercise)
            putInt("sets", sessionSets)
            putInt("reps", sessionReps)
            // Chỉ truyền sessionManager khi không phải resume
            if (!isResume && ::sessionManager.isInitialized) {
            putSerializable("sessionManager", sessionManager)
            }
            putInt("currentSetNumber", currentSetNumber) // Truyền set hiện tại cần tiếp tục
            putSerializable("session", currentSession) // Truyền session để fragment biết trạng thái các set
            putInt("exerciseIndex", currentExerciseNo - 1) // Truyền exerciseIndex để tìm đúng PerExercise trong session
            putBoolean("isResume", isResume) // Truyền flag resume
        }
        cameraFragment.arguments = args
        
        // Add fragment to container
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, cameraFragment)
        transaction.commit()
    }
    
    
    private fun startNextExercise() {
        // Get next exercise before incrementing index
        val nextExercise = getNextExercise()
        if (nextExercise != null) {
            // Increment exercise index when actually starting next exercise
            
            // Update current exercise to next exercise
            exercise = nextExercise
            
            // Switch to exercise mode
            isRestMode = false
            
            // Show exercise fragment
            showExerciseFragment()
        } else {
            // No next exercise, finish activity
            finish()
        }
    }
    
    fun onExerciseCompleted() {
        // Mark current exercise as completed
        markExerciseAsCompleted()
        
        // Check if there are more exercises
        if (currentExerciseNo + 1 <= totalExercises) {
            // Show rest screen before next exercise
            showRestScreenForNextExercise()
        } else {
            // No more exercises, return to WorkoutActivity
            val resultIntent = Intent().apply {
                putExtra("completedIndex", currentExerciseNo - 1) // Return the current completed index
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
    
    private fun showRestScreenForNextExercise() {
        // No rest screen needed, finish activity
        finish()
    }
    
    private fun getNextExercise(): Exercise? {
        // Get next exercise from loaded exercises list
        val nextIndex = currentExerciseNo + 1
        
        return if (nextIndex < allExercises.size) {
            allExercises[nextIndex]
        } else {
            null
        }
    }
    
    
    private fun markExerciseAsCompleted() {
        // Cập nhật state của exercise thành "completed"
        updateExerciseState("completed")
        android.util.Log.d("ExerciseActivity", "Exercise $currentExerciseNo completed")
    }
    
    /**
     * Cập nhật session sau mỗi set tập
     */
    private fun updateExerciseState(newState: String) {
        android.util.Log.d("ExerciseActivity", "=== UPDATE EXERCISE STATE ===")
        android.util.Log.d("ExerciseActivity", "Updating exercise state to: $newState")
        android.util.Log.d("ExerciseActivity", "Exercise No: $currentExerciseNo")
        
        currentSession?.let { session ->
            val perExerciseList = session.perExercise
            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            currentPerExercise?.let { perExercise ->
                val oldState = perExercise.getState()
                android.util.Log.d("ExerciseActivity", "Old state: $oldState -> New state: $newState")
                
                perExercise.setState(newState)
                
                // Save session khi exercise state thực sự thay đổi
                if (oldState != newState) {
                    saveSessionToFirebase()
                    android.util.Log.d("ExerciseActivity", "Exercise state updated and saved to Firebase")
                } else {
                    android.util.Log.d("ExerciseActivity", "Exercise state unchanged, NOT saved to Firebase")
                }
            }
        }
        android.util.Log.d("ExerciseActivity", "=== END UPDATE EXERCISE STATE ===")
    }
    
    /**
     * Skip toàn bộ exercise - chuyển exercise thành completed và tất cả set chưa làm thành skipped
     */
    fun skipExercise() {
        currentSession?.let { session ->
            val perExerciseList = session.perExercise
            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            
            currentPerExercise?.let { perExercise ->
                // Chuyển exercise state thành completed
                perExercise.setState("completed")
                
                // Chuyển tất cả set chưa làm (incomplete) thành skipped
                val setsList = perExercise.getSets() ?: ArrayList()
                setsList.forEach { setData ->
                    if (setData.getState() == "incomplete") {
                        setData.setState("skipped")
                        setData.setCorrectReps(0) // Đặt correctReps = 0 cho set bị skip
                    }
                }
                
                // Luôn save session khi user thực sự skip exercise (dù là resume hay new workout)
                saveSessionToFirebase()
                android.util.Log.d("ExerciseActivity", "Skip exercise: Session saved to Firebase")
            }
        }
    }
    
    fun updateSessionAfterSet(setNumber: Int, correctReps: Int, targetReps: Int, skipped: Boolean = false) {
        android.util.Log.d("ExerciseActivity", "=== UPDATE SESSION AFTER SET ===")
        android.util.Log.d("ExerciseActivity", "Set: $setNumber, CorrectReps: $correctReps, TargetReps: $targetReps, Skipped: $skipped")
        
        currentSession?.let { session ->
            // Tìm PerExercise tương ứng với exercise hiện tại dựa trên exerciseNo
            val perExerciseList = session.perExercise
            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            
            currentPerExercise?.let { perExercise ->
                // Tìm SetData tương ứng với setNumber
                val setsList = perExercise.getSets() ?: ArrayList()
                val targetSet = setsList.find { it.getSetNo() == setNumber }
                
                targetSet?.let { setData ->
                    val oldState = setData.getState()
                    val newState = if (skipped) "skipped" else "completed"
                    
                    android.util.Log.d("ExerciseActivity", "Set $setNumber: $oldState -> $newState, correctReps: ${setData.getCorrectReps()} -> $correctReps")
                    
                    // Cập nhật thông tin của set
                    setData.setCorrectReps(correctReps)
                    setData.setTargetReps(targetReps)
                    // Nếu skip thì state = "skipped", nếu không skip thì state = "completed"
                    setData.setState(newState)
                    
                    // Luôn save session khi user thực sự tập (dù là resume hay new workout)
                    saveSessionToFirebase()
                    android.util.Log.d("ExerciseActivity", "Set $setNumber updated and saved to Firebase")
                    
                    // Update exercise state if needed
                    updateExerciseStateIfNeeded(perExercise)
                }
            }
        }
        android.util.Log.d("ExerciseActivity", "=== END UPDATE SESSION AFTER SET ===")
    }
    
    /**
     * Get current session for fragment access
     */
    fun getCurrentSession(): fpt.fall2025.posetrainer.Domain.Session? {
        return currentSession
    }
    
    /**
     * Lưu session vào Firebase
     */
    private fun saveSessionToFirebase() {
        android.util.Log.d("ExerciseActivity", "=== SAVE SESSION TO FIREBASE ===")
        currentSession?.let { session ->
            android.util.Log.d("ExerciseActivity", "Saving session: ${session.getId()}")
            fpt.fall2025.posetrainer.Service.FirebaseService.getInstance().saveSession(session, object : fpt.fall2025.posetrainer.Service.FirebaseService.OnSessionSavedListener {
                override fun onSessionSaved(success: Boolean) {
                    if (success) {
                        android.util.Log.d("ExerciseActivity", "Session saved successfully to Firebase")
                    } else {
                        android.util.Log.e("ExerciseActivity", "Failed to save session to Firebase")
                    }
                }
            })
        } ?: run {
            android.util.Log.e("ExerciseActivity", "Cannot save session: currentSession is null")
        }
    }
    
    
    /**
     * Reload session from Firebase to ensure we have the latest data
     */
    private fun reloadSessionFromFirebase() {
        android.util.Log.d("ExerciseActivity", "=== RELOAD SESSION FROM FIREBASE ===")
        
        currentSession?.let { session ->
            val sessionId = session.getId()
            android.util.Log.d("ExerciseActivity", "Reloading session: $sessionId")
            
            // Load session from Firebase to get the latest data
            fpt.fall2025.posetrainer.Service.FirebaseService.getInstance().loadSessionById(sessionId, object : fpt.fall2025.posetrainer.Service.FirebaseService.OnSessionLoadedListener {
                override fun onSessionLoaded(freshSession: Session?) {
                    if (freshSession != null) {
                        android.util.Log.d("ExerciseActivity", "Fresh session loaded from Firebase")
                        currentSession = freshSession
                        
                        // Now determine current set from the fresh session
                        determineCurrentSetFromSession()
                        
                        // Show exercise fragment with fresh session data
                        showExerciseFragmentWithFreshData()
                    } else {
                        android.util.Log.e("ExerciseActivity", "Failed to load fresh session from Firebase")
                        // Fallback to existing session
                        determineCurrentSetFromSession()
                        showExerciseFragmentInternal()
                    }
                }
                
                override fun onError(error: String) {
                    android.util.Log.e("ExerciseActivity", "Error loading fresh session: $error")
                    // Fallback to existing session
                    determineCurrentSetFromSession()
                    showExerciseFragmentInternal()
                }
            })
        } ?: run {
            android.util.Log.e("ExerciseActivity", "No session to reload")
            currentSetNumber = 1
            showExerciseFragmentInternal()
        }
        
        android.util.Log.d("ExerciseActivity", "=== END RELOAD SESSION ===")
    }
    
    /**
     * Show exercise fragment with fresh session data (for resume mode)
     */
    private fun showExerciseFragmentWithFreshData() {
        android.util.Log.d("ExerciseActivity", "=== SHOW EXERCISE FRAGMENT WITH FRESH DATA ===")
        
        // Log fresh session state
        currentSession?.let { session ->
            val perExerciseList = session.perExercise
            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            currentPerExercise?.let { perExercise ->
                android.util.Log.d("ExerciseActivity", "FRESH DATA: Exercise state: ${perExercise.getState()}")
                val setsList = perExercise.getSets() ?: ArrayList()
                setsList.forEach { setData ->
                    android.util.Log.d("ExerciseActivity", "FRESH DATA: Set ${setData.getSetNo()}: state=${setData.getState()}, correctReps=${setData.getCorrectReps()}")
                }
                
                // Update exercise state to "doing" if it's "not_started"
                if (perExercise.getState() == "not_started") {
                    android.util.Log.d("ExerciseActivity", "Fresh data: Exercise not started, updating state to 'doing'")
                    updateExerciseState("doing")
                } else {
                    android.util.Log.d("ExerciseActivity", "Fresh data: Exercise state is ${perExercise.getState()}, no need to update")
                }
            }
        }
        
        // Chỉ tạo session manager mới khi KHÔNG phải resume
        if (!isResume) {
            android.util.Log.d("ExerciseActivity", "Fresh data: Creating WorkoutSessionManager")
            // Create new session manager - we need to load exercises first
            // For now, create with empty exercises list
            val exercises = ArrayList<Exercise>()
            exercises.add(exercise) // Add current exercise
            sessionManager = WorkoutSessionManager(workoutTemplate, exercises)
            sessionManager.startWorkout()
        } else {
            android.util.Log.d("ExerciseActivity", "Fresh data: Resume mode, NOT creating WorkoutSessionManager")
            // Khi resume, không tạo session manager mới để tránh ghi đè session data
        }
        
        // Show the exercise fragment (reuse existing logic)
        showExerciseFragmentInternal()
    }
    
    /**
     * Xác định set hiện tại cần tiếp tục dựa trên session từ Firebase
     */
    private fun determineCurrentSetFromSession() {
        android.util.Log.d("ExerciseActivity", "=== DETERMINE CURRENT SET FROM SESSION ===")
        android.util.Log.d("ExerciseActivity", "Session ID: ${currentSession?.getId()}")
        android.util.Log.d("ExerciseActivity", "Exercise No: $currentExerciseNo")
        
        currentSession?.let { session ->
            val perExerciseList = session.perExercise
            android.util.Log.d("ExerciseActivity", "Total PerExercise items: ${perExerciseList?.size}")
            
            val currentPerExercise = perExerciseList?.find { it.getExerciseNo() == currentExerciseNo }
            android.util.Log.d("ExerciseActivity", "Current PerExercise found: ${currentPerExercise != null}")
            
            currentPerExercise?.let { perExercise ->
                android.util.Log.d("ExerciseActivity", "Exercise state from Firebase: ${perExercise.getState()}")
                
                // Nếu exercise state là "doing" hoặc "not_started", tìm set cần tiếp tục
                if (perExercise.getState() == "doing" || perExercise.getState() == "not_started") {
                    val setsList = perExercise.getSets() ?: ArrayList()
                    android.util.Log.d("ExerciseActivity", "Total sets from Firebase: ${setsList.size}")
                    
                    // Log tất cả set states để debug
                    setsList.forEach { setData ->
                        android.util.Log.d("ExerciseActivity", "Set ${setData.getSetNo()}: state=${setData.getState()}, correctReps=${setData.getCorrectReps()}")
                    }
                    
                    // Tìm set đầu tiên có state là "incomplete" (bỏ qua "skipped" và "completed")
                    val incompleteSet = setsList.find { it.getState() == "incomplete" }
                    
                    incompleteSet?.let { setData ->
                        // Tiếp tục từ set chưa hoàn thành
                        currentSetNumber = setData.getSetNo()
                        android.util.Log.d("ExerciseActivity", "Resuming from set: $currentSetNumber (state: ${setData.getState()})")
                    } ?: run {
                        // Nếu không có set incomplete, tất cả set đều completed hoặc skipped
                        // Bắt đầu từ set tiếp theo
                        val lastSet = setsList.maxByOrNull { it.getSetNo() }
                        currentSetNumber = (lastSet?.getSetNo() ?: 0) + 1
                        android.util.Log.d("ExerciseActivity", "All sets completed/skipped, starting next set: $currentSetNumber")
                    }
                } else {
                    // Exercise đã completed, bắt đầu từ set 1
                    currentSetNumber = 1
                    android.util.Log.d("ExerciseActivity", "Exercise completed, starting from set 1")
                }
            }
        } ?: run {
            // Không có session, bắt đầu từ set 1
            currentSetNumber = 1
            android.util.Log.d("ExerciseActivity", "No session found, starting from set 1")
        }
        
        android.util.Log.d("ExerciseActivity", "Final currentSetNumber: $currentSetNumber")
        android.util.Log.d("ExerciseActivity", "=== END DETERMINE CURRENT SET ===")
    }
    
    /**
     * Lấy số sets từ session data
     */
    private fun getSetsFromSession(): Int {
        return currentSession?.perExercise?.find { 
            it.getExerciseNo() == currentExerciseNo 
        }?.sets?.size ?: 3
    }
    
    /**
     * Lấy số reps từ session data
     */
    private fun getRepsFromSession(): Int {
        return currentSession?.perExercise?.find { 
            it.getExerciseNo() == currentExerciseNo 
        }?.sets?.firstOrNull()?.getTargetReps() ?: 12
    }
    
    /**
     * Update exercise state based on sets status
     */
    private fun updateExerciseStateIfNeeded(perExercise: Session.PerExercise) {
        val currentState = perExercise.getState()
        val setsList = perExercise.getSets() ?: ArrayList()
        
        // Count sets by status
        val completedSets = setsList.count { it.getState() == "completed" }
        val skippedSets = setsList.count { it.getState() == "skipped" }
        val totalProcessedSets = completedSets + skippedSets
        
        android.util.Log.d("ExerciseActivity", "Exercise state check: current=$currentState, completed=$completedSets, skipped=$skippedSets, totalProcessed=$totalProcessedSets, totalSets=${setsList.size}")
        
        when (currentState) {
            "not_started" -> {
                // If any set is processed (completed or skipped), change to "doing"
                if (totalProcessedSets > 0) {
                    android.util.Log.d("ExerciseActivity", "Exercise started - changing state to 'doing'")
                    updateExerciseState("doing")
                }
            }
            "doing" -> {
                // If all sets are processed, change to "completed"
                if (totalProcessedSets >= setsList.size) {
                    android.util.Log.d("ExerciseActivity", "All sets processed - changing state to 'completed'")
                    updateExerciseState("completed")
                }
            }
            // "completed" state doesn't need to change
        }
    }
}
