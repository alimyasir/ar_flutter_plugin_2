package com.uhg0.ar_flutter_plugin_2.Serialization

import android.util.Log
import io.github.sceneview.math.Position as ScenePosition
import io.github.sceneview.math.Rotation as SceneRotation
import io.github.sceneview.math.Scale as SceneScale
import kotlin.math.sqrt
import kotlin.math.atan2

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
    val m00 = transform[0].toFloat() / scaleX
    val m01 = transform[4].toFloat() / scaleY
    val m02 = transform[8].toFloat() / scaleZ
    val m10 = transform[1].toFloat() / scaleX
    val m11 = transform[5].toFloat() / scaleY
    val m12 = transform[9].toFloat() / scaleZ
    val m20 = transform[2].toFloat() / scaleX
    val m21 = transform[6].toFloat() / scaleY
    val m22 = transform[10].toFloat() / scaleZ

    Log.d("Deserializers", "Matrice de rotation normalisée: [$m00, $m01, $m02, $m10, $m11, $m12, $m20, $m21, $m22]")
    
    // Conversion de la matrice de rotation en angles d'Euler
    val rotX = atan2(m21, m22)
    val rotY = atan2(-m20, sqrt(m21 * m21 + m22 * m22))
    val rotZ = atan2(m10, m00)
    
    // Ajout des corrections pour le système de coordonnées
    val correctedRotX = rotX
    val correctedRotY = rotY + Math.PI.toFloat() // Rotation de 180° autour de Y
    val correctedRotZ = rotZ + Math.PI.toFloat() // Rotation de 180° autour de Z
    
    val rotation = SceneRotation(correctedRotX, correctedRotY, correctedRotZ)
    
    Log.d("Deserializers", "Rotation extraite (euler): x=${rotation.x}, y=${rotation.y}, z=${rotation.z}")

    return Triple(position, rotation, scale)
} 