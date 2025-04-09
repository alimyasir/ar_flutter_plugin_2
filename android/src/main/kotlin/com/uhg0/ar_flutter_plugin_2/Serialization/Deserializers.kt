package com.uhg0.ar_flutter_plugin_2.Serialization

// --- SceneView Math Imports (Uniquement pour les types de retour si une fonction les utilise encore) ---
// Garder les alias SceneView uniquement si nécessaire pour l'interopérabilité directe,
// sinon, préférez les types dev.romainguy.
import io.github.sceneview.math.Position as ScenePosition
import io.github.sceneview.math.Scale as SceneScale
import io.github.sceneview.math.Quaternion as SceneQuaternion // Alias pour le type de propriété du nœud

// +++ dev.romainguy.kotlin.math Imports +++
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion // Le type Quaternion utilisé pour les calculs
import dev.romainguy.kotlin.math.Mat4       // Importer Mat4
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.normalize // Fonction de normalisation
import dev.romainguy.kotlin.math.length    // Fonction de longueur
import dev.romainguy.kotlin.math.Mat3
import dev.romainguy.kotlin.math.rotation
// --- Fin Imports ---

import kotlin.math.sqrt


// --- Mise à jour de l'ancienne fonction pour utiliser les nouveaux types ---
// Attention : la conversion de Mat4 vers Quaternion ici est simpliste et peut être moins robuste que celle de deserializeMatrixComponentsFromList
fun deserializeMatrix4(transform: List<Double>): Pair<Float3, Quaternion> {
     if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }
    val matrixArray = transform.map { it.toFloat() }.toFloatArray()
    // Correction: Utiliser le constructeur Mat4(FloatArray) direct (attend column-major)
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

    val rotMat = Mat4(
        Float4(mat4[0].xyz / safeScaleX, 0f),
        Float4(mat4[1].xyz / safeScaleY, 0f),
        Float4(mat4[2].xyz / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f)
    )
    // Correction: Extraire Mat3 et construire Quaternion depuis Mat3
    val rotationMat3: Mat3 = rotation(rotMat) // Extrait la sous-matrice 3x3
    val rotationQuaternion = Quaternion(rotationMat3) // Construit depuis Mat3

    // Correction : S'assurer que normalize est appelé sur le Quaternion
    return Pair(position, normalize(rotationQuaternion))
}

// --- Fonction principale de désérialisation ---
fun deserializeMatrixComponentsFromList(transform: List<Double>): Triple<Float3, Quaternion, Float3> {
    if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }

    val matrixArray = transform.map { it.toFloat() }.toFloatArray()
    // Correction: Utiliser le constructeur Mat4(FloatArray) direct
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
    val rotMat = Mat4(
        Float4(mat4[0].xyz / safeScaleX, 0f),
        Float4(mat4[1].xyz / safeScaleY, 0f),
        Float4(mat4[2].xyz / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f)
    )
    // Correction: Extraire Mat3 et construire Quaternion depuis Mat3
    val rotationMat3: Mat3 = rotation(rotMat) // Extrait la sous-matrice 3x3
    val quaternion = Quaternion(rotationMat3) // Construit depuis Mat3

    // Correction : S'assurer que normalize est appelé sur le Quaternion
    return Triple(position, normalize(quaternion), scale)
} 