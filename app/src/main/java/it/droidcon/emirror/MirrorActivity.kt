package it.droidcon.emirror

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.polidea.rxandroidble.RxBleClient
import com.ragnarok.rxcamera.RxCamera
import com.ragnarok.rxcamera.RxCameraData
import com.ragnarok.rxcamera.config.RxCameraConfig
import com.ragnarok.rxcamera.request.Func
import com.tbruyelle.rxpermissions.RxPermissions
import it.droidcon.emirror.model.Entry
import kotlinx.android.synthetic.main.activity_mirror.*
import okhttp3.MediaType
import okhttp3.RequestBody
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.ByteArrayOutputStream

class MirrorActivity : AppCompatActivity() {
    val TAG = "MirrorActivity"

    lateinit var cameraConfig: RxCameraConfig
    lateinit var cameraObservable: Observable<RxCamera>
    var cameraSubscription: Subscription? = null
    var cameraRequestSubscription: Subscription? = null
    var camera: RxCamera? = null
        set(value) {
            field = value
//            if (value != null) {
//                cameraRequestSubscription = value.request()
//                        .periodicDataRequest(10000)
//                        .subscribe(this::onCameraData)
//            } else {
//                cameraRequestSubscription?.unsubscribe()
//                cameraRequestSubscription = null
//            }
            shotHandler.postDelayed(shooter, 10000)
        }

    val closingHandler = Handler()
    val closing = this::finish

    val shotHandler = Handler()
    val shooter = this::takePicture

    lateinit var bleClient: RxBleClient

    var processing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mirror)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN

        cameraConfig = RxCameraConfig.Builder()
                .setHandleSurfaceEvent(true)
                .useBackCamera()
                //.setAutoFocus(true)
                .setPreferPreviewFrameRate(15,30)
                .setPreferPreviewSize(Point(1920,1080), false)
                .build()

        cameraObservable = RxCamera.open(this, cameraConfig)
                .flatMap { it.bindTexture(textureView) }
                .flatMap { it.startPreview() }

        handleFinish()

        bleClient = RxBleClient.create(this)
    }

    override fun onResume() {
        super.onResume()
        RxPermissions(this).request(Manifest.permission.CAMERA).subscribe(this::onPermissionUpdate)
    }

    override fun onPause() {
        cameraSubscription?.unsubscribe()
        cameraRequestSubscription?.unsubscribe()
        cameraSubscription = null
        cameraRequestSubscription = null
        camera = null
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera?.closeCamera()
    }

    fun onPermissionUpdate(granted: Boolean) {
        if (granted) {
            cameraSubscription = cameraObservable.subscribe { camera = it }
        }
    }

    fun handleFinish() {
        closingHandler.removeCallbacks(closing)
        closingHandler.postDelayed(closing, 2 * 60 * 1000)
    }

    fun takePicture() {
        if (camera != null) {
            val request = camera?.request()
            if (request != null) {
                request.takePictureRequest(true, Func {  }).subscribe(this::onCameraData)
            }
            shotHandler.postDelayed(shooter, 10000)
        }
    }

    fun onCameraData(data: RxCameraData) {
        Log.d(TAG, "New camera data")
        if (!processing) {

            val outputStream = ByteArrayOutputStream()
            val bitmap = BitmapFactory.decodeByteArray(data.cameraData, 0, data.cameraData.size)


            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val requestFile = RequestBody.create(
                        MediaType.parse("application/octet-stream"), outputStream.toByteArray())

                Client.recognize(requestFile)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onRecognitionSuccess,
                                this::onRecognitionError)

                processing = true
            }
        }
    }

    fun onRecognitionSuccess(entries: List<Entry>) {
        Log.i(TAG, entries.toString())
        handleFinish()
        if (entries.isNotEmpty()) {
            //TODO: fai qualcosa
            Toast.makeText(this, "Recognition success!!", Toast.LENGTH_LONG).show()
        }
        processing = false

        Handler().postDelayed({
            val request = camera?.request()
            if (request != null) {
                cameraRequestSubscription = request
                        .periodicDataRequest(10000)
                        .subscribe(this::onCameraData)
            }
        }, 30 * 1000)
    }

    fun onRecognitionError(error: Throwable) {
        Log.e(TAG, "Recognition error: ${error.localizedMessage}")
        processing = false
    }

}
