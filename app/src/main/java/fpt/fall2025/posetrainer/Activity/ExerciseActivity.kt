package fpt.fall2025.posetrainer.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import fpt.fall2025.posetrainer.Domain.Exercise
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate
import fpt.fall2025.posetrainer.Fragment.UnifiedCameraFragment
import fpt.fall2025.posetrainer.Fragment.RestFragment
import fpt.fall2025.posetrainer.Manager.WorkoutSessionManager
import fpt.fall2025.posetrainer.R

/**
 * ExerciseActivity - Activity để host camera fragment cho từng bài tập
 */
class ExerciseActivity : AppCompatActivity() {
    
    private lateinit var exercise: Exercise
    private var sets: Int = 3
    private var reps: Int = 12
    private lateinit var workoutTemplate: WorkoutTemplate
    private lateinit var sessionManager: WorkoutSessionManager
    private var exerciseIndex: Int = 0
    private var totalExercises: Int = 1
    private var nextExercise: Exercise? = null
    private var isRestMode: Boolean = false
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)
        
        // Full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Get data from intent
        exercise = intent.getSerializableExtra("exercise") as Exercise
        sets = intent.getIntExtra("sets", 3)
        reps = intent.getIntExtra("reps", 12)
        workoutTemplate = intent.getSerializableExtra("workoutTemplate") as WorkoutTemplate
        exerciseIndex = intent.getIntExtra("exerciseIndex", 0)
        totalExercises = intent.getIntExtra("totalExercises", 1)
        nextExercise = intent.getSerializableExtra("nextExercise") as? Exercise
        isRestMode = intent.getBooleanExtra("isRestMode", false)
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            initializeCamera()
        }
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
        if (isRestMode && nextExercise != null) {
            // Show rest fragment
            showRestFragment()
        } else {
            // Show exercise fragment
            showExerciseFragment()
        }
    }
    
    private fun showExerciseFragment() {
        // Create new session manager - we need to load exercises first
        // For now, create with empty exercises list
        val exercises = ArrayList<Exercise>()
        exercises.add(exercise) // Add current exercise
        sessionManager = WorkoutSessionManager(workoutTemplate, exercises)
        sessionManager.startWorkout()
        
        // Create and add UnifiedCameraFragment
        val cameraFragment = UnifiedCameraFragment()
        
        // Pass data to fragment
        val args = Bundle().apply {
            putSerializable("exercise", exercise)
            putInt("sets", sets)
            putInt("reps", reps)
            putSerializable("sessionManager", sessionManager)
        }
        cameraFragment.arguments = args
        
        // Add fragment to container
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, cameraFragment)
        transaction.commit()
    }
    
    private fun showRestFragment() {
        val restFragment = RestFragment()
        
        // Pass data to fragment
        val args = Bundle().apply {
            putSerializable("nextExercise", nextExercise)
            putSerializable("workoutTemplate", workoutTemplate)
            putInt("exerciseIndex", exerciseIndex)
            putInt("totalExercises", totalExercises)
        }
        restFragment.arguments = args
        
        // Set listener
        restFragment.setRestFragmentListener(object : RestFragment.RestFragmentListener {
            override fun onRestCompleted(exerciseIndex: Int) {
                // Auto start next exercise
                startNextExercise()
            }
            
            override fun onSkipRest(exerciseIndex: Int) {
                // Skip rest and start next exercise
                startNextExercise()
            }
            
            override fun onStartNow(exerciseIndex: Int) {
                // Start next exercise immediately
                startNextExercise()
            }
        })
        
        // Add fragment to container
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, restFragment)
        transaction.commit()
    }
    
    private fun startNextExercise() {
        if (nextExercise != null) {
            // Update current exercise to next exercise
            exercise = nextExercise!!
            
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
        if (exerciseIndex + 1 < totalExercises) {
            // Show rest screen before next exercise
            showRestScreenForNextExercise()
        } else {
            // No more exercises, return to WorkoutActivity
            val resultIntent = Intent().apply {
                putExtra("completedIndex", exerciseIndex)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
    
    private fun showRestScreenForNextExercise() {
        // Get next exercise from WorkoutActivity
        val nextExercise = getNextExercise()
        if (nextExercise != null) {
            // Show rest fragment with next exercise
            showRestFragmentWithNextExercise(nextExercise)
        } else {
            // No next exercise, finish
            finish()
        }
    }
    
    private fun getNextExercise(): Exercise? {
        // This should be passed from WorkoutActivity
        // For now, we'll get it from intent or return null
        return intent.getSerializableExtra("nextExercise") as? Exercise
    }
    
    private fun showRestFragmentWithNextExercise(nextExercise: Exercise) {
        val restFragment = RestFragment()
        
        // Pass data to fragment
        val args = Bundle().apply {
            putSerializable("nextExercise", nextExercise)
            putSerializable("workoutTemplate", workoutTemplate)
            putInt("exerciseIndex", exerciseIndex + 1)
            putInt("totalExercises", totalExercises)
        }
        restFragment.arguments = args
        
        // Set listener
        restFragment.setRestFragmentListener(object : RestFragment.RestFragmentListener {
            override fun onRestCompleted(exerciseIndex: Int) {
                // Auto start next exercise
                startNextExercise()
            }
            
            override fun onSkipRest(exerciseIndex: Int) {
                // Skip rest and start next exercise
                startNextExercise()
            }
            
            override fun onStartNow(exerciseIndex: Int) {
                // Start next exercise immediately
                startNextExercise()
            }
        })
        
        // Add fragment to container
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, restFragment)
        transaction.commit()
    }
    
    private fun markExerciseAsCompleted() {
        // You can implement logic to mark exercise as completed
        // For example, save to database or update UI
        android.util.Log.d("ExerciseActivity", "Exercise $exerciseIndex completed")
    }
}
