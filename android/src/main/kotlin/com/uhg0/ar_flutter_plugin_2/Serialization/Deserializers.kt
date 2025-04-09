package com.uhg0.ar_flutter_plugin_2.Serialization

// --- SceneView Math Imports (Uniquement pour les types de retour si une fonction les utilise encore) ---
// Garder les alias SceneView uniquement si nécessaire pour l'interopérabilité directe,
// sinon, préférez les types dev.romainguy.
import io.github.sceneview.math.Position as ScenePosition
import io.github.sceneview.math.Scale as SceneScale
import io.github.sceneview.math.Quaternion as SceneQuaternion // Alias pour le type de propriété du nœud

// +++ dev.romainguy.kotlin.math Imports +++
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.normalize
import dev.romainguy.kotlin.math.length
import dev.romainguy.kotlin.math.Mat3
// --- Fin Imports ---

import kotlin.math.sqrt


// Fonction utilitaire pour convertir Mat3 en Quaternion (algorithme standard)
fun mat3ToQuaternion(m: Mat3): Quaternion {
    val t = m[0, 0] + m[1, 1] + m[2, 2] // Trace
    val q: Quaternion

    if (t > 0.0f) {
        val s = sqrt(t + 1.0f) * 2.0f
        q = Quaternion(
            x = (m[2, 1] - m[1, 2]) / s,
            y = (m[0, 2] - m[2, 0]) / s,
            z = (m[1, 0] - m[0, 1]) / s,
            w = 0.25f * s
        )
    } else if ((m[0, 0] > m[1, 1]) && (m[0, 0] > m[2, 2])) {
        val s = sqrt(1.0f + m[0, 0] - m[1, 1] - m[2, 2]) * 2.0f
        q = Quaternion(
            x = 0.25f * s,
            y = (m[0, 1] + m[1, 0]) / s,
            z = (m[0, 2] + m[2, 0]) / s,
            w = (m[2, 1] - m[1, 2]) / s
        )
    } else if (m[1, 1] > m[2, 2]) {
        val s = sqrt(1.0f + m[1, 1] - m[0, 0] - m[2, 2]) * 2.0f
        q = Quaternion(
            x = (m[0, 1] + m[1, 0]) / s,
            y = 0.25f * s,
            z = (m[1, 2] + m[2, 1]) / s,
            w = (m[0, 2] - m[2, 0]) / s
        )
    } else {
        val s = sqrt(1.0f + m[2, 2] - m[0, 0] - m[1, 1]) * 2.0f
        q = Quaternion(
            x = (m[0, 2] + m[2, 0]) / s,
            y = (m[1, 2] + m[2, 1]) / s,
            z = 0.25f * s,
            w = (m[1, 0] - m[0, 1]) / s
        )
    }
    // Retourner normalisé car les calculs peuvent introduire de petites erreurs
    return normalize(q)
}


// --- Mise à jour de l'ancienne fonction pour utiliser les nouveaux types ---
// Attention : la conversion de Mat4 vers Quaternion ici est simpliste et peut être moins robuste que celle de deserializeMatrixComponentsFromList
fun deserializeMatrix4(transform: List<Double>): Pair<Float3, Quaternion> {
     if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }
    val matrixArray = transform.map { it.toFloat() }.toFloatArray()
    // Correction: Utiliser le constructeur standard
    val mat4 = Mat4(matrixArray)

    // Position
    val position = Float3(mat4[3].x, mat4[3].y, mat4[3].z)

    // Rotation (simplifié)
    val scaleX = length(mat4[0].xyz)
    val scaleY = length(mat4[1].xyz)
    val scaleZ = length(mat4[2].xyz)
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ

    // Créer la matrice de rotation pure (Mat4)
    val rotMat = Mat4(
        Float4(mat4[0].xyz / safeScaleX, 0f),
        Float4(mat4[1].xyz / safeScaleY, 0f),
        Float4(mat4[2].xyz / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f)
    )
    // Correction: Utiliser la construction manuelle de Mat3 (Tentative 2)
    val rotationMat3 = Mat3(
        rotMat[0].x, rotMat[0].y, rotMat[0].z,
        rotMat[1].x, rotMat[1].y, rotMat[1].z,
        rotMat[2].x, rotMat[2].y, rotMat[2].z
    )

    val rotationQuaternion = mat3ToQuaternion(rotationMat3)
    return Pair(position, rotationQuaternion)
}

// --- Fonction principale de désérialisation ---
fun deserializeMatrixComponentsFromList(transform: List<Double>): Triple<Float3, Quaternion, Float3> {
    if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }

    val matrixArray = transform.map { it.toFloat() }.toFloatArray()
    // Correction: Utiliser le constructeur standard
    val mat4 = Mat4(matrixArray)

    // Position
    val position = Float3(mat4[3].x, mat4[3].y, mat4[3].z)

    // Scale
    val scaleX = length(mat4[0].xyz)
    val scaleY = length(mat4[1].xyz)
    val scaleZ = length(mat4[2].xyz)
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ
    val scale = Float3(safeScaleX, safeScaleY, safeScaleZ)

    // Rotation
    // Créer la matrice de rotation pure (Mat4)
    val rotMat = Mat4(
        Float4(mat4[0].xyz / safeScaleX, 0f),
        Float4(mat4[1].xyz / safeScaleY, 0f),
        Float4(mat4[2].xyz / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f)
    )
    // Correction: Utiliser la construction manuelle de Mat3 (Tentative 2)
    val rotationMat3 = Mat3(
        rotMat[0].x, rotMat[0].y, rotMat[0].z,
        rotMat[1].x, rotMat[1].y, rotMat[1].z,
        rotMat[2].x, rotMat[2].y, rotMat[2].z
    )

    val quaternion = mat3ToQuaternion(rotationMat3)
    return Triple(position, quaternion, scale)
} 