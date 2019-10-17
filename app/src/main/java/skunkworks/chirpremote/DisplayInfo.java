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


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.theta360.pluginapplication.oled.Oled;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;


public class DisplayInfo extends Oled {

    public boolean timeShift;
    public int exposureDelay;
    public String captureMode;
    public String strExpProg;
    public String strAv;
    public String strTv;
    public String strIso;
    public String strExpComp;
    public String strWb;
    public String strColorTemperature;


    private Map<String, String> wbApi2Filename;
    private Map<String, String> wbApi2Cmd;
    private Map<String, String> expProgApi2Disp;
    private Map<String, String> ssApi2Cmd;


    public static final int CHIRP_CONFIG_16KHZ_MONO = 1;
    public static final int CHIRP_CONFIG_ULTRA_SONICK = 2;
    public static final int CHIRP_CONFIG_STANDARD = 3;
    public int chirpConfig;
    public boolean chirpReply = true;
    public String chirpSdkVer = "";
    public float replyVolume = 1.0f;  //Chirpでの返信に使う音量
    public float restoreVolume = 0.0f; //Chirpでの返信以外で元の音量に戻す用


    public int wavePos=0;
    public int messageCnt=0;
    public String messageStr="";

    public static final int DIALOG_MSG_NORM = 0;
    public static final int DIALOG_MSG_RCV  = 1;
    public static final int DIALOG_MSG_SEND = 2;
    public int messageType=DIALOG_MSG_NORM;

    private final Context context;

    public DisplayInfo (Context context, Map<String, String> expProgApi2Disp, Map<String, String> ssApi2Cmd, Map<String, String>  wbApi2Cmd) {
        super(context);
        this.context = context;

        this.expProgApi2Disp = expProgApi2Disp;
        this.ssApi2Cmd = ssApi2Cmd;
        this.wbApi2Cmd = wbApi2Cmd;

        timeShift = false;

        chirpConfig = CHIRP_CONFIG_16KHZ_MONO;
        captureMode = "";
        exposureDelay = 0;
        strExpProg = "2";
        strAv = "0";
        strTv = "0";
        strIso = "0";
        strExpComp = "0.0";
        strWb = "auto";
        strColorTemperature = "5000";


        wbApi2Filename = new HashMap<String, String>();
        wbApi2Filename.put("auto"                 , "wb0_auto.bmp"    );
        wbApi2Filename.put("daylight"             , "wb1_daylight.bmp");
        wbApi2Filename.put("shade"                , "wb2_shade.bmp"   );
        wbApi2Filename.put("cloudy-daylight"      , "wb3_cloudy.bmp"  );
        wbApi2Filename.put("incandescent"         , "wb4_lamp1.bmp"   );
        wbApi2Filename.put("_warmWhiteFluorescent", "wb5_lamp2.bmp"   );
        wbApi2Filename.put("_dayLightFluorescent" , "wb6_fluo1.bmp"   );
        wbApi2Filename.put("_dayWhiteFluorescent" , "wb7_fluo2.bmp"   );
        wbApi2Filename.put("fluorescent"          , "wb8_fluo3.bmp"   );
        wbApi2Filename.put("_bulbFluorescent"     , "wb9_fluo4.bmp"   );

    }


    void displayShootingInfo() {
        int PosX = 1;
        int line1 = 0;
        int line2 = 8;
        int line3 = 16;

        clear();

        //--- Line1 ---
        if ( captureMode.equals("video") ) {
            setBitmap(2, 0, 12, 8,  0, 0, 128, "capmode2_move.bmp" );
        } else {
            setBitmap(2, 0, 12, 8,  0, 0, 128, "capmode1_still.bmp" );
        }

        if ( exposureDelay == 0 ) {
            //空欄のまま
        } else {
            setBitmap(16, 0, 8, 8,  0, 0, 128, "timer.bmp" );
        }

        String dispExpProg = "["+ convertExpProg() + "]";
        setString(26, line1, dispExpProg);

        // Chirp Icon
        if (chirpReply) {
            setBitmap( 67, line1, 17, 8,  0, 0, 128, "chirpLineResOn.bmp" );
        } else {
            setBitmap( 67, line1, 17, 8,  0, 0, 128, "chirpLineResOff.bmp" );
        }
        // Chirp Config
        switch (chirpConfig){
            case CHIRP_CONFIG_16KHZ_MONO :
                setString(86, line1, "16kHz-m");
                break;
            case CHIRP_CONFIG_ULTRA_SONICK :
                setString(87, line1, "Ultra ");
                break;
            case CHIRP_CONFIG_STANDARD :
                setString(87, line1, "Std   ");
                break;
            default:
                setString(87, line1, "Undef ");
                break;
        }

        //--- Line2 ---
        String strAvTv = "F" + convertAv() + "   " + convertTv();
        setString(PosX, line2, strAvTv);
        setBitmap((6*15), line2, 12, 8,  0, 0, 128, "iso.bmp" );
        setString((6*17)+1, line2, convertIso());


        //--- Line3 ---
        setString(PosX, line3, convertExpComp());
        setBitmap((6*4)+2, line3, 12, 8,  0, 0, 128, "ev.bmp" );

        setBitmap((6*7), line3, 12, 8,  0, 0, 128, "wb.bmp" );
        if ( wbApi2Filename.containsKey(strWb) ) {
            setBitmap((6*9)+1, line3, 24, 8,  0, 0, 128, wbApi2Filename.get(strWb) );
        } else {
            if ( strWb.equals("_colorTemperature") ) {
                setString((6*9)+1, line3, strColorTemperature + "K");
            } else {
                setString((6*9)+1, line3, "Undef");
            }
        }

        if (timeShift){
            setString((6*15)+1, line3, "Tshift");
        }

        //sample Chirp Status dialog
        if ( messageCnt != 0 ) {
            messageCnt--;

            int dalogWidth = (Oled.FONT_WIDTH * 18) + 3*2;
            int dialogHeight = 21;
            int dialogX = 7;
            int dialogY = 2;

            rectFill(dialogX, dialogY, dalogWidth, dialogHeight, black, false);
            rect(dialogX+1,dialogY+1,dalogWidth-2, dialogHeight-2);
            //Chirp icon for dialog
            drawChirpIconDialog(dialogX, dialogY, dalogWidth, dialogHeight);
        } else {
            wavePos=0;
        }

        draw();
    }

