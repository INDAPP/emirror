package it.droidcon.emirror;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import org.udoo.udooandroidserial.OnResult;
import org.udoo.udooandroidserial.UdooASManager;

public class BootActivity extends AppCompatActivity {

    private View mDecorView;
    private UdooASManager mArduinoManager;
    private final String TAG = this.getClass().getSimpleName();
    private RelativeLayout rootLayout;
    private WebView mWebView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mDecorView = getWindow().getDecorView();

        setContentView(R.layout.activity_boot);
        rootLayout = (RelativeLayout) findViewById(R.id.root_layout);

        mWebView = (WebView) findViewById(R.id.video_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();

        UdooASManager.Open(new UdooASManager.IReadyManager() {
            @Override
            public void onReadyASManager(UdooASManager arduinoManager) {
                mArduinoManager = arduinoManager;

                arduinoManager.setPinMode(52, UdooASManager.DIGITAL_MODE.INPUT);
                arduinoManager.subscribeDigitalRead(52, 200, new OnResult<Integer>() {
                    @Override
                    public void onSuccess(final Integer o) {
                        Log.d(TAG, "Successfully read PIR with value " + o);
                        EmotionLedService.startActionLed(BootActivity.this, "no");
                        startActivity(new Intent(BootActivity.this, MirrorActivity.class));


                        rootLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                if (o == 1)
                                    rootLayout.setBackgroundColor(0x44008800);
                                else
                                    rootLayout.setBackgroundColor(0xff888888);

                            }
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d(TAG, "Error while reading PIR sensor");

                    }
                });
            }
        });
    }

    @Override
    protected void onStop() {
        showSystemUI();
        if (mArduinoManager != null)
            mArduinoManager.unsubscribeDigitalRead(52);
        super.onStop();
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
