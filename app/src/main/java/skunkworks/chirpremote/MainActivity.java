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

package skunkworks.chirpremote;

import java.util.function.Consumer;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.media.AudioManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginapplication.task.CheckWlanModeTask;
import com.theta360.pluginapplication.task.ShutterButtonTask;
import com.theta360.pluginapplication.task.ChangeExposureDelayTask;
import com.theta360.pluginapplication.task.ChangeCaptureModeTask;
import com.theta360.pluginapplication.task.GetCameraStatusTask;

import io.chirp.chirpsdk.ChirpSDK;
import io.chirp.chirpsdk.models.ChirpError;
import io.chirp.chirpsdk.interfaces.ChirpEventListener;
import io.chirp.chirpsdk.models.ChirpSDKState;


public class MainActivity extends PluginActivity {

    private static final String TAG = "ChirpRemote";

    private Context context;

    private ChirpSDK chirp;

    private static final String CHIRP_APP_KEY = "Please set the credential you have acquired.";
    private static final String CHIRP_APP_SECRET = "Please set the credential you have acquired.";
    //16kHz-mono
    private static final String CHIRP_APP_CONFIG_16K_MONO = "Please set the CHIRP_APP_CONFIG key for the '16kHz-mono' protocol.";
    //ultrasonic
    private static final String CHIRP_APP_CONFIG_US = "Please set the CHIRP_APP_CONFIG key for the 'ultrasonic' protocol.";
    //standard
    private static final String CHIRP_APP_CONFIG_STD = "Please set the CHIRP_APP_CONFIG key for the 'standard' protocol.";

    int chripReplyCnt = 0;
    String chripReplyStr8 = "";
    String chripReplyStr32 = "";


    //WLANモード関連
    private static final int WLAN_MODE_OFF = 0;
    private static final int WLAN_MODE_AP = 1;
    private static final int WLAN_MODE_CL = 2;
    private boolean existCL = false;
    private int wlanMode = WLAN_MODE_OFF;
    private int dispWlanChgCnt = 0;

    //プラグイン起動時のMode長押し後 Upをスルーする用
    private boolean onKeyDownModeButton = false;

    //長押し後のボタン離し認識用
    private boolean onKeyLongPressWlan = false;
    private boolean onKeyLongPressFn = false;

    //表示系クラス（OLED描画クラス継承, LED表示も含む）
    DisplayInfo displayInfo=null;
    //表示スレッド終了用
    private boolean mFinished;

    //独自コマンド解析実行クラス
    Command command=null;


    // <<コマンド実行結果をするタスクの共通コールバック処理>>
    // 設定変更の結果を文字列で受け取る -> Chirpで返信するため
    Consumer<String> consumerAfterCommandExec = string -> commonAfterCommandExec(string);
    void commonAfterCommandExec(String inStr){
        Log.d(TAG, "commonAfterCommandExec() : inStr=" + inStr);

        //Chirpでアンサーバックする設定をする
        if (displayInfo.chirpReply) {
            //chripReplyCnt = 1; //最短
            chripReplyCnt = 4; //400～500msくらい受信データ表示を維持したあと応答
            chripReplyStr8 = inStr;
        }
    }
    // ボタン操作でタスク実行した場合は何もしたくないので、以下のダミールーチンを仕込む
    Consumer<String> consumerAfterButtonExec = string -> commonAfterButtonExec(string);
    void commonAfterButtonExec(String inStr){
        Log.d(TAG, "consumerAfterButtonExec() : inStr=" + inStr);
        //無処理
    }

    //<<タスクを起動せず内部情報を更新するだけの処理>>
    Consumer<Integer> chgTimeShift = integer -> chgTimeShift(integer);
    void chgTimeShift(int setTimeShift) {
        if (setTimeShift==0) {
            displayInfo.timeShift = false;
        } else {
            displayInfo.timeShift = true;
        }
    }
    Consumer<Integer> chgChirpReply = integer -> chgChirpReply(integer);
    void chgChirpReply(int setChirpReply) {
        if (setChirpReply==0) {
            displayInfo.chirpReply = false;
        } else {
            displayInfo.chirpReply = true;
        }
    }

