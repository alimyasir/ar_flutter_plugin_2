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
// --- Fin Imports ---

import kotlin.math.sqrt


// --- Mise à jour de l'ancienne fonction pour utiliser les nouveaux types ---
// Attention : la conversion de Mat4 vers Quaternion ici est simpliste et peut être moins robuste que celle de deserializeMatrixComponentsFromList
fun deserializeMatrix4(transform: List<Double>): Pair<Float3, Quaternion> {
     if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }
    val m = transform.map { it.toFloat() }.toFloatArray() // Renommé pour clarté

    // Correction: Construire Mat4 colonne par colonne (column-major)
    val mat4 = Mat4(
        x = Float4(m[0], m[1], m[2], m[3]),   // Colonne 0
        y = Float4(m[4], m[5], m[6], m[7]),   // Colonne 1
        z = Float4(m[8], m[9], m[10], m[11]), // Colonne 2
        w = Float4(m[12], m[13], m[14], m[15]) // Colonne 3 (Translation)
    )

    // Position
    val position = Float3(mat4[3].x, mat4[3].y, mat4[3].z) // mat4.w.xyz

    // Rotation (simplifié)
    val scaleX = length(mat4[0].xyz) // mat4.x.xyz
    val scaleY = length(mat4[1].xyz) // mat4.y.xyz
    val scaleZ = length(mat4[2].xyz) // mat4.z.xyz
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ

    // Correction: Construire la matrice de rotation pure Mat4
    val rotMat = Mat4(
        Float4(mat4[0].xyz / safeScaleX, 0f),
        Float4(mat4[1].xyz / safeScaleY, 0f),
        Float4(mat4[2].xyz / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f) // Colonne W identité
    )
    // Correction: Utiliser le constructeur Quaternion(Mat4)
    val rotationQuaternion: Quaternion = Quaternion(rotMat)

    // Correction : Normaliser le Quaternion
    return Pair(position, normalize(rotationQuaternion))
}

// --- Fonction principale de désérialisation ---
fun deserializeMatrixComponentsFromList(transform: List<Double>): Triple<Float3, Quaternion, Float3> {
    if (transform.size != 16) {
        throw IllegalArgumentException("Transformation list must have 16 elements.")
    }

    val m = transform.map { it.toFloat() }.toFloatArray()

    // Correction: Construire Mat4 colonne par colonne
    val mat4 = Mat4(
        x = Float4(m[0], m[1], m[2], m[3]),
        y = Float4(m[4], m[5], m[6], m[7]),
        z = Float4(m[8], m[9], m[10], m[11]),
        w = Float4(m[12], m[13], m[14], m[15])
    )

    // Position
    val position = Float3(mat4[3].x, mat4[3].y, mat4[3].z) // mat4.w.xyz

    // Scale
    val scaleX = length(mat4[0].xyz) // mat4.x.xyz
    val scaleY = length(mat4[1].xyz) // mat4.y.xyz
    val scaleZ = length(mat4[2].xyz) // mat4.z.xyz
    val safeScaleX = if (scaleX == 0f) 1f else scaleX
    val safeScaleY = if (scaleY == 0f) 1f else scaleY
    val safeScaleZ = if (scaleZ == 0f) 1f else scaleZ
    val scale = Float3(safeScaleX, safeScaleY, safeScaleZ)

    // Rotation
    // Correction: Construire la matrice de rotation pure Mat4
    val rotMat = Mat4(
        Float4(mat4[0].xyz / safeScaleX, 0f),
        Float4(mat4[1].xyz / safeScaleY, 0f),
        Float4(mat4[2].xyz / safeScaleZ, 0f),
        Float4(0f, 0f, 0f, 1f) // Colonne W identité
    )
    // Correction: Utiliser le constructeur Quaternion(Mat4) et typer explicitement
    val quaternion: Quaternion = Quaternion(rotMat)

    // Correction : Normaliser le Quaternion
    return Triple(position, normalize(quaternion), scale)
} 