    void drawChirpIconDialog(int dialogX, int dialogY, int dalogWidth, int dialogHeight) {
        int infoAreaWidth = dalogWidth-(4+3+28)/*79*/;

        if ( messageType == DIALOG_MSG_RCV ) {
            int offset = 40;
            setBitmap(dialogX + 3, dialogY + 3, 28, 15, 0, 0, 128, "chirpDialog2.bmp");

            setBitmap(dialogX + 32, dialogY + 3, 79, 15, offset+wavePos, 0, 128, "chirpDialogWave.bmp");
            wavePos += 4;
            if ( (offset+wavePos) >= (200 - infoAreaWidth) ) {
                wavePos = 0;
            }
        } else if ( messageType == DIALOG_MSG_SEND ) {
            if ( (messageCnt%2) == 0 ) {
                setBitmap( dialogX+3, dialogY+3, 28, 15,  0, 0, 128, "chirpDialog1.bmp" );
            } else {
                setBitmap( dialogX+3, dialogY+3, 28, 15,  0, 0, 128, "chirpDialog3.bmp" );
            }

            setBitmap(dialogX + 32, dialogY + 3, 79, 15, (116-wavePos), 0, 128, "chirpDialogWave.bmp");
            wavePos += 4;
            if (wavePos >= 116) {
                wavePos = 0;
            }
        } else {
            setBitmap( dialogX+3, dialogY+3, 28, 15,  0, 0, 128, "chirpDialog1.bmp" );
            setString( dialogX+3+29+10, dialogY+2+4, messageStr);
        }
    }


    String convertExpProg(){
        String result = "";
        if ( expProgApi2Disp.containsKey(strExpProg) ) {
            result = expProgApi2Disp.get(strExpProg);
        } else {
            result = "ERR ";
        }
        return result;
    }

    String convertAv(){
        String result = "";
        double nowAv = Double.parseDouble(strAv);
        if ( nowAv == 0.0 ) {
            result = "---";
        } else {
            result = strAv;
        }
        return result;
    }

    String convertTv(){
        String result = "";
        double nowSs = Double.parseDouble(strTv);

        if ( nowSs == 0.0 ) {
            result = "------";
        } else if ( nowSs < 1.0 ) {
            String ssStr;
            double denominator = 1.0 / nowSs;
            // 0.4 (1/2.5), 0.625 (1/1.6), 0.76923076 (1/1.3) は小数点下位1桁表示
            if ( (nowSs==0.4) || (nowSs==0.625) || (nowSs==0.76923076) ) {
                ssStr = String.format("%.1f", denominator );
            } else {
                ssStr = String.format("%.0f", denominator );
            }
            result = "1/" + ssStr;
        } else {
            String ssStr;
            //1.3, 1.6, 2.5, 3.2 は小数点下位1桁表示
            if ( (nowSs==1.3) || (nowSs==1.6) || (nowSs==2.5) || (nowSs==3.2) ) {
                ssStr = String.format("%.1f", nowSs );
            } else {
                ssStr = String.format("%.0f", nowSs );
            }
            result = ssStr + "\"";
        }

        return result;
    }

    String convertIso(){
        String result = "";
        if ( strIso.equals("0") ) {
            result = "----";
        } else {
            result = strIso;
        }

        return result;
    }

    String convertExpComp() {
        String result = "";
        if ( strExpProg.equals("1") ) {
            result = "----";
        } else {
            if ( Double.parseDouble(strExpComp) >= 0.0 ){
                result = " " + strExpComp;
            } else {
                result = strExpComp;
            }
        }

        return result;
    }

