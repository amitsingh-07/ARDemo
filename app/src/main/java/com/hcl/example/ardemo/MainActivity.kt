package com.hcl.example.ardemo

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var anchorNode: AnchorNode? = null
    private lateinit var depthSettings: DepthSettings
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = supportFragmentManager.findFragmentById(R.id.fragment) as ArFragment

        // Initialize the DepthSettings object
        depthSettings = DepthSettings()
        depthSettings.depthMode = DepthSettings.DepthMode.AUTOMATIC

        // Touch listener to detect when a user touches the ArScene plane to place a model
        arFragment.setOnTapArPlaneListener { hitResult, _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                previousX = motionEvent.x
                previousY = motionEvent.y
            }
            setModelOnUi(hitResult, motionEvent)
        }
    }

    private fun setModelOnUi(hitResult: HitResult, motionEvent: MotionEvent) {
        val trackable = hitResult.trackable
        if (trackable is Plane) {
            if (anchorNode == null) {
                loadModel(R.raw.model) { modelRenderable ->
                    // Create an AnchorNode at the tap point
                    val anchor = hitResult.createAnchor()
                    // Create a TransformableNode and set its renderable to the model
                    anchorNode = AnchorNode(anchor)
                    val transformableNode = TransformableNode(arFragment.transformationSystem).apply {
                        setParent(anchorNode)
                        renderable = modelRenderable
                        select()
                    }
                    // Add the Anchor Node to the scene
                    arFragment.arSceneView.scene.addChild(anchorNode)

                    // Set the onTouchListener to the TransformableNode so that we can drag the object
                    transformableNode.setOnTouchListener { _, motionEvent ->
                        val currentX = motionEvent.x
                        val currentY = motionEvent.y

                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                previousX = currentX
                                previousY = currentY
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val deltaX = currentX - previousX
                                val deltaY = currentY - previousY

                                // Calculate the translation vector based on the touch movement
                                val translation = calculateTranslationVector(deltaX, deltaY)

                                // Apply the translation vector to the TransformableNode
                                val newPosition = Vector3.add(transformableNode.localPosition, translation)
                                transformableNode.localPosition = newPosition
                                previousX = currentX
                                previousY = currentY
                            }
                        }
                        true
                    }
                }
            } else {
                // If the anchorNode already exists, remove it from the scene
                if (anchorNode != null) {
                    arFragment.arSceneView.scene.removeChild(anchorNode)
                    anchorNode = null
                }
            }
        }
    }

    private fun calculateTranslationVector(deltaX: Float, deltaY: Float): Vector3 {
        // Adjust the sensitivity of the translation
        val sensitivity = 0.01f
        val translationX = -deltaX * sensitivity
        val translationY = deltaY * sensitivity

        // Calculate the translation vector
        val cameraForward = arFragment.arSceneView.scene.camera.forward
        val translationVector = Vector3.add(cameraForward.scaled(translationY), arFragment.arSceneView.scene.camera.right.scaled(translationX))

        // Return the translation vector
        return translationVector
    }

    private fun loadModel(model: Int, callback: (ModelRenderable) -> Unit) {
        ModelRenderable.builder()
            .setSource(this, model)
            .build()
            .thenAccept(callback)
    }
}