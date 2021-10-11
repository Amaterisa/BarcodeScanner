package com.amaterisa.barcodescanner

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.jar.Manifest

typealias BarcodeListener = (barcode: String) -> Unit

class BarcodeScannerActivity : AppCompatActivity() {

    companion object{
        const val TAG = "BarcodeScannerActivity"
        const val RC_CAMERA = 1003
    }

    private var processingBarcode = AtomicBoolean(false)
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA)
                , RC_CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        processingBarcode.set(false)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(
                    camera_preview.surfaceProvider
                )
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        if (processingBarcode.compareAndSet(false, true)) {
                            searchBarcode(barcode)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e("PreviewUseCase", "Binding failed! :(", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (grantResults.isNotEmpty()){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                when (requestCode){
                    RC_CAMERA -> {
                        startCamera()
                    }
                }
            } else{
                when (requestCode){
                    RC_CAMERA -> {
                        finish()
                    }
                }
            }
        }
    }

    private fun searchBarcode(barcode: String) {
        Log.i(TAG, barcode)
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}