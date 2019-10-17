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
import java.util.Map;


public class ChangeWbTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChgWB";

    public static final String MINUS = "-";
    public static final String PLUS  = "+";
    public static final String CT  = "_colorTemperature";

    private String setWB;
    private String setCT;

    private Consumer<String> consumer ;
    private Map<String, String>  wbApi2Cmd;

    public ChangeWbTask(Consumer<String> consumer, Map<String, String>  wbApi2Cmd, String inWB, String inCT) {
        this.consumer = consumer;
        this.wbApi2Cmd = wbApi2Cmd;

        setWB = inWB;
        setCT = inCT;
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

                String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"whiteBalance\", \"whiteBalanceSupport\", \"_colorTemperature\", \"_colorTemperatureSupport\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);
                Log.d(TAG, "debug result=" + strResult);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                String curWB = options.getString("whiteBalance");
                JSONArray wbSupport = options.getJSONArray("whiteBalanceSupport");

                int curCT = options.getInt("_colorTemperature");
                JSONObject ctSupport = options.getJSONObject("_colorTemperatureSupport");
                int ctMax = ctSupport.getInt("maxTemperature");
                int ctMin = ctSupport.getInt("minTemperature");
                int ctStep = ctSupport.getInt("stepSize");

                //引数なしの場合は現在の値を返せるよう、現在の値を設定する
                if ( setWB.equals("") ) {
                    setWB = curWB;
                    setCT = String.valueOf(curCT);
                }

                if ( setWB.equals(CT) || ( curWB.equals(CT) && (setWB.equals(PLUS) || setWB.equals(MINUS)) ) )  {
                    //---- 色温度 ----
                    if ( setCT.equals(PLUS) || setCT.equals(MINUS) || ( curWB.equals(CT) && (setWB.equals(PLUS) || setWB.equals(MINUS)) ) ) {
                        //色温度を +/- 操作する
                        int newCT = curCT;
                        if ( setCT.equals(PLUS) || setWB.equals(PLUS) ) {
                            newCT += ctStep;
                            if ( newCT > ctMax ) {
                                newCT = ctMax;
                            }
                        } else {
                            newCT -= ctStep;
                            if ( newCT < ctMin ) {
                                newCT = ctMin;
                            }
                        }

                        String newParam = String.valueOf(newCT);
                        setValue = newParam;
                        String strJsonSetCT = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"whiteBalance\":\"_colorTemperature\", \"_colorTemperature\":\"" + setValue +"\"} } }";
                        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCT);

                    } else {
                        //指定された色温度にする

                        if (setCT.equals("")) {
                            setCT=String.valueOf(curCT);
                        }
                        int temp = Integer.parseInt(setCT);
                        Log.d(TAG, "setCT=" + String.valueOf(temp) );

                        boolean mutchFlag = false;
                        for (int i=ctMin; i<=ctMax; i+=ctStep) {
                            if ( temp == i ) {
                                mutchFlag = true;
                                break;
                            }
                        }
                        if (mutchFlag){
                            //送信
                            setValue = setCT;
                            String strJsonSetCT = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"whiteBalance\":\"_colorTemperature\", \"_colorTemperature\":\"" + setValue +"\"} } }";
                            strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCT);

                        } else {
                            //パラメーターエラー
                            setValue = "ParamERR";
                        }
                    }
                } else {
                    //---- プリセットWB ----
                    if ( setWB.equals(PLUS) || setWB.equals(MINUS) ) {
                        //プリセットWBを +/- 操作する（色温度は除く）
                        //現在が色温度状態の場合、そこを基点に +/-操作をするだけ

                        int curPos=-1;
                        for (int i=0; i<wbSupport.length(); i++) {
                            if ( curWB.equals( wbSupport.getString(i) ) ) {
                                curPos = i;
                                break;
                            }
                        }

                        int nextPos=0;
                        if ( setWB.equals(PLUS) ) {
                            nextPos = curPos+1;
                            if ( nextPos >= (wbSupport.length()-1) ) { //CTを除くため
                                nextPos = 0;
                            }
                        } else {
                            nextPos = curPos-1;
                            if ( nextPos < 0) {
                                nextPos = wbSupport.length() - 2 ; //CTを除くため
                            }
                        }

                        String newParam = wbSupport.getString(nextPos);
                        setValue = newParam;
                        String strJsonSetWB = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"whiteBalance\":\"" + setValue + "\"} } }";
                        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetWB);

                    } else {
                        //指定されたプリセットWBにする
                        boolean mutchFlag = false;
                        for (int i=0; i<wbSupport.length(); i++) {
                            if ( setWB.equals(wbSupport.getString(i)) ) {
                                mutchFlag = true;
                                break;
                            }
                        }
                        if (mutchFlag){
                            //送信
                            setValue = setWB;
                            String strJsonSetWB = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"whiteBalance\":\"" + setValue + "\"} } }";
                            strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetWB);

                        } else {
                            //パラメーターエラー
                            setValue = "ParamERR";
                        }

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
        if ( wbApi2Cmd.containsKey(setValue) ) {
            setResult = wbApi2Cmd.get(setValue);
        } else {
            setResult = setValue;
        }
        Log.d(TAG, "result WB=" + setResult);
        consumer.accept(setResult);
    }

}
