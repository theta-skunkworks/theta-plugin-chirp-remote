/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.pluginapplication;

import android.os.Bundle;
import android.view.KeyEvent;
import com.theta360.pluginapplication.task.TakePictureTask;
import com.theta360.pluginapplication.task.TakePictureTask.Callback;
import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;

//Chirpを使うために追加
import android.util.Log;
import io.chirp.connect.ChirpConnect;
import io.chirp.connect.interfaces.ConnectEventListener;
import io.chirp.connect.models.ChirpConnectState;
import io.chirp.connect.models.ChirpError;

import android.media.AudioManager;




public class MainActivity extends PluginActivity {

    String CHIRP_APP_KEY = "B85cC9a72E49eC623Aeccd3F7";
    String CHIRP_APP_SECRET = "1b2EacEDad1BCde319ef9BCa459a13E88D48b1f2196dCD8bBA";
    //16kHz-mono
    String CHIRP_APP_CONFIG_16K_MONO = "NAhsEBmQus5tzg1rFR+Z6Pty4Am4Z5m8xXLAYx+4ujUln8+yE4YpH4OkSKnRKX9BEYFPsWqyJG06/FGKaaIF1Zzarw0TBBHGiwliZD7dZGV5i6/ExpnIRMLCj9hKP0ry4GNMlrH1gwumJe6CpHVONLn1XtLvI78PWyQTQtxgsXbVv5mzJJpiknLfw9vsmgMB2sjpiiFuY/TTBFf+fskXFc1ZCOGEpId5Q+Q7oE0zypX411EdB9tx7UqXsjqeXWetmMEjWrCl6O4/CkZtQjfxueBmrTEFyogdLAoRPd10rhrvfFKPs7ALi5q6LUE9CiIwm3Qns6o2F38awYIa/ogaLf6PdU6TUHq4JYakCnjH0bbnBn/aYUCQ79dHsedw+9OjD4NdKBfA6UdzO/Yd4FNzCAXkBEtYn5KKJvTNI5MTGtLTGeEzhxK+pprdc4FLd63lr4XmwmMFHRm6d/u1Q4U2jumDpKSmwZc9JoUiZryt7EDVTdUYkwwxZiHhVkw+5lHUVRZkIRO12/rQaYjIocCVx5OasFl+7g3Wid2eGiUVcTnHOM7vNYnlQrXG1OWznkHc6qQeUessWlmrn/O9SlRvhp502tG5olyCNP6pxhbIwQ1hWV9qtH0F7qQEleLICe7uu3c5wKppipwKNDqSnwJfOTUR4DOzf3wNTkM5VcuBwKYmTsuc5VgqLlVSO9iRRo6ar9fy+PtiQPtSmBlBgkia7pmh0Y/QTVbkwGsnnIsGOL/zOPARsntXoq2AMjXIu2BGG7H7baeBFVOCSVUaQM1zDh/M9gYTMaACShTGtTd5+RDz9qlnpRxcvjdyJbFd1Q8HTKo8rRizeG899pMsDiiqZpFCsZ9DkFsiP4U/fRoCwpk=";
    //超音波
    //String CHIRP_APP_CONFIG_US = "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";


    private static final String TAG = "Chip TEST";

    private static final int RESULT_REQUEST_RECORD_AUDIO = 1;
    private ChirpConnect chirpConnect;
    private Context context;


    private TakePictureTask.Callback mTakePictureTaskCallback = new Callback() {
        @Override
        public void onTakePicture(String fileUrl) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        //---- Chirp ----
        if (CHIRP_APP_KEY.equals("") || CHIRP_APP_SECRET.equals("")) {
            Log.d(TAG, "CHIRP_APP_KEY or CHIRP_APP_SECRET is not set. " +
                    "Please update with your CHIRP_APP_KEY/CHIRP_APP_SECRET from developers.chirp.io");
            return;
        }

        chirpConnect = new ChirpConnect(this, CHIRP_APP_KEY, CHIRP_APP_SECRET);

        Log.d(TAG, "Connect Version: " + chirpConnect.getVersion());

        ChirpError setConfigError = chirpConnect.setConfig(CHIRP_APP_CONFIG_16K_MONO);
        if (setConfigError.getCode() > 0) {
            Log.d(TAG, setConfigError.getMessage());
        }

        //String versionDisplay = chirpConnect.getVersion() + "\n" +
        //        chirpConnect.getProtocolName() + " v" + chirpConnect.getProtocolVersion();
        //versionView.setText(versionDisplay);
        chirpConnect.setListener(connectEventListener);
        //---------------------------------------------------------------------------



        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true);
        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    /*
                     * To take a static picture, use the takePicture method.
                     * You can receive a fileUrl of the static picture in the callback.
                     */
                    new TakePictureTask(mTakePictureTaskCallback).execute();

                    chirpSnedData();

                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                /**
                 * You can control the LED of the camera.
                 * It is possible to change the way of lighting, the cycle of blinking, the color of light emission.
                 * Light emitting color can be changed only LED3.
                 */
                notificationLedBlink(LedTarget.LED3, LedColor.BLUE, 1000);
            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isApConnected()) {

        }

        //---- Chirp ----
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, RESULT_REQUEST_RECORD_AUDIO);
        }
        else {
            // Start ChirpSDK sender and receiver, if no arguments are passed both sender and receiver are started
            ChirpError error = chirpConnect.start(true, true);
            if (error.getCode() > 0) {
                //Log.e("ChirpError: ", error.getMessage())
                Log.d(TAG, "ChirpError:" + error.getMessage();
            } else {
                //Log.v("ChirpSDK: ", "Started ChirpSDK");
                Log.d(TAG, "ChirpSDK: Started ChirpSDK");
            }
        }



    }

    @Override
    protected void onPause() {
        // Do end processing
        //close();

        super.onPause();

        //---- Chirp ----
        chirpConnect.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RESULT_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ChirpError error = chirpConnect.start();
                    if (error.getCode() > 0) {
                        //Log.e("ChirpError: ", error.getMessage())
                        Log.d(TAG, "ChirpError:" + error.getMessage();
                    } else {
                        //Log.v("ChirpSDK: ", "Started ChirpSDK");
                        Log.d(TAG, "ChirpSDK: Started ChirpSDK");
                    }
                }
                return;
            }
        }
    }

    void chirpSnedData(){
        String identifier = "hello";
        byte[] payload = identifier.getBytes(Charset.forName("UTF-8"));

        ChirpError error = chirpConnect.send(payload);
        if (error.getCode() > 0) {
            //Log.e("ChirpError: ", error.getMessage())
            Log.d(TAG, "ChirpError:" + error.getMessage();
        } else {
            //Log.v("ChirpSDK: ", "Sent " + identifier);
            Log.d(TAG, "ChirpSDK: Sent [" + identifier + "]" );
        }
    }



}
