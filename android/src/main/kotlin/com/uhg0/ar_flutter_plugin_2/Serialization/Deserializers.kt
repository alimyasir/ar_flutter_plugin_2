package com.uhg0.ar_flutter_plugin_2.Serialization

import android.util.Log
import io.github.sceneview.math.Position as ScenePosition
import io.github.sceneview.math.Rotation as SceneRotation
import io.github.sceneview.math.Scale as SceneScale
import kotlin.math.sqrt

fun deserializeMatrix4(transform: ArrayList<Double>): Triple<ScenePosition, SceneRotation, SceneScale> {
    // Position
    val position = ScenePosition(
        x = transform[12].toFloat(),
        y = transform[13].toFloat(),
        z = transform[14].toFloat()
    )
    
    Log.d("Deserializers", "Position extraite: x=${position.x}, y=${position.y}, z=${position.z}")

    // Scale Calculation
    val scaleX = sqrt(transform[0]*transform[0] + transform[1]*transform[1] + transform[2]*transform[2]).toFloat()
    val scaleY = sqrt(transform[4]*transform[4] + transform[5]*transform[5] + transform[6]*transform[6]).toFloat()
    val scaleZ = sqrt(transform[8]*transform[8] + transform[9]*transform[9] + transform[10]*transform[10]).toFloat()
    val scale = SceneScale(scaleX, scaleY, scaleZ)
    
    Log.d("Deserializers", "Échelle extraite: x=${scale.x}, y=${scale.y}, z=${scale.z}")

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

    Log.d("Deserializers", "Matrice de rotation brute: [$m00, $m01, $m02, $m10, $m11, $m12, $m20, $m21, $m22]")
    
    val trace = m00 + m11 + m22
    Log.d("Deserializers", "Trace de la matrice: $trace")
    
    val rotation = if (trace > 0) {
        val s = sqrt(trace + 1.0f) * 2f
        Log.d("Deserializers", "Valeur de s pour le calcul du quaternion: $s")
        
        val qx = (m21 - m12) / s
        val qy = (m02 - m20) / s
        val qz = (m10 - m01) / s
        
        SceneRotation(x = qx, y = qy, z = qz)
    } else {
        Log.d("Deserializers", "Trace négative ou nulle, utilisation d'une rotation par défaut")
        SceneRotation()
    }
    
    Log.d("Deserializers", "Rotation extraite (quaternion): x=${rotation.x}, y=${rotation.y}, z=${rotation.z}")

    return Triple(position, rotation, scale)
} 