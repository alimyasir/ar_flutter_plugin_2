package com.uhg0.ar_flutter_plugin_2.Serialization

import io.github.sceneview.math.Position as ScenePosition
import io.github.sceneview.math.Rotation as SceneRotation
import io.github.sceneview.math.Scale as SceneScale
import io.github.sceneview.math.Quaternion as SceneQuaternion
import io.github.sceneview.math.Transform as SceneTransform
import io.github.sceneview.math.toQuaternion
import kotlin.math.sqrt

fun deserializeMatrix4(transform: ArrayList<Double>): Pair<ScenePosition, SceneRotation> {
    // Position
    val position = ScenePosition(
        x = transform[12].toFloat(),
        y = transform[13].toFloat(),
        z = transform[14].toFloat()
    )

    // Rotation
    val m00 = transform[0].toFloat()
    val m01 = transform[4].toFloat()
    val m02 = transform[8].toFloat()
    val m10 = transform[1].toFloat()
    val m11 = transform[5].toFloat()
    val m12 = transform[9].toFloat()
    val m20 = transform[2].toFloat()
    val m21 = transform[6].toFloat()
    val m22 = transform[10].toFloat()

    val trace = m00 + m11 + m22
    val rotation = if (trace > 0) {
        val s = sqrt(trace + 1.0f) * 2f
        SceneRotation(
            x = (m21 - m12) / s,
            y = (m02 - m20) / s,
            z = (m10 - m01) / s
        )
    } else {
        SceneRotation()
    }

    return Pair(position, rotation)
}

fun deserializeMatrixComponents(transform: List<Double>): Triple<ScenePosition, SceneQuaternion, SceneScale> {
    // Check size just in case
    if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }

    // Position
    val position = ScenePosition(
        x = transform[12].toFloat(),
        y = transform[13].toFloat(),
        z = transform[14].toFloat()
    )

    // Scale - Calculate scale from matrix columns' length
    val scaleX = sqrt(transform[0].toFloat() * transform[0].toFloat() + transform[1].toFloat() * transform[1].toFloat() + transform[2].toFloat() * transform[2].toFloat())
    val scaleY = sqrt(transform[4].toFloat() * transform[4].toFloat() + transform[5].toFloat() * transform[5].toFloat() + transform[6].toFloat() * transform[6].toFloat())
    val scaleZ = sqrt(transform[8].toFloat() * transform[8].toFloat() + transform[9].toFloat() * transform[9].toFloat() + transform[10].toFloat() * transform[10].toFloat())
    // Avoid division by zero if scale is zero (though unlikely for valid matrices)
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ
    val scale = SceneScale(safeScaleX, safeScaleY, safeScaleZ)


    // Rotation - Convert rotation part of matrix to Quaternion
    // Create Mat4 from the list (SceneTransform is an alias for Mat4)
    val mat4 = SceneTransform(
         floatArrayOf(
            transform[0].toFloat(), transform[1].toFloat(), transform[2].toFloat(), transform[3].toFloat(),
            transform[4].toFloat(), transform[5].toFloat(), transform[6].toFloat(), transform[7].toFloat(),
            transform[8].toFloat(), transform[9].toFloat(), transform[10].toFloat(), transform[11].toFloat(),
            transform[12].toFloat(), transform[13].toFloat(), transform[14].toFloat(), transform[15].toFloat()
        )
    )

    // Normalize the matrix first for stable quaternion conversion if scales are non-uniform or zero
    val normalizedMatrix = mat4.toMutableMat4().apply {
         // Divide columns by scale to get pure rotation
         this[0] = this[0] / safeScaleX
         this[1] = this[1] / safeScaleY
         this[2] = this[2] / safeScaleZ
         // Ensure the 4th column represents translation = 0 for rotation matrix
         this[3] = io.github.sceneview.math.Float4(0f, 0f, 0f, 1f)
    }
    val quaternion = normalizedMatrix.toQuaternion() // Use SceneView's conversion

    return Triple(position, quaternion, scale)
} 