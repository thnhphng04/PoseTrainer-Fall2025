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
                val leftFoot = lm(31)
                val leftEar = lm(7)
                val rightShoulder = lm(12)
                val rightElbow = lm(14)
                val rightWrist = lm(16)
                val rightHip = lm(24)
                val rightKnee = lm(26)
                val rightAnkle = lm(28)
                val rightFoot = lm(32)
                val rightEar = lm(8)
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
                    // VẼ ĐẦY ĐỦ ĐỘNG TÁC BÊN TRỤ
                    val distL = kotlin.math.abs(leftFoot.second - leftShoulder.second)
                    val distR = kotlin.math.abs(rightFoot.second - rightShoulder.second)
                    val isLeft = distL > distR
                    val points = if (isLeft) {
                        listOf(leftEar, leftShoulder, leftElbow, leftWrist, leftHip, leftKnee, leftAnkle, leftFoot)
                    } else {
                        listOf(rightEar, rightShoulder, rightElbow, rightWrist, rightHip, rightKnee, rightAnkle, rightFoot)
                    }
                    val ear = points[0]
                    val shldr = points[1]
                    val elbow = points[2]
                    val wrist = points[3]
                    val hip = points[4]
                    val knee = points[5]
                    val ankle = points[6]
                    val foot = points[7]

                    // Vẽ các đường nối bên trụ
                    val jointPaint = Paint(linePaint)
                    jointPaint.strokeWidth = 8f
                    jointPaint.color = Color.parseColor("#66ccff")
                    canvas.drawLine(ear.first, ear.second, shldr.first, shldr.second, jointPaint)
                    canvas.drawLine(shldr.first, shldr.second, hip.first, hip.second, jointPaint)
                    canvas.drawLine(hip.first, hip.second, knee.first, knee.second, jointPaint)
                    canvas.drawLine(knee.first, knee.second, ankle.first, ankle.second, jointPaint)

                    // Vẽ đường nối ear-elbow và elbow-hip
                    val earElbowPaint = Paint(linePaint)
                    earElbowPaint.strokeWidth = 6f
                    earElbowPaint.color = Color.parseColor("#FF6B6B") // Màu đỏ cam
                    canvas.drawLine(ear.first, ear.second, elbow.first, elbow.second, earElbowPaint)
                    
                    val elbowHipPaint = Paint(linePaint)
                    elbowHipPaint.strokeWidth = 6f
                    elbowHipPaint.color = Color.parseColor("#FF6B6B") // Màu đỏ cam
                    canvas.drawLine(elbow.first, elbow.second, hip.first, hip.second, elbowHipPaint)

                    // Vẽ các điểm landmark bên trụ
                    val mainPaint = Paint(pointPaint)
                    mainPaint.color = if (isLeft) Color.YELLOW else Color.CYAN
                    listOf(shldr, elbow, wrist, hip, knee, ankle, foot, ear).forEach {
                        canvas.drawCircle(it.first, it.second, 14f, mainPaint)
                    }

                    /*
                    // Vẽ số liệu các góc tại vị trí tương ứng
                    val anglePaint = Paint().apply {
                        color = Color.GREEN
                        textSize = 48f
                        style = Paint.Style.FILL
                        setShadowLayer(8f, 0f, 0f, Color.BLACK)
                    }

                    canvas.drawText("${feedback?.shoulderAngle ?: 0}", shldr.first + 10, shldr.second, anglePaint)
                    canvas.drawText("${feedback?.hipAngle ?: 0}", hip.first + 10, hip.second, anglePaint)
                    canvas.drawText("${feedback?.kneeAngle ?: 0}", knee.first + 15, knee.second + 10, anglePaint)

                    // Vẽ số liệu góc ear-elbow-hip tại elbow
                    val earElbowHipAnglePaint = Paint().apply {
                        color = Color.parseColor("#FF6B6B") // Màu đỏ cam giống cung tròn
                        textSize = 42f
                        style = Paint.Style.FILL
                        setShadowLayer(8f, 0f, 0f, Color.BLACK)
                    }
                    canvas.drawText("${feedback?.earElbowHipAngle ?: 0}", elbow.first + 10, elbow.second + 5, earElbowHipAnglePaint)
                    */

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

                /*
                // Hiển thị bộ đếm Push-up đúng/sai ở phía dưới cùng (luôn luôn hiển thị)
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
        }
    }
}
