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

import com.theta360.pluginapplication.task.ShutterButtonTask;
import com.theta360.pluginapplication.task.ChangeCaptureModeTask;
import com.theta360.pluginapplication.task.ChangeExposureDelayTask;
import com.theta360.pluginapplication.task.ChangeEvTask;
import com.theta360.pluginapplication.task.ChangeExpProgTask;
import com.theta360.pluginapplication.task.ChangeShutterSpeedTask;
import com.theta360.pluginapplication.task.ChangeIsoTask;
import com.theta360.pluginapplication.task.ChangeApertureTask;
import com.theta360.pluginapplication.task.ChangeWbTask;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import android.util.Log;


public class Command  {
    private static final String TAG = "Command";

    private static final int CMD_LEN_MIN = 1;
    private static final int CMD_LEN_MAX = 8;

    private Consumer<String> consumer ;

    public Map<String, String> expProgCmd2Api;
    public Map<String, String> expProgApi2Disp;

    public Map<String, String> evCmd2Api;
    public Map<String, Integer> expDelayCmd2Api;

    public Map<String, String>  wbCmd2Api;
    public Map<String, String>  wbApi2Cmd;

    public Map<String, String>  ssCmd2Api;
    public Map<String, String>  ssApi2Cmd;


    public Command (Consumer<String> consumer) {
        this.consumer = consumer;

        expProgCmd2Api = new HashMap<String, String>();
        expProgCmd2Api.put("", "");
        expProgCmd2Api.put("+", "+");
        expProgCmd2Api.put("-", "-");
        expProgCmd2Api.put("m", "1");
        expProgCmd2Api.put("a", "2");
        expProgCmd2Api.put("p", "2");
        expProgCmd2Api.put("av", "3");
        expProgCmd2Api.put("tv", "4");
        expProgCmd2Api.put("iso", "9");

        expProgApi2Disp = new HashMap<String, String>();
        expProgApi2Disp.put("1", "MANU");
        expProgApi2Disp.put("2", "AUTO");
        expProgApi2Disp.put("3", " Av ");
        expProgApi2Disp.put("4", " Tv ");
        expProgApi2Disp.put("9", "ISO ");

        evCmd2Api = new HashMap<String, String>();
        evCmd2Api.put("", "");
        evCmd2Api.put("+", "+");
        evCmd2Api.put("-", "-");
        evCmd2Api.put("-2.0", "-2.0");
        evCmd2Api.put("-1.7", "-1.7");
        evCmd2Api.put("-1.3", "-1.3");
        evCmd2Api.put("-1.0", "-1.0");
        evCmd2Api.put("-0.7", "-0.7");
        evCmd2Api.put("-0.3", "-0.3");
        evCmd2Api.put("0.0" , "0.0" );
        evCmd2Api.put("0.3" , "0.3" );
        evCmd2Api.put("0.7" , "0.7" );
        evCmd2Api.put("1.0" , "1.0" );
        evCmd2Api.put("1.3" , "1.3" );
        evCmd2Api.put("1.7" , "1.7" );
        evCmd2Api.put("2.0" , "2.0" );

        expDelayCmd2Api = new HashMap<String, Integer>();
        expDelayCmd2Api.put("", ChangeExposureDelayTask.EXPOSURE_DELAY_CUR);
        expDelayCmd2Api.put("0", 0);
        expDelayCmd2Api.put("1", 1);
        expDelayCmd2Api.put("2", 2);
        expDelayCmd2Api.put("3", 3);
        expDelayCmd2Api.put("4", 4);
        expDelayCmd2Api.put("5", 5);
        expDelayCmd2Api.put("6", 6);
        expDelayCmd2Api.put("7", 7);
        expDelayCmd2Api.put("8", 8);
        expDelayCmd2Api.put("9", 9);
        expDelayCmd2Api.put("10", 10);


        wbCmd2Api = new HashMap<String, String>();
        wbCmd2Api.put("" , "");
        wbCmd2Api.put("+", "+");
        wbCmd2Api.put("-", "-");
        wbCmd2Api.put("auto" , "auto");
        wbCmd2Api.put("day"  , "daylight");
        wbCmd2Api.put("shade", "shade");
        wbCmd2Api.put("cloud", "cloudy-daylight");
        wbCmd2Api.put("lamp1", "incandescent");
        wbCmd2Api.put("lamp2", "_warmWhiteFluorescent");
        wbCmd2Api.put("fluo1", "_dayLightFluorescent");
        wbCmd2Api.put("fluo2", "_dayWhiteFluorescent");
        wbCmd2Api.put("fluo3", "fluorescent");
        wbCmd2Api.put("fluo4", "_bulbFluorescent");

        List<String> listWbKey = new ArrayList<>( wbCmd2Api.keySet() );
        wbApi2Cmd = new HashMap<String, String>();
        for (String s: listWbKey) {
            wbApi2Cmd.put( wbCmd2Api.get(s), s);
        }


        ssCmd2Api = new HashMap<String, String>();
        ssCmd2Api.put("", "");
        ssCmd2Api.put("+", "+");
        ssCmd2Api.put("-", "-");
        ssCmd2Api.put("25000", "0.00004");
        ssCmd2Api.put("20000", "0.00005");
        ssCmd2Api.put("16000", "0.0000625");
        ssCmd2Api.put("12500", "0.00008");
        ssCmd2Api.put("10000", "0.0001");
        ssCmd2Api.put("8000",  "0.000125");
        ssCmd2Api.put("6400",  "0.00015625");
        ssCmd2Api.put("5000",  "0.0002");
        ssCmd2Api.put("4000",  "0.00025");
        ssCmd2Api.put("3200",  "0.0003125");
        ssCmd2Api.put("2500",  "0.0004");
        ssCmd2Api.put("2000",  "0.0005");
        ssCmd2Api.put("1600",  "0.000625");
        ssCmd2Api.put("1250",  "0.0008");
        ssCmd2Api.put("1000",  "0.001");
        ssCmd2Api.put("800",   "0.00125");
        ssCmd2Api.put("640",   "0.0015625");
        ssCmd2Api.put("500",   "0.002");
        ssCmd2Api.put("400",   "0.0025");
        ssCmd2Api.put("320",   "0.003125");
        ssCmd2Api.put("250",   "0.004");
        ssCmd2Api.put("200",   "0.005");
        ssCmd2Api.put("160",   "0.00625");
        ssCmd2Api.put("125",   "0.008");
        ssCmd2Api.put("100",   "0.01");
        ssCmd2Api.put("80",    "0.0125");
        ssCmd2Api.put("60",    "0.01666666");
        ssCmd2Api.put("50",    "0.02");
        ssCmd2Api.put("40",    "0.025");
        ssCmd2Api.put("30",    "0.03333333");
        ssCmd2Api.put("25",    "0.04");
        ssCmd2Api.put("20",    "0.05");
        ssCmd2Api.put("15",    "0.06666666");
        ssCmd2Api.put("13",    "0.07692307");
        ssCmd2Api.put("10",    "0.1");
        ssCmd2Api.put("8",     "0.125");
        ssCmd2Api.put("6",     "0.16666666");
        ssCmd2Api.put("5",     "0.2");
        ssCmd2Api.put("4",     "0.25");
        ssCmd2Api.put("3",     "0.33333333");
        ssCmd2Api.put("2.5",   "0.4");
        ssCmd2Api.put("2",     "0.5");
        ssCmd2Api.put("1.6",   "0.625");
        ssCmd2Api.put("1.3",   "0.76923076");
        ssCmd2Api.put("1\"",   "1");
        ssCmd2Api.put("1.3\"", "1.3");
        ssCmd2Api.put("1.6\"", "1.6");
        ssCmd2Api.put("2\"",   "2");
        ssCmd2Api.put("2.5\"", "2.5");
        ssCmd2Api.put("3.2\"", "3.2");
        ssCmd2Api.put("4\"",   "4");
        ssCmd2Api.put("5\"",   "5");
        ssCmd2Api.put("6\"",   "6");
        ssCmd2Api.put("8\"",   "8");
        ssCmd2Api.put("10\"",  "10");
        ssCmd2Api.put("13\"",  "13");
        ssCmd2Api.put("15\"",  "15");
        ssCmd2Api.put("20\"",  "20");
        ssCmd2Api.put("25\"",  "25");
        ssCmd2Api.put("30\"",  "30");
        ssCmd2Api.put("60\"",  "60");

        List<String> listSsKey = new ArrayList<>( ssCmd2Api.keySet() );
        ssApi2Cmd = new HashMap<String, String>();
        for (String s: listSsKey) {
            ssApi2Cmd.put( ssCmd2Api.get(s), s);
        }
        ssApi2Cmd.put( "0", "auto");

    }

