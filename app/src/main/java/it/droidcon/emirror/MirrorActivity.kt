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
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleScanResult
import com.ragnarok.rxcamera.RxCamera
import com.ragnarok.rxcamera.RxCameraData
import com.ragnarok.rxcamera.config.RxCameraConfig
import com.ragnarok.rxcamera.request.Func
import com.spotify.sdk.android.player.*
import com.tbruyelle.rxpermissions.RxPermissions
import it.droidcon.emirror.model.AuthCode
import it.droidcon.emirror.model.Entry
import it.droidcon.emirror.model.Scores
import it.droidcon.emirror.model.Track
import kotlinx.android.synthetic.main.activity_mirror.*
import okhttp3.MediaType
import okhttp3.RequestBody
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.io.ByteArrayOutputStream
import java.util.*

class MirrorActivity : AppCompatActivity(), Player.NotificationCallback, ConnectionStateCallback, SpotifyPlayer.InitializationObserver, Player.OperationCallback {
    val TAG = "MirrorActivity"
    val uuid = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    val spotifyUuid = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fc")

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
    var bleSubscription: Subscription? = null

    val closingHandler = Handler()
    val closing = this::finish

    val shotHandler = Handler()
    val shooter = this::takePicture

    lateinit var bleClient: RxBleClient
    val disconnectTriggerSubject = PublishSubject.create<Void>()

    var processing: Boolean = false

    var emotions: Scores? = null
        set(value) {
            field = value
            EmotionLedService.startActionLed(this, "happiness")
            startRequest()
        }
    var spotifySession: String? = null
        set(value) {
            field = value
            startRequest()
        }

    var spotifyPlayer: SpotifyPlayer? = null
    var playlist: List<Track>? = null
        set(value) {
            field = value
            startMusic()
        }

//    val socket = IO.socket("http://localhost")

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

        //handleFinish()

        bleClient = RxBleClient.create(this)
    }

    override fun onResume() {
        super.onResume()
        RxPermissions(this).request(Manifest.permission.CAMERA).subscribe(this::onPermissionCamera)
        RxPermissions(this).request(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION).subscribe(this::onPermissionLocation)
    }

    override fun onPause() {
        cameraSubscription?.unsubscribe()
        cameraRequestSubscription?.unsubscribe()
        bleSubscription?.unsubscribe()
        cameraSubscription = null
        cameraRequestSubscription = null
        bleSubscription = null
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera?.closeCamera()
        Spotify.destroyPlayer(this)
        camera = null
    }

    fun onPermissionCamera(granted: Boolean) {
        if (granted) {
            cameraSubscription = cameraObservable.subscribe { camera = it }
        }
    }

    fun onPermissionLocation(granted: Boolean) {
        if (granted) {
            bleSubscription = bleClient.scanBleDevices(uuid)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onBleScanResult,
                            this::onBleError)
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

    fun startRequest() {
        val emotions = this.emotions
        val spotifySession = this.spotifySession
        if (emotions != null && spotifySession != null) {
            Client.emotions(spotifySession, emotions)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onEmirrorData,
                            this::onEmirrorError)
            Client.code(spotifySession)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::setupPlayer,
                            this::onEmirrorError)
        }
    }

    fun startMusic() {
        val spotifyPlayer = this.spotifyPlayer
        val playlist = this.playlist
        if (spotifyPlayer != null && playlist != null && playlist.isNotEmpty()) {
            playlist.forEach { spotifyPlayer.queue(this, it.uri) }
        }
    }

    fun onEmirrorData(playlist: List<Track>) {
        this.playlist = playlist
    }

    fun onEmirrorError(error: Throwable) {
        Log.e(TAG, "Fetch from eMirror error: $error")
    }

    fun onCameraData(data: RxCameraData) {
        Log.d(TAG, "New camera data")
        if (!processing) {

            val outputStream = ByteArrayOutputStream()
            val bitmap = BitmapFactory.decodeByteArray(data.cameraData, 0, data.cameraData.size)


            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val bytes = outputStream.toByteArray()

                if (bytes.isNotEmpty()) {
                    val requestFile = RequestBody.create(
                            MediaType.parse("application/octet-stream"), bytes)

                    Client.recognize(requestFile)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onRecognitionSuccess,
                                    this::onRecognitionError)

                    processing = true
                }

            }
        }
    }

    fun onRecognitionSuccess(entries: List<Entry>) {
        Log.i(TAG, entries.toString())
        //handleFinish()
        if (entries.isNotEmpty()) {
            Toast.makeText(this, "Recognition success!!", Toast.LENGTH_LONG).show()
            emotions = entries.maxBy { it.faceRectangle.height * it.faceRectangle.width }?.scores
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
        Log.e(TAG, "Recognition error: $error")
        processing = false
    }

    fun onBleScanResult(result: RxBleScanResult) {
        if (result.bleDevice.connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
            result.bleDevice.establishConnection(false)
                    .takeUntil(disconnectTriggerSubject)
                    .flatMap { it.readCharacteristic(spotifyUuid) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onCharacteristicRead, this::onBleError)
        }
        Log.i(TAG, "BLE new device: $result")
    }

    fun onCharacteristicRead(value: ByteArray) {
        val session = String(value)
        Log.e(TAG, "BLE value read: $session")
        spotifySession = session
        disconnectTriggerSubject.onNext(null)
    }

    fun onBleError(error: Throwable) {
        Log.e(TAG, "BLE error: $error")
    }

    fun setupPlayer(token: AuthCode) {
        val playerConfig = Config(this, token.authCode, "5525970cf64f489383e7789cecf1aff1")
        Spotify.getPlayer(playerConfig, this, this)
    }

    override fun onError(p0: Error?) {

    }

    override fun onSuccess() {

    }

    override fun onInitialized(player: SpotifyPlayer?) {
        spotifyPlayer = player
        spotifyPlayer?.addConnectionStateCallback(this)
        spotifyPlayer?.addNotificationCallback(this)
    }

    override fun onError(error: Throwable?) {
        Log.e(TAG, "Spotify error: $error")
    }

    override fun onPlaybackEvent(p0: PlayerEvent?) {

    }

    override fun onPlaybackError(p0: Error?) {

    }

    override fun onLoggedOut() {

    }

    override fun onLoggedIn() {
        startMusic()
    }

    override fun onConnectionMessage(p0: String?) {

    }

    override fun onLoginFailed(p0: Error?) {

    }

    override fun onTemporaryError() {

    }


}
