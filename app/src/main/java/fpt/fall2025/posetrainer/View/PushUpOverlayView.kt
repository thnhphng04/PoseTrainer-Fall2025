package fpt.fall2025.posetrainer.View

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import fpt.fall2025.posetrainer.Analyzer.ExerciseFeedback
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlin.math.max
import kotlin.math.min

/**
 * PushUpOverlayView - Overlay view for Push-up exercise
 * Dựa trên logic từ project PushUp nghiên cứu
 */
class PushUpOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var poseResult: PoseLandmarkerResult? = null
    private var imageHeight: Int = 0
    private var imageWidth: Int = 0
    private var runningMode: RunningMode = RunningMode.LIVE_STREAM
    private var feedback: ExerciseFeedback? = null
    private var scaleFactor: Float = 1f

    private var pointPaint = Paint()
    private var linePaint = Paint()

    init {
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = Color.parseColor("#66ccff")
        linePaint.strokeWidth = 8f
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = 8f
        pointPaint.style = Paint.Style.FILL
    }

    fun setResults(
        poseResult: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode,
        feedback: ExerciseFeedback?
    ) {
        this.poseResult = poseResult
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        this.runningMode = runningMode
        this.feedback = feedback
        
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        poseResult?.let { result ->
            val landmarkList = result.landmarks().firstOrNull()
            if (landmarkList != null) {
                fun lm(idx: Int): Pair<Float, Float> {
                    val pt = landmarkList.getOrNull(idx)
                    return if (pt != null) Pair(pt.x() * imageWidth * scaleFactor, pt.y() * imageHeight * scaleFactor) else Pair(0f, 0f)
                }
                
                // Lấy các điểm landmark
                val leftShoulder = lm(11)
                val leftElbow = lm(13)
                val leftWrist = lm(15)
                val leftHip = lm(23)
                val leftKnee = lm(25)
                val leftAnkle = lm(27)
                val rightShoulder = lm(12)
                val rightElbow = lm(14)
                val rightWrist = lm(16)
                val rightHip = lm(24)
                val rightKnee = lm(26)
                val rightAnkle = lm(28)
                val nose = lm(0)

                if (feedback?.isCameraWarning() == true) {
            // Vẽ landmark mũi, vai trái, vai phải và các đường nối
            val nosePaint = Paint(pointPaint).apply { color = Color.WHITE }
            val leftPaint = Paint(pointPaint).apply { color = Color.YELLOW }
            val rightPaint = Paint(pointPaint).apply { color = Color.CYAN }
            canvas.drawCircle(nose.first, nose.second, 18f, nosePaint)
            canvas.drawCircle(leftShoulder.first, leftShoulder.second, 18f, leftPaint)
            canvas.drawCircle(rightShoulder.first, rightShoulder.second, 18f, rightPaint)
            
            // Vẽ đường nối giữa 3 điểm
            val connectPaint = Paint(linePaint).apply { 
                color = Color.MAGENTA
                strokeWidth = 8f 
            }
            canvas.drawLine(leftShoulder.first, leftShoulder.second, rightShoulder.first, rightShoulder.second, connectPaint)
            canvas.drawLine(nose.first, nose.second, leftShoulder.first, leftShoulder.second, connectPaint)
            canvas.drawLine(nose.first, nose.second, rightShoulder.first, rightShoulder.second, connectPaint)
        } else {
            // Vẽ các đường nối cho Push-up
            val jointPaint = Paint(linePaint)
            jointPaint.strokeWidth = 8f
            jointPaint.color = Color.parseColor("#66ccff")

            // Vẽ đường nối tay trái
            canvas.drawLine(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second, jointPaint)
            canvas.drawLine(leftElbow.first, leftElbow.second, leftWrist.first, leftWrist.second, jointPaint)
            
            // Vẽ đường nối tay phải
            canvas.drawLine(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second, jointPaint)
            canvas.drawLine(rightElbow.first, rightElbow.second, rightWrist.first, rightWrist.second, jointPaint)
            
            // Vẽ đường nối thân người
            canvas.drawLine(leftShoulder.first, leftShoulder.second, leftHip.first, leftHip.second, jointPaint)
            canvas.drawLine(rightShoulder.first, rightShoulder.second, rightHip.first, rightHip.second, jointPaint)
            canvas.drawLine(leftHip.first, leftHip.second, rightHip.first, rightHip.second, jointPaint)
            
            // Vẽ đường nối chân
            canvas.drawLine(leftHip.first, leftHip.second, leftKnee.first, leftKnee.second, jointPaint)
            canvas.drawLine(leftKnee.first, leftKnee.second, leftAnkle.first, leftAnkle.second, jointPaint)
            canvas.drawLine(rightHip.first, rightHip.second, rightKnee.first, rightKnee.second, jointPaint)
            canvas.drawLine(rightKnee.first, rightKnee.second, rightAnkle.first, rightAnkle.second, jointPaint)

            // Vẽ các điểm landmark
            val mainPaint = Paint(pointPaint)
            mainPaint.color = Color.CYAN
            listOf(leftShoulder, leftElbow, leftWrist, leftHip, leftKnee, leftAnkle, 
                   rightShoulder, rightElbow, rightWrist, rightHip, rightKnee, rightAnkle).forEach {
                canvas.drawCircle(it.first, it.second, 14f, mainPaint)
            }

            // Hiển thị các cảnh báo động tác nếu có
            val feedbacks = feedback?.feedbackList ?: emptyList()
            if (feedbacks.isNotEmpty()) {
                val fbPaint = Paint().apply {
                    color = Color.rgb(255, 140, 0)
                    textSize = 70f
                    style = Paint.Style.FILL
                    setShadowLayer(10f, 0f, 0f, Color.BLACK)
                }
                feedbacks.forEachIndexed { i, msg ->
                    canvas.drawText(msg, 40f, height - 220f - (feedbacks.size-1-i)*80f, fbPaint)
                }
            }
        }

        /*
        // Hiển thị bộ đếm Push-up đúng/sai
        val countPaint = Paint().apply {
            color = Color.WHITE
            textSize = 54f
            style = Paint.Style.FILL
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }
        val correctBg = Paint().apply {
            color = Color.rgb(18, 185, 0)
            style = Paint.Style.FILL
        }
        val incorrectBg = Paint().apply {
            color = Color.rgb(221, 0, 0)
            style = Paint.Style.FILL
        }

        val correctText = "PushUp đúng: ${feedback?.correctCount ?: 0}"
        val incorrectText = "PushUp sai: ${feedback?.incorrectCount ?: 0}"
        val padding = 30f
        val bottomY = height - 200f
        val spacing = 80f
        val correctWidth = countPaint.measureText(correctText) + 2*padding
        val incorrectWidth = countPaint.measureText(incorrectText) + 2*padding
        
        // Nền xanh cho Push-up đúng
        canvas.drawRoundRect(width-correctWidth-40f, bottomY-spacing-40f, width-40f, bottomY-spacing+30f, 30f, 30f, correctBg)
        // Nền đỏ cho Push-up sai
        canvas.drawRoundRect(width-incorrectWidth-40f, bottomY-40f, width-40f, bottomY+30f, 30f, 30f, incorrectBg)
        // Text
        canvas.drawText(correctText, width-correctWidth-padding, bottomY-spacing, countPaint)
        canvas.drawText(incorrectText, width-incorrectWidth-padding, bottomY, countPaint)
        */
        

    }
}}}
