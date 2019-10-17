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

package com.theta360.pluginapplication.task;

import android.os.AsyncTask;
import android.util.Log;

import com.theta360.pluginapplication.network.HttpConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.function.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ChangeShutterSpeedTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChgSS";

    public static final String MINUS = "-";
    public static final String PLUS  = "+";

    private String setSS;

    private Consumer<String> consumer ;
    private Map<String, String>  ssApi2Cmd;

    public ChangeShutterSpeedTask(Consumer<String> consumer, Map<String, String> ssApi2Cmd, String inputSS) {
        this.consumer = consumer;
        this.ssApi2Cmd = ssApi2Cmd;

        setSS = inputSS;
        //今回は、このタスクの引数を与える時点で、
        //呼び出し元が入力文字列をwebAPIで定義された文字列に読み替えていることを前提とする。
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        String setValue="";

        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";

        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_STAT, "");
        
        try {
            JSONObject output2 = new JSONObject(strResult);
            JSONObject state = output2.getJSONObject("state");
            String captureStatus = state.getString("_captureStatus");
            int recordedTime = state.getInt("_recordedTime");

            if ( captureStatus.equals("idle") && (recordedTime==0) ) {

                String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"shutterSpeed\", \"shutterSpeedSupport\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);
                Log.d(TAG, "debug result=" + strResult);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                String curShutterSpeed = options.getString("shutterSpeed");
                JSONArray shutterSpeedSupport = options.getJSONArray("shutterSpeedSupport");

                if ( (setSS.equals(MINUS)) || (setSS.equals(PLUS)) ) {
                    int curPos=-1;
                    for (int i=0; i<shutterSpeedSupport.length(); i++) {
                        if ( curShutterSpeed.equals( shutterSpeedSupport.getString(i) ) ) {
                            curPos = i;
                            break;
                        }
                    }

                    int nextPos=0;
                    if ( setSS.equals(PLUS) ) {
                        nextPos = curPos+1;
                        if ( nextPos >= shutterSpeedSupport.length()) {
                            nextPos = shutterSpeedSupport.length() - 1;
                        }
                    } else {
                        nextPos = curPos-1;
                        if ( nextPos < 0) {
                            nextPos = 0;
                        }
                    }

                    String newParam = shutterSpeedSupport.getString(nextPos);
                    setValue = newParam;
                    String strJsonSetSS = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"shutterSpeed\":\"" + setValue +"\"} } }";
                    Log.d(TAG, "strJsonSetSS=" + strJsonSetSS);
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetSS);

                } else {
                    boolean mutchFlag = false;
                    Log.d(TAG, "setSS=" + setSS);
                    if (setSS.equals("")) {
                        Log.d(TAG, "curShutterSpeed=" + curShutterSpeed);
                        setSS=curShutterSpeed;
                    }
                    double doubleSS = Double.parseDouble(setSS);
                    for (int i=0; i<shutterSpeedSupport.length(); i++) {
                        if ( doubleSS == shutterSpeedSupport.getDouble(i) ) {
                            mutchFlag = true;
                            break;
                        }
                    }
                    if (mutchFlag){
                        //送信
                        setValue = setSS;
                        String strJsonSetSS = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"shutterSpeed\":\"" + setValue +"\"} } }";
                        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetSS);
                    } else {
                        //パラメーターエラー
                        setValue = "ParamERR";
                    }
                }
            } else {
                //実行不可(BUSY)
                setValue = "BUSY";
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
            //通信エラー
            setValue = "COM ERR";
        }

        return setValue;
    }

    @Override
    protected void onPostExecute(String setValue) {
        String setResult = "";
        try {
            double chkNum = Double.parseDouble(setValue);

            List<String> listSsKey = new ArrayList<>( ssApi2Cmd.keySet() );
            for ( String s : listSsKey) {
                double listNum;
                try {
                    listNum = Double.parseDouble(s);
                    if ( chkNum == listNum ) {
                        setResult = ssApi2Cmd.get(s);
                        break;
                    }
                } catch (NumberFormatException e) {
                    // 無処理
                }
            }
        } catch (NumberFormatException e) {
            setResult = setValue;
        }
        Log.d(TAG, "result SS=" + setResult);
        consumer.accept(setResult);
    }

}