    //<<その他 各タスク固有のコールバック>>
    private CheckWlanModeTask.Callback mCheckWlanModeTaskCallback = new CheckWlanModeTask.Callback() {
        @Override
        public void onCheckWlanMode(String networkType, boolean inExistCL) {
            if ( networkType.equals(CheckWlanModeTask.WLAN_OFF) ) {
                wlanMode = WLAN_MODE_OFF;
            } else if ( networkType.equals(CheckWlanModeTask.WLAN_AP) ) {
                wlanMode = WLAN_MODE_AP;
            } else if ( networkType.equals(CheckWlanModeTask.WLAN_CL) ) {
                wlanMode = WLAN_MODE_CL;
            } else {
                wlanMode = WLAN_MODE_OFF;
            }
            existCL = inExistCL;
        }
    };

    private GetCameraStatusTask.Callback mGetCameraStatusCallback = new GetCameraStatusTask.Callback() {
        @Override
        public void onUpdateCameraStatus( String captureMode,
                                          int exposureDelay,
                                          String strExpProg,
                                          String strAv,
                                          String strTv,
                                          String strIso,
                                          String strExpComp,
                                          String strWb,
                                          String strColorTemperature) {
            displayInfo.captureMode = captureMode;
            displayInfo.exposureDelay = exposureDelay;
            displayInfo.strExpProg = strExpProg;
            displayInfo.strAv = strAv;
            displayInfo.strTv = strTv;
            displayInfo.strIso = strIso;
            displayInfo.strExpComp = strExpComp;
            displayInfo.strWb = strWb;
            displayInfo.strColorTemperature = strColorTemperature;

            chripReplyStr32 = displayInfo.editChirpReply32();

        }
    };

    //==============================================================
    // MainActivity 定型
    //==============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        //コマンド解析&実行クラス初期化 ※DisplayInfoの初期化より先に行うこと
        command = new Command(consumerAfterCommandExec);

        //OLEDディスプレイまわり初期化
        displayInfo = new DisplayInfo(getApplicationContext(), command.expProgApi2Disp, command.ssApi2Cmd, command.wbApi2Cmd);
        displayInfo.brightness(50);     //輝度設定
        displayInfo.clear(displayInfo.black); //表示領域クリア設定
        displayInfo.draw();                     //表示領域クリア結果を反映

