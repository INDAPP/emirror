package it.droidcon.emirror

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.ragnarok.rxcamera.RxCamera
import com.ragnarok.rxcamera.RxCameraData
import com.ragnarok.rxcamera.config.RxCameraConfig
import com.ragnarok.rxcamera.request.Func
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import it.droidcon.emirror.model.Entry
import kotlinx.android.synthetic.main.activity_mirror.*
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream

class MirrorActivity : AppCompatActivity() {
    val TAG = "MirrorActivity"

    lateinit var cameraConfig: RxCameraConfig
    lateinit var cameraObservable: Observable<RxCamera>
    var cameraSubscription: Disposable? = null
    var cameraRequestSubscription: Disposable? = null
    var camera: RxCamera? = null
        set(value) {
            field = value
            if (value != null) {
                cameraRequestSubscription = value.request()
                        .faceDetectionRequest()
                        //.periodicDataRequest(10000)
                        .subscribe(this::onCameraData)
            } else {
                cameraRequestSubscription?.dispose()
                cameraRequestSubscription = null
            }
        }


    var processing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mirror)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN

        cameraConfig = RxCameraConfig.Builder()
                .setHandleSurfaceEvent(true)
                .useBackCamera()
                .setAutoFocus(true)
                .setPreferPreviewFrameRate(15,30)
                .setPreferPreviewSize(Point(1920,1080), false)
                .build()
        cameraObservable = RxCamera.open(this, cameraConfig)
                .flatMap { it.bindTexture(textureView) }
                .flatMap { it.startPreview() }

        textureView.setOnClickListener { takePicture() }
    }

    override fun onResume() {
        super.onResume()
        cameraSubscription = cameraObservable.subscribe { camera = it }
    }

    override fun onPause() {
        cameraSubscription?.dispose()
        cameraRequestSubscription?.dispose()
        cameraSubscription = null
        cameraRequestSubscription = null
        camera = null
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera?.closeCamera()
    }

    fun takePicture() {
        if (camera != null) {
            val request = camera?.request()
            if (request != null) {
                request.takePictureRequest(true, Func {  }).subscribe(this::onCameraData)
            }
        }
    }

    fun onCameraData(data: RxCameraData) {
        Log.d(TAG, "New camera data")
        if (!processing) {
            processing = true
            val outputStream = ByteArrayOutputStream()
            val image = BitmapFactory.decodeByteArray(data.cameraData, 0, data.cameraData.size)
            if (image != null) {
                image.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

                val requestFile = RequestBody.create(MediaType.parse("application/octet-stream"),
                        outputStream.toByteArray())

                Client.recognize(requestFile)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onRecognitionSuccess,
                                this::onRecognitionError)

            } else {
                Log.e(TAG, "Empty image after decode")
                processing = false
            }
        }
    }

    fun onRecognitionSuccess(entries: List<Entry>) {
        Log.i(TAG, entries.toString())
        processing = false
    }

    fun onRecognitionError(error: Throwable) {
        Log.e(TAG, "Recognition error: ${error.localizedMessage}")
        processing = false
    }
}
