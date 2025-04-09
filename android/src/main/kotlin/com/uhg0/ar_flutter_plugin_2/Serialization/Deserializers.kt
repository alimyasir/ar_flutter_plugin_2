package com.uhg0.ar_flutter_plugin_2.Serialization

// --- Importations SceneView (garder uniquement pour les alias si nécessaire) ---
// import io.github.sceneview.math.Position as ScenePosition // Plus nécessaire pour le retour
// import io.github.sceneview.math.Rotation as SceneRotation // Plus nécessaire
// import io.github.sceneview.math.Scale as SceneScale // Plus nécessaire
// import io.github.sceneview.math.Quaternion as SceneQuaternion // Plus nécessaire
// import io.github.sceneview.math.Transform as SceneTransform // Plus nécessaire
// import io.github.sceneview.math.toQuaternion // Plus nécessaire

// +++ Importations dev.romainguy.kotlin.math +++
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.normalize // Pour normaliser les quaternions si nécessaire
import dev.romainguy.kotlin.math.rotation // Pour extraire la sous-matrice de rotation
import dev.romainguy.kotlin.math.Float4 // Pour la colonne w de Mat4 si nécessaire
// --- Fin Importations ---

import kotlin.math.sqrt

fun deserializeMatrix4(transform: ArrayList<Double>): Pair<Float3, Quaternion> {
    // Position
    val position = Float3(
        transform[12].toFloat(),
        transform[13].toFloat(),
        transform[14].toFloat()
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
        Quaternion(
            x = (m21 - m12) / s,
            y = (m02 - m20) / s,
            z = (m10 - m01) / s
        )
    } else {
        Quaternion()
    }

    return Pair(position, rotation)
}

fun deserializeMatrixComponentsFromList(transform: List<Double>): Triple<Float3, Quaternion, Float3> {
    // Check size just in case
    if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }

    // Convertir List<Double> en FloatArray pour Mat4
    val matrixArray = transform.map { it.toFloat() }.toFloatArray()
    val mat4 = Mat4.fromColumnMajor(matrixArray) // Créer Mat4 à partir de column-major data

    // Position (extraite de la 4ème colonne)
    val position = Float3(mat4[3].x, mat4[3].y, mat4[3].z) // Accès à la 4ème colonne (indice 3)

    // Scale - Calculate scale from matrix columns' length
    val scaleX = length(mat4[0].xyz) // Longueur de la 1ère colonne (vecteur X)
    val scaleY = length(mat4[1].xyz) // Longueur de la 2ème colonne (vecteur Y)
    val scaleZ = length(mat4[2].xyz) // Longueur de la 3ème colonne (vecteur Z)
    // Avoid division by zero
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ
    val scale = Float3(safeScaleX, safeScaleY, safeScaleZ)

    // Rotation - Extraire la sous-matrice 3x3 de rotation et la convertir en Quaternion
    // Créer une Mat4 de rotation pure en normalisant les colonnes
    val rotMat = Mat4(
        Float4(mat4[0].x / safeScaleX, mat4[0].y / safeScaleX, mat4[0].z / safeScaleX, 0f),
        Float4(mat4[1].x / safeScaleY, mat4[1].y / safeScaleY, mat4[1].z / safeScaleY, 0f),
        Float4(mat4[2].x / safeScaleZ, mat4[2].y / safeScaleZ, mat4[2].z / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f) // Colonne de translation mise à zéro
    )
    // Utiliser le constructeur de Quaternion à partir de Mat4
    val quaternion = Quaternion(rotMat) // Crée un quaternion à partir de la matrice 4x4 (qui ne contient que la rotation)

    return Triple(position, normalize(quaternion), scale) // Normaliser le quaternion par sécurité
} 