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
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.normalize // Fonction de normalisation
import dev.romainguy.kotlin.math.length    // Fonction de longueur
// --- Fin Imports ---

import kotlin.math.sqrt


// --- Mise à jour de l'ancienne fonction pour utiliser les nouveaux types ---
// Attention : la conversion de Mat4 vers Quaternion ici est simpliste et peut être moins robuste que celle de deserializeMatrixComponentsFromList
fun deserializeMatrix4(transform: List<Double>): Pair<Float3, Quaternion> {
     if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }
    val matrixArray = transform.map { it.toFloat() }.toFloatArray()
    val mat4 = Mat4(matrixArray) // Utiliser le constructeur Mat4(FloatArray)

    // Position
    val position = Float3(mat4[3].x, mat4[3].y, mat4[3].z)

    // Rotation (simplifié - préférer la méthode de deserializeMatrixComponentsFromList)
    val scaleX = length(mat4[0].xyz)
    val scaleY = length(mat4[1].xyz)
    val scaleZ = length(mat4[2].xyz)
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ

    val rotMat = Mat4(
        Float4(mat4[0].x / safeScaleX, mat4[0].y / safeScaleX, mat4[0].z / safeScaleX, 0f),
        Float4(mat4[1].x / safeScaleY, mat4[1].y / safeScaleY, mat4[1].z / safeScaleY, 0f),
        Float4(mat4[2].x / safeScaleZ, mat4[2].y / safeScaleZ, mat4[2].z / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f)
    )
    val rotation = Quaternion(rotMat) // Utiliser le constructeur Quaternion(Mat4)

    return Pair(position, normalize(rotation))
}

// --- Fonction principale de désérialisation ---
fun deserializeMatrixComponentsFromList(transform: List<Double>): Triple<Float3, Quaternion, Float3> {
    if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }

    val matrixArray = transform.map { it.toFloat() }.toFloatArray()
    // Correction: Utiliser le constructeur Mat4(FloatArray)
    val mat4 = Mat4(matrixArray)

    // Position
    val position = Float3(mat4[3].x, mat4[3].y, mat4[3].z)

    // Scale - Correction: Utiliser la fonction length importée
    val scaleX = length(mat4[0].xyz)
    val scaleY = length(mat4[1].xyz)
    val scaleZ = length(mat4[2].xyz)
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ
    val scale = Float3(safeScaleX, safeScaleY, safeScaleZ)

    // Rotation
    // Correction: Créer Mat4 correctement
    val rotMat = Mat4(
        // Normaliser les colonnes de la matrice originale
        Float4(mat4[0].xyz / safeScaleX, 0f), // Créer Float4 à partir de Float3 et w=0
        Float4(mat4[1].xyz / safeScaleY, 0f),
        Float4(mat4[2].xyz / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f) // Colonne W
    )
    // Correction: Utiliser le constructeur Quaternion(Mat4)
    val quaternion = Quaternion(rotMat)

    // Correction: Utiliser la fonction normalize importée
    return Triple(position, normalize(quaternion), scale)
} 