    String parser(String inStr, boolean timeShift, Consumer<Integer> chgTimeShift, boolean chirpReply, Consumer<Integer> chgChirpReply, String captureMode) {
		String result = "";

        inStr.trim();
        int len = inStr.length();

        if ( (CMD_LEN_MIN <= len) && (len <= CMD_LEN_MAX) ) {
            String[] splitStr = inStr.split(" ", 0);
            if ( (splitStr.length==1) || (splitStr.length==2) ) {

                String cmdName = splitStr[0];
                String cmdParam = "";
                if (splitStr.length==2) {
                    cmdParam = splitStr[1];
                }

                if ( cmdName.equals("shutter") || cmdName.equals("tp") ) {
                    if ( captureMode.equals("image") ) {
                        new ShutterButtonTask(timeShift).execute();
                    } else {
                        //Chirpがオーディオ使用中のため、録画させない
                        result = "Mode ERR";
                    }

                } else if ( cmdName.equals("image") ) {
                    new ChangeCaptureModeTask(consumer, ChangeCaptureModeTask.CAPMODE_IMAGE).execute();
                } else if ( cmdName.equals("move") ) {
                    new ChangeCaptureModeTask(consumer, ChangeCaptureModeTask.CAPMODE_VIDEO).execute();

                } else if ( cmdName.equals("mode") ) {
                    if ( expProgCmd2Api.containsKey(cmdParam) ) {
                        String inParam = expProgCmd2Api.get(cmdParam);
                        new ChangeExpProgTask(consumer, expProgApi2Disp, inParam).execute();
                    } else {
                        result = "ParamERR";
                    }

                } else if ( cmdName.equals("ev") ) {
                    if ( evCmd2Api.containsKey(cmdParam) ) {
                        String inParam = evCmd2Api.get(cmdParam);
                        new ChangeEvTask(consumer, inParam).execute();
                    } else {
                        result = "ParamERR";
                    }

                } else if ( cmdName.equals("ss") ) {
                    if ( ssCmd2Api.containsKey(cmdParam) ) {
                        String apiParam = ssCmd2Api.get(cmdParam);
                        new ChangeShutterSpeedTask(consumer, ssApi2Cmd, apiParam).execute();
                    } else {
                        result = "ParamERR";
                    }

                } else if ( cmdName.equals("f") ) {
                    new ChangeApertureTask(consumer, cmdParam).execute();

                } else if ( cmdName.equals("iso") ) {
                    new ChangeIsoTask(consumer, cmdParam).execute();

                } else if ( cmdName.equals("wb") ) {
                    if ( wbCmd2Api.containsKey(cmdParam) ) {
                        String apiParam = wbCmd2Api.get(cmdParam);
                        new ChangeWbTask(consumer, wbApi2Cmd, apiParam, "").execute();
                    } else {
                        try {
                            Integer.parseInt(cmdParam);
                            Log.d(TAG, "CT val=" + cmdParam);
                            new ChangeWbTask(consumer, wbApi2Cmd, ChangeWbTask.CT, cmdParam).execute();
                        } catch (NumberFormatException e) {
                            result = "ParamERR";
                        }
                    }

                } else if ( cmdName.equals("timer") ) {
                    if ( expDelayCmd2Api.containsKey(cmdParam) ) {
                        int intParam = expDelayCmd2Api.get(cmdParam);
                        new ChangeExposureDelayTask(consumer, intParam).execute();
                    } else {
                        result = "ParamERR";
                    }

                } else if ( cmdName.equals("ts") ) {
                    if ( cmdParam.equals("") ) {
                        if (timeShift) {
                            result = "on";
                        } else {
                            result = "off";
                        }
                    } else if ( cmdParam.equals("on") ) {
                        chgTimeShift.accept(1);
                        result = "on";
                    } else if ( cmdParam.equals("off") ) {
                        chgTimeShift.accept(0);
                        result = "off";
                    } else {
                        result = "ParamERR";
                    }
                } else if ( cmdName.equals("reply") ) {
                    if ( cmdParam.equals("") ) {
                        if (chirpReply) {
                            result = "true";
                        } else {
                            result = "false";
                        }
                    } else if ( cmdParam.equals("t") || cmdParam.equals("e") ) {
                        chgChirpReply.accept(1);
                        result = "true";
                    } else if ( cmdParam.equals("f") || cmdParam.equals("d") ) {
                        chgChirpReply.accept(0);
                        result = "false";
                    } else {
                        result = "ParamERR";
                    }
                } else {
                    result = "UndefCmd";
                }
            } else {
                result = "SplitERR";
            }
        } else {
            result = "Len ERR";
        }

		return result;
    }



}
