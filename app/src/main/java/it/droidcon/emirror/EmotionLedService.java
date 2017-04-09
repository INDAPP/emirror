package it.droidcon.emirror;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.udoo.udooandroidserial.UdooASManager;

import java.util.ArrayList;


public class EmotionLedService extends IntentService {
    private static final String ACTION_LED = "it.droidcon.emirror.action.LED";


    private static final String EXTRA_EMOTIONS = "it.droidcon.emirror.extra.EMOTIONS";
    private UdooASManager mArduinoManager;
    private String TAG = getClass().getSimpleName();

    public EmotionLedService() {
        super("EmotionLedService");
    }


    public static void startActionLed(Context context, String param1) {
        Intent intent = new Intent(context, EmotionLedService.class);
        intent.setAction(ACTION_LED);
        intent.putExtra(EXTRA_EMOTIONS, param1);
        context.startService(intent);
    }

    private ArrayList<Color> colorArray;


    @Override
    protected void onHandleIntent(Intent intent) {

        colorArray = new ArrayList<>();


        colorArray.add(new Color(255, 0, 0, "neutral"));
        colorArray.add(new Color(255, 255, 0, "disgust"));
        colorArray.add(new Color(255, 192, 0, "sad"));
        colorArray.add(new Color(112, 173, 71, "anger"));
        colorArray.add(new Color(0, 176, 71, "contempt"));
        colorArray.add(new Color(0, 0, 255, "happiness"));
        colorArray.add(new Color(255, 102, 255, "fear"));
        colorArray.add(new Color(153, 0, 204, "surprise"));
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LED.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_EMOTIONS);
                handleActionFoo(param1);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(final String emotions) {
        UdooASManager.Open(new UdooASManager.IReadyManager() {
            @Override
            public void onReadyASManager(UdooASManager arduinoManager) {
                mArduinoManager = arduinoManager;

                arduinoManager.setPinMode(50, UdooASManager.DIGITAL_MODE.OUTPUT);
                arduinoManager.setPinMode(48, UdooASManager.DIGITAL_MODE.OUTPUT);
                arduinoManager.setPinMode(46, UdooASManager.DIGITAL_MODE.OUTPUT);
                Color color = null;

                for (Color c : colorArray) {
                    if (c.emotio.equals(emotions)) {
                        color = c;

                        arduinoManager.digitalWrite(50, color.B);
                        arduinoManager.digitalWrite(48, color.R);
                        arduinoManager.digitalWrite(46, color.G);
                        break;
                    }
                }

                if (color == null) {
                    arduinoManager.digitalWrite(50, 0);
                    arduinoManager.digitalWrite(48, 0);
                    arduinoManager.digitalWrite(46, 0);

                }

            }
        });
    }

    class Color {
        public String emotio;
        public int R, G, B;

        public Color(int R, int G, int B, String emotion) {
            this.R = R;
            this.G = G;
            this.B = B;
            this.emotio = emotion;
        }
    }

}
