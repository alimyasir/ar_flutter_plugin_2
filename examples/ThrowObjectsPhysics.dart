// Automatic FlutterFlow imports
import '/flutter_flow/flutter_flow_theme.dart';
import '/flutter_flow/flutter_flow_util.dart';
import '/custom_code/widgets/index.dart'; // Imports other custom widgets
import '/flutter_flow/custom_functions.dart'; // Imports custom functions
import 'package:flutter/material.dart';
// Begin custom widget code
// DO NOT REMOVE OR MODIFY THE CODE ABOVE!

//AR Flutter Plugin Imports
import 'package:ar_flutter_plugin_2/ar_flutter_plugin.dart';
import 'package:ar_flutter_plugin_2/datatypes/config_planedetection.dart';
import 'package:ar_flutter_plugin_2/datatypes/node_types.dart';
import 'package:ar_flutter_plugin_2/datatypes/hittest_result_types.dart';
import 'package:ar_flutter_plugin_2/managers/ar_anchor_manager.dart';
import 'package:ar_flutter_plugin_2/managers/ar_location_manager.dart';
import 'package:ar_flutter_plugin_2/managers/ar_object_manager.dart';
import 'package:ar_flutter_plugin_2/managers/ar_session_manager.dart';
import 'package:ar_flutter_plugin_2/models/ar_anchor.dart';
import 'package:ar_flutter_plugin_2/models/ar_hittest_result.dart';
import 'package:ar_flutter_plugin_2/models/ar_node.dart';

// Other Custom Imports
import 'package:vector_math/vector_math_64.dart' hide Colors;
import 'dart:math' as math;

class ThrowObjectsPhysics extends StatefulWidget {
  const ThrowObjectsPhysics({
    Key? key,
    this.width,
    this.height,
  }) : super(key: key);

  final double? width;
  final double? height;

  @override
  State<ThrowObjectsPhysics> createState() => _ThrowObjectsPhysicsState();
}

class _ThrowObjectsPhysicsState extends State<ThrowObjectsPhysics> {
  ARSessionManager? arSessionManager;
  ARObjectManager? arObjectManager;
  ARAnchorManager? arAnchorManager;

  // Map to keep track of nodes added to the scene by name
  final Map<String, ARNode> _sceneNodes = {};
  // Map to keep track of targets by name
  final Map<String, ARNode> _targetNodes = {};

  bool _projectileInFlight =
      false; // Track if a projectile is currently launched
  int _score = 0;

  // Use local assets (ensure these exist in your FlutterFlow assets)
  final String _targetModelPath =
      "assets/models/target.gltf"; // Ensure this path is correct
  final String _projectileModelPath =
      "assets/models/projectile.gltf"; // Ensure this path is correct
  final String _planeTexturePath =
      "assets/images/triangle.png"; // Ensure this path is correct

