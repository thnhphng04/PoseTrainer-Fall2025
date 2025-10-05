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
 * BurpeesOverlayView - Overlay view for Burpees exercise
 * Dựa trên logic từ project Burpees nghiên cứu
 */
class BurpeeOverlayView @JvmOverloads constructor(
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
                
                // Lấy các điểm landmark cần thiết cho Burpees
                val nose = lm(0)
                val leftEar = lm(7)
                val rightEar = lm(8)
                val leftShoulder = lm(11)
                val leftElbow = lm(13)
                val leftWrist = lm(15)
                val leftHip = lm(23)
                val leftKnee = lm(25)
                val leftAnkle = lm(27)
                val leftFoot = lm(31)
                val rightShoulder = lm(12)
                val rightElbow = lm(14)
                val rightWrist = lm(16)
                val rightHip = lm(24)
                val rightKnee = lm(26)
                val rightAnkle = lm(28)
                val rightFoot = lm(32)

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
                    // VẼ ĐẦY ĐỦ ĐỘNG TÁC BURPEES
                    
                    // Vẽ các đường nối cho cơ thể
                    val jointPaint = Paint(linePaint)
                    jointPaint.strokeWidth = 8f
                    jointPaint.color = Color.parseColor("#66ccff")
                    
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
                    val shoulderLinePaint = Paint(linePaint)
                    shoulderLinePaint.strokeWidth = 6f
                    shoulderLinePaint.color = Color.parseColor("#FF6B6B")
                    canvas.drawLine(leftShoulder.first, leftShoulder.second, rightShoulder.first, rightShoulder.second, shoulderLinePaint)
                    
                    // Vẽ đường nối hông trái - hông phải
                    val hipLinePaint = Paint(linePaint)
                    hipLinePaint.strokeWidth = 6f
                    hipLinePaint.color = Color.parseColor("#FF6B6B")
                    canvas.drawLine(leftHip.first, leftHip.second, rightHip.first, rightHip.second, hipLinePaint)

                    // Vẽ các điểm landmark với màu sắc khác nhau
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

                    // Hiển thị các cảnh báo động tác nếu có (ở phía dưới)
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

                // Hiển thị trạng thái hiện tại ở góc trên bên trái
                val currentState = feedback?.currentState
                if (!currentState.isNullOrEmpty()) {
                    val statePaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 48f
                        style = Paint.Style.FILL
                        setShadowLayer(8f, 0f, 0f, Color.BLACK)
                    }
                    val stateBg = Paint().apply {
                        color = Color.rgb(150, 0, 150)
                        style = Paint.Style.FILL
                    }
                    val stateText = "State: $currentState"
                    val statePadding = 20f
                    val stateWidth = statePaint.measureText(stateText) + 2*statePadding
                    val stateHeight = 60f
                    val stateX = 40f
                    val stateY = 100f
                    
                    // Vẽ nền cho trạng thái
                    canvas.drawRoundRect(stateX, stateY-stateHeight/2, stateX+stateWidth, stateY+stateHeight/2, 15f, 15f, stateBg)
                    // Vẽ text trạng thái
                    canvas.drawText(stateText, stateX+statePadding, stateY+15f, statePaint)
                }

                // Hiển thị bộ đếm Burpees đúng/sai ở phía dưới cùng
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
                val correctText = "Burpees đúng: ${feedback?.correctCount ?: 0}"
                val incorrectText = "Burpees sai: ${feedback?.incorrectCount ?: 0}"
                val padding = 30f
                val bottomY = height - 200f
                val spacing = 80f
                val correctWidth = countPaint.measureText(correctText) + 2*padding
                val incorrectWidth = countPaint.measureText(incorrectText) + 2*padding
                // Nền xanh cho Burpees đúng
                canvas.drawRoundRect(width-correctWidth-40f, bottomY-spacing-40f, width-40f, bottomY-spacing+30f, 30f, 30f, correctBg)
                // Nền đỏ cho Burpees sai
                canvas.drawRoundRect(width-incorrectWidth-40f, bottomY-40f, width-40f, bottomY+30f, 30f, 30f, incorrectBg)
                // Text
                canvas.drawText(correctText, width-correctWidth-padding, bottomY-spacing, countPaint)
                canvas.drawText(incorrectText, width-incorrectWidth-padding, bottomY, countPaint)

            }
        }
    }
}
