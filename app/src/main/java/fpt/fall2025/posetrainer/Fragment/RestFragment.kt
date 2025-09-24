package fpt.fall2025.posetrainer.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import fpt.fall2025.posetrainer.Domain.Exercise
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate
import fpt.fall2025.posetrainer.databinding.FragmentRestBinding

/**
 * RestFragment - Fragment hiển thị màn hình nghỉ ngơi giữa các exercises
 */
class RestFragment : Fragment() {
    
    companion object {
        private const val REST_DURATION = 30000L // 30 seconds
        private const val TAG = "RestFragment"
    }
    
    private var _binding: FragmentRestBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var nextExercise: Exercise
    private lateinit var workoutTemplate: WorkoutTemplate
    private var exerciseIndex: Int = 0
    private var totalExercises: Int = 1
    private var countDownTimer: CountDownTimer? = null
    
    interface RestFragmentListener {
        fun onRestCompleted(exerciseIndex: Int)
        fun onSkipRest(exerciseIndex: Int)
        fun onStartNow(exerciseIndex: Int)
    }
    
    private var listener: RestFragmentListener? = null
    
    fun setRestFragmentListener(listener: RestFragmentListener) {
        this.listener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get data from arguments
        arguments?.let { args ->
            nextExercise = args.getSerializable("nextExercise") as Exercise
            workoutTemplate = args.getSerializable("workoutTemplate") as WorkoutTemplate
            exerciseIndex = args.getInt("exerciseIndex", 0)
            totalExercises = args.getInt("totalExercises", 1)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        startRestTimer()
    }
    
    private fun setupUI() {
        // Set exercise info
        binding.exerciseNameTxt.text = nextExercise.name
        binding.exerciseDescriptionTxt.text = "Get ready for the next exercise"
        
        // Set progress
        binding.progressTxt.text = "Exercise ${exerciseIndex + 1} of $totalExercises"
        
        // Set rest duration
        binding.restTimeTxt.text = "Rest for 30 seconds"
        
        // Skip button
        binding.skipBtn.setOnClickListener {
            skipRest()
        }
        
        // Start Now button
        binding.startBtn.setOnClickListener {
            startNow()
        }
        
        // Back button
        binding.backBtn.setOnClickListener {
            activity?.finish()
        }
    }
    
    private fun startRestTimer() {
        countDownTimer = object : CountDownTimer(REST_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.restTimeTxt.text = "Rest for $secondsRemaining seconds"
                
                // Update progress bar
                val progress = ((REST_DURATION - millisUntilFinished) * 100 / REST_DURATION).toInt()
                binding.progressBar.progress = progress
            }
            
            override fun onFinish() {
                binding.restTimeTxt.text = "Ready to start!"
                binding.progressBar.progress = 100
                listener?.onRestCompleted(exerciseIndex)
            }
        }
        countDownTimer?.start()
    }
    
    private fun skipRest() {
        countDownTimer?.cancel()
        listener?.onSkipRest(exerciseIndex)
    }
    
    private fun startNow() {
        countDownTimer?.cancel()
        listener?.onStartNow(exerciseIndex)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}