  @override
  void dispose() {
    super.dispose();
    // Clean up nodes from the scene when the widget is disposed
    _clearAllNodes();
    arSessionManager?.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Use FlutterFlow theme if available, otherwise default Theme
    final ffTheme = FlutterFlowTheme.of(context);

    return Scaffold(
      // Use AppBar if desired, otherwise remove
      // appBar: AppBar(
      //   title: const Text('Throw Objects Physics'),
      // ),
      body: SizedBox(
        width: widget.width,
        height: widget.height,
        child: Stack(
          children: [
            ARView(
              onARViewCreated: onARViewCreated,
              planeDetectionConfig: PlaneDetectionConfig.horizontalAndVertical,
            ),
            Align(
              alignment: Alignment.topLeft,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Text(
                  "Score: $_score",
                  style: ffTheme.headlineMedium.override(
                    fontFamily: ffTheme.headlineMediumFamily,
                    color: Colors.white, // Ensure text is visible
                    // useGoogleFonts: false, // Check if this needs adjustment based on your FF setup
                  ),
                ),
              ),
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    ElevatedButton(
                      // Disable button if a projectile is already in flight
                      onPressed: _projectileInFlight ? null : _launchProjectile,
                      child: const Text('LAUNCH'),
                      style: ElevatedButton.styleFrom(
                        foregroundColor: Colors.white,
                        backgroundColor: ffTheme.primary,
                        textStyle: ffTheme.bodyMedium,
                        padding:
                            EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                      ),
                    ),
                    ElevatedButton(
                      onPressed: _addRandomTarget,
                      child: const Text('ADD TARGET'),
                      style: ElevatedButton.styleFrom(
                        foregroundColor: Colors.white,
                        backgroundColor: ffTheme.secondary,
                        textStyle: ffTheme.bodyMedium,
                        padding:
                            EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                      ),
                    ),
                    ElevatedButton(
                      onPressed: _clearAllNodes,
                      child: const Text('CLEAR ALL'),
                      style: ElevatedButton.styleFrom(
                        foregroundColor: Colors.white,
                        backgroundColor: Colors.redAccent,
                        textStyle: ffTheme.bodyMedium,
                        padding:
                            EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void onARViewCreated(
      ARSessionManager sessionManager,
      ARObjectManager objectManager,
      ARAnchorManager anchorManager,
      ARLocationManager locationManager) {
    arSessionManager = sessionManager;
    arObjectManager = objectManager;
    arAnchorManager = anchorManager;

    arSessionManager!.onInitialize(
      showFeaturePoints: false,
      showPlanes: true, // Needed to place targets or visualize environment
      customPlaneTexturePath: _planeTexturePath,
      showWorldOrigin: false,
      handleTaps:
          true, // Allow taps for interactions (e.g., placing targets manually if needed)
    );
    arObjectManager!.onInitialize();

    // Optional: Handle taps on planes to place targets manually
    arSessionManager!.onPlaneOrPointTap = _onPlaneTapHandler;

    // Assign the physics collision handler
    arObjectManager!.onPhysicsNodeCollision = _onProjectileCollision;

    // Add an initial target for testing
    _addDefaultTarget();

    // Provide feedback
    _showSnackBar("AR View Initialized. Tap a plane or 'ADD TARGET'.");
  }

  // --- Target Placement ---

  Future<void> _addDefaultTarget() async {
    // Wait a bit for AR session to stabilize
    await Future.delayed(const Duration(seconds: 3));
    // Add a target 2 meters in front of the initial camera position
    final cameraPose = await arSessionManager?.getCameraPose();
    if (cameraPose == null) return;

    final position = cameraPose.getTranslation() +
        (MatrixUtils.getForward(cameraPose) * -2.0); // Forward is -Z
    _addTargetAtPosition(position);
  }

  Future<void> _addRandomTarget() async {
    final cameraPose = await arSessionManager?.getCameraPose();
    if (cameraPose == null) {
      _showSnackBar("Cannot get camera position.");
      return;
    }
    // Place target randomly within a box in front of the camera
    final camPos = cameraPose.getTranslation();
    final camForward =
        MatrixUtils.getForward(cameraPose) * -1.0; // Forward direction
    final camRight = MatrixUtils.getRight(cameraPose);

    final rand = math.Random();
    final randomDistance = 2.0 + rand.nextDouble() * 2.0; // 2m to 4m away
    final randomRight =
        (rand.nextDouble() - 0.5) * 2.0; // -1m to +1m right/left
    final randomUp = rand.nextDouble() * 1.0; // 0m to 1m up

    final targetPosition = camPos +
        (camForward * randomDistance) +
        (camRight * randomRight) +
        Vector3(0, randomUp, 0);

    _addTargetAtPosition(targetPosition);
  }

  // Handle taps on detected planes
  void _onPlaneTapHandler(List<ARHitTestResult> hits) async {
    // Find the first hit that is a plane, or fallback to the first non-feature point hit, or just the first hit.
    final hit = hits.firstWhere(
        (hitTestResult) => hitTestResult.type == ARHitTestResultType.plane);

    final position = Vector3(hit.worldTransform.getColumn(3).x,
        hit.worldTransform.getColumn(3).y, hit.worldTransform.getColumn(3).z);

    _addTargetAtPosition(position);
  }

  // Adds a target node at the specified world position
  Future<void> _addTargetAtPosition(Vector3 position) async {
    final nodeName = "target_${DateTime.now().millisecondsSinceEpoch}";
    final targetNode = ARNode(
      // Corrected to use local GLTF2 for local assets
      type: NodeType.localGLTF2,
      uri: _targetModelPath,
      name: nodeName,
      scale: Vector3(0.3, 0.3, 0.3),
      position: position,
      rotation: Vector4(0, 0, 0, 1),
    );

    bool? added = await arObjectManager?.addNode(targetNode);
    if (added ?? false) {
      _sceneNodes[nodeName] = targetNode;
      _targetNodes[nodeName] = targetNode;
      _showSnackBar("Target added: $nodeName");
    } else {
      _showSnackBar("Failed to add target node.");
    }
  }

  // --- Projectile Launching ---

  Future<void> _launchProjectile() async {
    if (arSessionManager == null || arObjectManager == null) {
      _showSnackBar("AR Managers not ready.");
      return;
    }
    if (_projectileInFlight) {
      _showSnackBar("Wait for the current projectile.");
      return;
    }

    final Matrix4? cameraTransform = await arSessionManager!.getCameraPose();
    if (cameraTransform == null) {
      _showSnackBar("Could not get camera pose.");
      return;
    }

    final position = cameraTransform.getTranslation();
    // Use MatrixUtils
    final direction = MatrixUtils.getForward(cameraTransform) * -1.0;

    final nodeName = "projectile_${DateTime.now().millisecondsSinceEpoch}";
    final projectileNode = ARNode(
      // Corrected to use local GLTF2 for local assets
      type: NodeType.localGLTF2,
      uri: _projectileModelPath,
      name: nodeName,
      scale: Vector3(0.1, 0.1, 0.1),
      position: position + (direction * 0.2),
      // Corrected rotation: Convert Quaternion to Vector4
      rotation: Vector4(
          _vectorToQuaternion(direction).x,
          _vectorToQuaternion(direction).y,
          _vectorToQuaternion(direction).z,
          _vectorToQuaternion(direction).w),
    );

    bool? added = await arObjectManager!.addNode(projectileNode);

    if (added ?? false) {
      _sceneNodes[nodeName] = projectileNode;

      final speed = 12.0;
      final initialVelocity = direction * speed;

      await arObjectManager!.startPhysics(projectileNode.name, initialVelocity);

      setState(() {
        _projectileInFlight = true;
      });
      _showSnackBar("Launched ${projectileNode.name}!");
    } else {
      _showSnackBar("Failed to add projectile node.");
    }
  }

  // --- Collision Handling ---

  void _onProjectileCollision(String nodeName, String? collidedWithNodeName) {
    print(
        "Collision Event: Node '$nodeName' collided with '$collidedWithNodeName'");

    if (nodeName.startsWith("projectile_")) {
      // Correctly remove node: Use the object stored in the map
      final nodeToRemove = _sceneNodes.remove(nodeName);
      if (nodeToRemove != null) {
        arObjectManager?.removeNode(nodeToRemove);
      } else {
        // Fallback if somehow not in map (less ideal)
        // Still need type and uri for the constructor to be valid.
        // Using placeholder values - this shouldn't normally happen if tracking is correct.
        print(
            "Warning: Node $nodeName not found in _sceneNodes for removal after collision.");
        // arObjectManager?.removeNode(ARNode(name: nodeName, type: NodeType.fileSystemAppFolderGLB, uri: 'placeholder')); // Avoid this if possible
      }

      if (collidedWithNodeName != null &&
          collidedWithNodeName.startsWith("target_")) {
        setState(() {
          _score++;
        });
        _showSnackBar("🎉 Target Hit! Score: $_score");

        final targetToRemove = _targetNodes.remove(collidedWithNodeName);
        if (targetToRemove != null) {
          _sceneNodes.remove(collidedWithNodeName);
          arObjectManager?.removeNode(targetToRemove);
        }
      } else if (collidedWithNodeName == null) {
        _showSnackBar("💥 Projectile hit environment.");
      } else {
        _showSnackBar("Projectile hit '$collidedWithNodeName'.");
      }

      setState(() {
        _projectileInFlight = false;
      });
    }
  }

  // --- Cleanup ---

  void _clearAllNodes() {
    if (arObjectManager == null) return;
    for (var node in _sceneNodes.values) {
      arObjectManager!.removeNode(node);
    }
    setState(() {
      _sceneNodes.clear();
      _targetNodes.clear();
      _score = 0;
      _projectileInFlight = false;
    });
    _showSnackBar("All nodes cleared.");
  }

  // --- Utility Functions ---

  // Helper to show feedback messages
  void _showSnackBar(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  // Helper to create a quaternion looking in a direction
  // Note: This is a simplified version. For perfect alignment, you might need a more
  // robust method considering an 'up' vector.
  Quaternion _vectorToQuaternion(Vector3 direction, [Vector3? up]) {
    final Matrix4 rotMatrix =
        makeViewMatrix(Vector3.zero(), direction, up ?? Vector3(0, 1, 0));
    // The rotation part of the view matrix needs to be inverted (transposed)
    // to represent the object's rotation that achieves this view.
    return Quaternion.fromRotation(rotMatrix.getRotation().transposed());
  }
}

// Utility class to extract axes from Matrix4 (ensure this is accessible)
// If you have this in custom_functions.dart, you don't need to define it here.
// If not, uncomment this class definition.
class MatrixUtils {
  static Vector3 getForward(Matrix4 matrix) =>
      Vector3(matrix.storage[8], matrix.storage[9], matrix.storage[10])
          .normalized();
  static Vector3 getUp(Matrix4 matrix) =>
      Vector3(matrix.storage[4], matrix.storage[5], matrix.storage[6])
          .normalized();
  static Vector3 getRight(Matrix4 matrix) =>
      Vector3(matrix.storage[0], matrix.storage[1], matrix.storage[2])
          .normalized();
}
