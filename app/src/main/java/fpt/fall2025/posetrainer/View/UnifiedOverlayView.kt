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
 * UnifiedOverlayView - Overlay view chung cho tất cả các bài tập
 * Chỉ hiển thị feedbackList và cảnh báo camera nếu cần
 */
class UnifiedOverlayView @JvmOverloads constructor(
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
                /*
                fun lm(idx: Int): Pair<Float, Float> {
                    val pt = landmarkList.getOrNull(idx)
                    return if (pt != null) Pair(pt.x() * imageWidth * scaleFactor, pt.y() * imageHeight * scaleFactor) else Pair(0f, 0f)
                }
                
                // Lấy các điểm cơ bản cho vẽ skeleton
                val nose = lm(0)
                val leftEar = lm(7)
                val rightEar = lm(8)
                val leftShoulder = lm(11)
                val rightShoulder = lm(12)
                val leftElbow = lm(13)
                val rightElbow = lm(14)
                val leftWrist = lm(15)
                val rightWrist = lm(16)
                val leftHip = lm(23)
                val rightHip = lm(24)
                val leftKnee = lm(25)
                val rightKnee = lm(26)
                val leftAnkle = lm(27)
                val rightAnkle = lm(28)
                val leftFoot = lm(31)
                val rightFoot = lm(32)

                // Vẽ skeleton đầy đủ
                val jointPaint = Paint(linePaint).apply {
                    strokeWidth = 8f
                    color = Color.parseColor("#66ccff")
                }
                
                // Vẽ đường nối tai - vai - khuỷu tay - cổ tay (cánh tay)
                canvas.drawLine(leftEar.first, leftEar.second, leftShoulder.first, leftShoulder.second, jointPaint)
                canvas.drawLine(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second, jointPaint)
                canvas.drawLine(leftElbow.first, leftElbow.second, leftWrist.first, leftWrist.second, jointPaint)
                canvas.drawLine(rightEar.first, rightEar.second, rightShoulder.first, rightShoulder.second, jointPaint)
                canvas.drawLine(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second, jointPaint)
                canvas.drawLine(rightElbow.first, rightElbow.second, rightWrist.first, rightWrist.second, jointPaint)
                
                // Vẽ đường nối vai - hông
                canvas.drawLine(leftShoulder.first, leftShoulder.second, leftHip.first, leftHip.second, jointPaint)
                canvas.drawLine(rightShoulder.first, rightShoulder.second, rightHip.first, rightHip.second, jointPaint)
                
                // Vẽ đường nối hông - đầu gối - mắt cá chân - bàn chân (chân)
                canvas.drawLine(leftHip.first, leftHip.second, leftKnee.first, leftKnee.second, jointPaint)
                canvas.drawLine(leftKnee.first, leftKnee.second, leftAnkle.first, leftAnkle.second, jointPaint)
                canvas.drawLine(leftAnkle.first, leftAnkle.second, leftFoot.first, leftFoot.second, jointPaint)
                canvas.drawLine(rightHip.first, rightHip.second, rightKnee.first, rightKnee.second, jointPaint)
                canvas.drawLine(rightKnee.first, rightKnee.second, rightAnkle.first, rightAnkle.second, jointPaint)
                canvas.drawLine(rightAnkle.first, rightAnkle.second, rightFoot.first, rightFoot.second, jointPaint)
                
                // Vẽ đường nối vai trái - vai phải
                val shoulderLinePaint = Paint(linePaint).apply {
                    strokeWidth = 6f
                    color = Color.parseColor("#FF6B6B")
                }
                canvas.drawLine(leftShoulder.first, leftShoulder.second, rightShoulder.first, rightShoulder.second, shoulderLinePaint)
                
                // Vẽ đường nối hông trái - hông phải
                val hipLinePaint = Paint(linePaint).apply {
                    strokeWidth = 6f
                    color = Color.parseColor("#FF6B6B")
                }
                canvas.drawLine(leftHip.first, leftHip.second, rightHip.first, rightHip.second, hipLinePaint)

                // Vẽ các điểm landmark
                val leftPaint = Paint(pointPaint).apply { color = Color.YELLOW }
                val rightPaint = Paint(pointPaint).apply { color = Color.CYAN }
                val earPaint = Paint(pointPaint).apply { color = Color.GREEN }
                val footPaint = Paint(pointPaint).apply { color = Color.MAGENTA }
                
                // Vẽ điểm tai
                canvas.drawCircle(leftEar.first, leftEar.second, 12f, earPaint)
                canvas.drawCircle(rightEar.first, rightEar.second, 12f, earPaint)
                
                // Vẽ điểm bên trái
                listOf(leftShoulder, leftElbow, leftWrist, leftHip, leftKnee, leftAnkle).forEach {
                    canvas.drawCircle(it.first, it.second, 14f, leftPaint)
                }
                
                // Vẽ điểm bên phải
                listOf(rightShoulder, rightElbow, rightWrist, rightHip, rightKnee, rightAnkle).forEach {
                    canvas.drawCircle(it.first, it.second, 14f, rightPaint)
                }
                
                // Vẽ điểm bàn chân
                canvas.drawCircle(leftFoot.first, leftFoot.second, 12f, footPaint)
                canvas.drawCircle(rightFoot.first, rightFoot.second, 12f, footPaint)
                */

                // Hiển thị cảnh báo camera nếu cần
                if (feedback?.isCameraWarning() == true) {
                    val warningPaint = Paint().apply {
                        color = Color.RED
                        textSize = 80f
                        style = Paint.Style.FILL
                        setShadowLayer(12f, 0f, 0f, Color.BLACK)
                        textAlign = Paint.Align.CENTER
                    }
                    val warningBg = Paint().apply {
                        color = Color.argb(180, 255, 0, 0)
                        style = Paint.Style.FILL
                    }
                    
                    // Vẽ nền cảnh báo
                    val warningText = "Vui lòng vào tư thế"
                    val warningY = height / 2f
                    val warningPadding = 40f
                    val warningWidth = warningPaint.measureText(warningText) + 2 * warningPadding
                    val warningHeight = 120f
                    
                    canvas.drawRoundRect(
                        width/2f - warningWidth/2f,
                        warningY - warningHeight/2f,
                        width/2f + warningWidth/2f,
                        warningY + warningHeight/2f,
                        20f, 20f,
                        warningBg
                    )
                    
                    // Vẽ text cảnh báo
                    canvas.drawText(warningText, width/2f, warningY + 20f, warningPaint)
                }

                // Hiển thị feedbackList (các cảnh báo động tác)
                val feedbacks = feedback?.feedbackList ?: emptyList()
                if (feedbacks.isNotEmpty()) {
                    val fbPaint = Paint().apply {
                        color = Color.rgb(255, 140, 0)
                        textSize = 60f
                        style = Paint.Style.FILL
                        setShadowLayer(10f, 0f, 0f, Color.BLACK)
                    }
                    
                    // Vẽ từ dưới lên, mỗi dòng cách nhau 80f
                    feedbacks.forEachIndexed { i, msg ->
                        canvas.drawText(
                            msg, 
                            40f, 
                            height - 220f - (feedbacks.size - 1 - i) * 80f, 
                            fbPaint
                        )
                    }
                }
            }
        }
    }
}