        //Chirpライセンス情報有無チェック -> "なし"は終了させる
        if (CHIRP_APP_KEY.equals("") || CHIRP_APP_SECRET.equals("")) {
            Log.d(TAG, "CHIRP_APP_KEY or CHIRP_APP_SECRET is not set. " +
                    "Please update with your CHIRP_APP_KEY/CHIRP_APP_SECRET from developers.chirp.io");
            return;
        }

        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true);
        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {

                switch (keyCode) {
                    case KeyReceiver.KEYCODE_CAMERA :
                        //撮影 : Chirp SDKがAudio使用中のため 動画記録は禁止とする
                        if ( displayInfo.captureMode.equals("image") ) {
                            new ShutterButtonTask(displayInfo.timeShift).execute();
                        }
                        break;
                    case KeyReceiver.KEYCODE_MEDIA_RECORD :
                        //プラグイン起動時のMode長押し後 onKeyUp() を無処理とするための仕掛け
                        onKeyDownModeButton = true;
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {

                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF :
                        if (onKeyLongPressWlan) {
                            onKeyLongPressWlan=false;
                        } else {
                            //WLANモード切り替え
                            switch (wlanMode) {
                                case WLAN_MODE_OFF :
                                    notificationWlanAp();
                                    wlanMode = WLAN_MODE_AP;
                                    break ;
                                case WLAN_MODE_AP :
                                    if (existCL == true) {
                                        notificationWlanCl();
                                        wlanMode = WLAN_MODE_CL;
                                    } else {
                                        notificationWlanOff();
                                        wlanMode = WLAN_MODE_OFF;
                                    }
                                    break ;
                                case WLAN_MODE_CL :
                                    notificationWlanOff();
                                    wlanMode = WLAN_MODE_OFF;
                                    break ;
                            }

                            dispWlanChgCnt=20;//およそ2秒間くらい
                        }

                        break;
                    case KeyReceiver.KEYCODE_MEDIA_RECORD :
                        //プラグイン起動時のMode長押し後 onKeyUp() を無処理とするための仕掛け
                        if (onKeyDownModeButton) {
                            //Time Shift On/Off 切り替え
                            if ( displayInfo.timeShift ) {
                                displayInfo.timeShift = false;
                            } else {
                                displayInfo.timeShift = true;
                            }
                        }
                        onKeyDownModeButton = false;

                        break;
                    case KeyEvent.KEYCODE_FUNCTION :
                        if (onKeyLongPressFn) {
                            onKeyLongPressFn=false;
                        } else {
                            //タイマー On/Off 切り替え
                            new ChangeExposureDelayTask(consumerAfterButtonExec, ChangeExposureDelayTask.EXPOSURE_DELAY_TGGLE).execute();
                        }

                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {

                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF:
                        onKeyLongPressWlan=true;

                        //Chirp Config（周波数）切り替え
                        ChirpError error = chirp.stop();
                        if (error.getCode() > 0) {
                            Log.d(TAG, error.getMessage());
                        }

                        if ( displayInfo.chirpConfig == DisplayInfo.CHIRP_CONFIG_16KHZ_MONO ) {
                            Log.d(TAG, "--- Change Chirp Config [Ultra Sonic] ---");
                            displayInfo.chirpConfig = DisplayInfo.CHIRP_CONFIG_ULTRA_SONICK;
                            initalChirpSdk(displayInfo.chirpConfig);
                        } else if (displayInfo.chirpConfig == DisplayInfo.CHIRP_CONFIG_ULTRA_SONICK) {
                            Log.d(TAG, "--- Change Chirp Config [Standard] ---");
                            displayInfo.chirpConfig = DisplayInfo.CHIRP_CONFIG_STANDARD;
                            initalChirpSdk(displayInfo.chirpConfig);
                        } else if (displayInfo.chirpConfig == DisplayInfo.CHIRP_CONFIG_STANDARD) {
                            Log.d(TAG, "--- Change Chirp Config [16kHz-mono] ---");
                            displayInfo.chirpConfig = DisplayInfo.CHIRP_CONFIG_16KHZ_MONO;
                            initalChirpSdk(displayInfo.chirpConfig);
                        } else {
                            Log.d(TAG, "--- Change Chirp Config [16kHz-mono] ---");
                            displayInfo.chirpConfig = DisplayInfo.CHIRP_CONFIG_16KHZ_MONO;
                            initalChirpSdk(displayInfo.chirpConfig);
                        }

                        error = chirp.start();
                        if (error.getCode() > 0) {
                            Log.d(TAG, error.getMessage());
                        }

                        break;
                    case KeyEvent.KEYCODE_FUNCTION :
                        onKeyLongPressFn=true;

                        //Chirp応答有無切り替え
                        if (displayInfo.chirpReply) {
                            displayInfo.chirpReply = false;
                        } else {
                            displayInfo.chirpReply = true;
                        }

                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // マイクを使うプラグインにおけるTHETA固有の設定。
        // これをしないとChirpライブラリに音を渡せない。
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setParameters("RicUseBFormat=false");

        //前回起動時に保存した情報を読む
        restorePluginInfo();

        //Chirp初期化 (onCreateで行わなくてもよい。restoreの結果を反映したいのでココで実施した)
        initalChirpSdk(displayInfo.chirpConfig);

        // Chirpライブラリの動作開始
        ChirpError error = chirp.start();
        if (error.getCode() > 0) {
            Log.d(TAG, error.getMessage());
        }

        //WLANモード(OFF/AP/CL)を取得
        new CheckWlanModeTask(mCheckWlanModeTaskCallback).execute();

        //静止画モードにする（Chirp受け待ちと動画録画は共存できないため、静止画モードに固定する）
        new ChangeCaptureModeTask(consumerAfterButtonExec, ChangeCaptureModeTask.CAPMODE_IMAGE).execute();

        //スレッド開始
        mFinished = false;
        drawOledThread();
    }

    @Override
    protected void onPause() {
        // Do end processing
        //close();

        //Chirp ライブラリの動作停止
        ChirpError error = chirp.stop();
        if (error.getCode() > 0) {
            Log.d(TAG, error.getMessage());
        }

        //スレッドを終わらせる指示。終了待ちしていません。
        mFinished = true;

        //次回起動時のために必要な情報を保存
        savePluginInfo();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            chirp.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //==============================================================
    // Chirp関連
    //==============================================================
    ChirpEventListener chirpEventListener = new ChirpEventListener() {

        @Override
        public void onSending(byte[] data, int channel) {   //送信中
            /**
             * onSending is called when a send event begins.
             * The data argument contains the payload being sent.
             */
            String sendingString = "null";
            if (data != null) {
                sendingString =  new String(data);
            }
            Log.d(TAG, "ConnectCallback: onSending: " + sendingString + " on channel: " + channel);

            displayInfo.messageCnt=40;
            displayInfo.wavePos=0;
            displayInfo.messageType = DisplayInfo.DIALOG_MSG_SEND;
            if ( displayInfo.chirpConfig == DisplayInfo.CHIRP_CONFIG_16KHZ_MONO ) {
                displayInfo.messageStr = "SendStat";
            } else {
                displayInfo.messageStr = sendingString;
            }

            displayInfo.setLedChirpSending(true);
        }

        @Override
        public void onSent(byte[] data, int channel) {  //送信完了
            /**
             * onSent is called when a send event has completed.
             * The data argument contains the payload that was sent.
             */
            String sentString = "null";
            if (data != null) {
                sentString =  new String(data);
            }
            Log.d(TAG, "ConnectCallback: onSent: " + sentString + " on channel: " + channel);

            //音量を戻す
            chirp.setSystemVolume(displayInfo.restoreVolume);

            displayInfo.messageCnt=0;
            displayInfo.messageType = DisplayInfo.DIALOG_MSG_NORM;
            displayInfo.messageStr = "";

            displayInfo.setLedChirpSending(false);
        }

        @Override
        public void onReceiving(int channel) {  //受信中
            /**
             * onReceiving is called when a receive event begins.
             * No data has yet been received.
             */
            Log.d(TAG, "ConnectCallback: onReceiving on channel: " + channel);

            displayInfo.messageCnt=30;
            displayInfo.wavePos=0;
            displayInfo.messageType = DisplayInfo.DIALOG_MSG_RCV;
            displayInfo.messageStr="";

            displayInfo.setLedChirpReceiving(true);
        }

        @Override
        public void onReceived(byte[] data, int channel) {  //受信完了
            /**
             * onReceived is called when a receive event has completed.
             * If the payload was decoded successfully, it is passed in data.
             * Otherwise, data is null.
             */
            String receivedString = "null";
            if (data != null) {
                receivedString =  new String(data);
            }
            Log.d(TAG, "ConnectCallback: onReceived: " + receivedString + " on channel: " + channel);

            displayInfo.messageCnt=10;
            displayInfo.wavePos=0;
            displayInfo.messageType = DisplayInfo.DIALOG_MSG_NORM;
            if (receivedString.length()>8) {
                displayInfo.messageStr = "\"" + receivedString.substring(0, 8) + "\"+";
            } else {
                displayInfo.messageStr = "\"" + receivedString + "\"";
            }

            String result = command.parser(receivedString, displayInfo.timeShift, chgTimeShift, displayInfo.chirpReply, chgChirpReply, displayInfo.captureMode);
            if ( !result.equals("") ) {
                commonAfterCommandExec(result);
            }

            displayInfo.setLedChirpReceiving(false);
        }

        @Override
        public void onStateChanged(int oldState, int newState) {
            /**
             * onStateChanged is called when the SDK changes state.
             */
            Log.d(TAG, "ConnectCallback: onStateChanged " + oldState + " -> " + newState);
            if (newState == ChirpSDKState.CHIRP_SDK_STATE_NOT_CREATED.getCode()) {
                //updateStatus("NotCreated");
            } else if (newState == ChirpSDKState.CHIRP_SDK_STATE_STOPPED.getCode()) {
                //updateStatus("Stopped");
            } else if (newState == ChirpSDKState.CHIRP_SDK_STATE_RUNNING.getCode()) {
                //updateStatus("Running");
            } else if (newState == ChirpSDKState.CHIRP_SDK_STATE_SENDING.getCode()) {
                //updateStatus("Sending");
            } else if (newState == ChirpSDKState.CHIRP_SDK_STATE_RECEIVING.getCode()) {
                //updateStatus("Receiving");
            } else {
                //updateStatus(newState + "");
            }
        }

        @Override
        public void onSystemVolumeChanged(float oldVolume, float newVolume) {
            /**
             * onSystemVolumeChanged is called when the system volume is changed.
             */
            Log.d(TAG, "ConnectCallback: onSystemVolumeChanged " + oldVolume + " -> " + newVolume);
        }
    };

    private void sendString(String sendStr) {
        //入力チェック
        long maxSize = chirp.maxPayloadLength();
        byte[] payload = sendStr.getBytes();
        if (maxSize < payload.length) {
            Log.d(TAG, "Invalid Payload");
            return;
        }

        //音量を送信用に設定する
        chirp.setSystemVolume(displayInfo.replyVolume);
        //送信
        ChirpError error = chirp.send(payload);
        if (error.getCode() > 0) {
            Log.d(TAG, error.getMessage());
            //音量を戻す
            chirp.setSystemVolume(displayInfo.restoreVolume);
        }
    }

    private void initalChirpSdk(int chirpConfig) {

        chirp = new ChirpSDK(context, CHIRP_APP_KEY, CHIRP_APP_SECRET);

        displayInfo.chirpSdkVer = chirp.getVersion();
        Log.d(TAG, "Chirp SDK Version: " + displayInfo.chirpSdkVer);

        ChirpError setConfigError;
        switch ( chirpConfig ) {
            case DisplayInfo.CHIRP_CONFIG_16KHZ_MONO :
                setConfigError = chirp.setConfig(CHIRP_APP_CONFIG_16K_MONO);
                break;
            case DisplayInfo.CHIRP_CONFIG_ULTRA_SONICK:
                setConfigError = chirp.setConfig(CHIRP_APP_CONFIG_US);
                break;
            case DisplayInfo.CHIRP_CONFIG_STANDARD:
                setConfigError = chirp.setConfig(CHIRP_APP_CONFIG_STD);
                break;
            default:
                // 引数指定エラーは 16kHz-monoにする
                setConfigError = chirp.setConfig(CHIRP_APP_CONFIG_16K_MONO);
                break;
        }
        if (setConfigError.getCode() > 0) {
            Log.d(TAG, setConfigError.getMessage());
        }

        chirp.setListener(chirpEventListener);

        displayInfo.restoreVolume = chirp.getSystemVolume();
        Log.d(TAG, "restoreVolume=" + String.format("%.1f", displayInfo.restoreVolume )); //
    }


    //==============================================================
    // スレッド (定常ループ)
    //==============================================================
    public void drawOledThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                //ループ手前でやることがあるならば・・・

                //描画ループ
                while (mFinished == false) {
                    //ステータスチェックと描画
                    try {
                        //内部状態獲得
                        new GetCameraStatusTask(mGetCameraStatusCallback).execute();

                        // OLED表示 & 内部状態メッセージ作成
                        displayInfo.displayShootingInfo();
                        // LED表示
                        if ( dispWlanChgCnt>0 ) {
                            //Wlan切り替え操作直後一定期間LED更新をしない
                            dispWlanChgCnt--;
                        } else {
                            displayInfo.setLedChirpConfigAndTsStat();
                            dispWlanChgCnt=0;
                        }

                        //もろもろが高頻度になりすぎないようスリープする
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Deal with error.
                        e.printStackTrace();
                    } finally {
                        //
                    }

                    //Chirp通信でのアンサーバック送信
                    if ( chripReplyCnt>0 ) {
                        chripReplyCnt--;
                        if ( chripReplyCnt == 0 ){
                            if ( displayInfo.chirpConfig == DisplayInfo.CHIRP_CONFIG_ULTRA_SONICK ) {
                                sendString(chripReplyStr8);
                            } else {
                                sendString(chripReplyStr32);
                            }
                        }
                    }
                }
            }
        }).start();
    }

    //==============================================================
    // 設定保存・復帰
    //==============================================================
    private static final String SAVE_KEY_CHIRP_CONFIG  = "chirpConfig";
    private static final String SAVE_KEY_CHIRP_REPLY = "chirpReply";
    private static final String SAVE_KEY_TIME_SHIFT = "timeShift";
    SharedPreferences sharedPreferences;

    void restorePluginInfo() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        displayInfo.chirpConfig = sharedPreferences.getInt(SAVE_KEY_CHIRP_CONFIG, DisplayInfo.CHIRP_CONFIG_16KHZ_MONO);
        displayInfo.chirpReply = sharedPreferences.getBoolean(SAVE_KEY_CHIRP_REPLY,true);
        displayInfo.timeShift = sharedPreferences.getBoolean(SAVE_KEY_TIME_SHIFT,false);
    }

    void savePluginInfo() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SAVE_KEY_CHIRP_CONFIG, displayInfo.chirpConfig);
        editor.putBoolean(SAVE_KEY_CHIRP_REPLY, displayInfo.chirpReply);
        editor.putBoolean(SAVE_KEY_TIME_SHIFT, displayInfo.timeShift);
        editor.commit();
    }

}