    String convertWb(){
        String result = "";
        if ( strWb.equals("_colorTemperature") ) {
            result = strColorTemperature + "K";
        } else {
            result = wbApi2Cmd.get(strWb);
        }
        return result;
    }

    String editChirpReply32() {
        String result = "";

        //動画モードを禁止としたのでコメントアウト（参考として残す）
        //if ( captureMode.equals("image") ) {
        //    result = "i-" ;
        //} else {
        //    result = "m-" ;
        //}

        String nowExpProg = convertExpProg();
        result += nowExpProg.trim() + " " ;
        int exposureProgram = Integer.parseInt(strExpProg);
        switch (exposureProgram) {
            case 1 :    //MANU
                result += "F" + convertAv() + " " + convertTv() + " " + convertIso() + " " + convertWb() ;
                break;
            case 2 :    //AUTO
                result += convertExpComp() + "ev " + " " + convertWb() ;
                break;
            case 3 :    //Av
                result += convertExpComp() + "ev F" + convertAv() + " " + convertWb() ;
                break;
            case 4 :    //Tv
                result += convertExpComp() + "ev ss" + convertTv() + " " + convertWb() ;
                break;
            case 9 :    //ISO
                result += convertExpComp() + "ev iso" + convertIso() + " " + convertWb() ;
                break;
            default:   //ERROR
                break;
        }

        //captureModeを返さなくしたのでTimeShiftの情報を追加できるようになった
        if (timeShift) {
            result += " ts";
        }

        return result;
    }

    void setLedChirpReceiving(boolean ledOn) {
        if (ledOn) {
            Intent intentLedShow = new Intent("com.theta360.plugin.ACTION_LED_SHOW");
            intentLedShow.putExtra("target", LedTarget.LED6.toString());
            context.sendBroadcast(intentLedShow);
        } else {
            Intent intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
            intentLedHide.putExtra("target", LedTarget.LED6.toString());
            context.sendBroadcast(intentLedHide);
        }
    }
    void setLedChirpSending(boolean ledOn) {
        if (ledOn) {
            Intent intentLedShow = new Intent("com.theta360.plugin.ACTION_LED_SHOW");
            intentLedShow.putExtra("target", LedTarget.LED6.toString());
            context.sendBroadcast(intentLedShow);
        } else {
            Intent intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
            intentLedHide.putExtra("target", LedTarget.LED6.toString());
            context.sendBroadcast(intentLedHide);
        }
    }

    void setLedChirpConfigAndTsStat() {
        Intent intentLedShow ;
        Intent intentLedHide;

        //LED3 : Chirp Config状態
        intentLedShow = new Intent("com.theta360.plugin.ACTION_LED_SHOW");
        intentLedShow.putExtra("target", LedTarget.LED3.toString());
        switch (chirpConfig) {
            case CHIRP_CONFIG_16KHZ_MONO :
                intentLedShow.putExtra("color", LedColor.YELLOW.toString());
                break;
            case CHIRP_CONFIG_ULTRA_SONICK :
                intentLedShow.putExtra("color", LedColor.WHITE.toString());
                break;
            case CHIRP_CONFIG_STANDARD :
                intentLedShow.putExtra("color", LedColor.MAGENTA.toString());
                break;
            default:
                intentLedShow.putExtra("color", LedColor.RED.toString());
                break;
        }
        context.sendBroadcast(intentLedShow);

        //LED4/LED5 : Chirp返信 Ena/Dis (実験のためLED4/LED5の切り替えも実装しておく）
        if (captureMode.equals("image")) {
            if (chirpReply) {
                intentLedShow.putExtra("target", LedTarget.LED4.toString());
                context.sendBroadcast(intentLedShow);
            } else {
                intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
                intentLedHide.putExtra("target", LedTarget.LED4.toString());
                context.sendBroadcast(intentLedHide);
            }

            intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
            intentLedHide.putExtra("target", LedTarget.LED5.toString());
            context.sendBroadcast(intentLedHide);

        } else {
            intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
            intentLedHide.putExtra("target", LedTarget.LED4.toString());
            context.sendBroadcast(intentLedHide);

            if (chirpReply) {
                intentLedShow.putExtra("target", LedTarget.LED5.toString());
                context.sendBroadcast(intentLedShow);
            } else {
                intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
                intentLedHide.putExtra("target", LedTarget.LED5.toString());
                context.sendBroadcast(intentLedHide);
            }
        }

        //LED7 : TIme Sift状態
        if (timeShift) {
            intentLedShow.putExtra("target", LedTarget.LED7.toString());
            context.sendBroadcast(intentLedShow);
        } else {
            intentLedHide = new Intent("com.theta360.plugin.ACTION_LED_HIDE");
            intentLedHide.putExtra("target", LedTarget.LED7.toString());
            context.sendBroadcast(intentLedHide);
        }
    }

}
