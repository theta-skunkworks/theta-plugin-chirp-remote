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


public class ChangeEvTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChgEv";

    public static final String MINUS = "-";
    public static final String PLUS  = "+";

    private String setEv;
    private Consumer<String> consumer ;

    public ChangeEvTask(Consumer<String> consumer, String inEv) {
        this.consumer = consumer;
        setEv = inEv;
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

                String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"exposureCompensation\", \"exposureCompensationSupport\", \"exposureProgram\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                int curExposureProgram = options.getInt("exposureProgram");
                String curExposureCompensation = options.getString("exposureCompensation");
                JSONArray exposureCompensationSupport = options.getJSONArray("exposureCompensationSupport");

                if ( curExposureProgram != 1 ) {    //not MANUAL

                    if ( (setEv.equals(MINUS)) || (setEv.equals(PLUS)) ) {
                        int curPos=-1;
                        for (int i=0; i<exposureCompensationSupport.length(); i++) {
                            if ( curExposureCompensation.equals( exposureCompensationSupport.getString(i) ) ) {
                                curPos = i;
                                break;
                            }
                        }

                        int nextPos=0;
                        if ( setEv.equals(PLUS) ) {
                            nextPos = curPos+1;
                            if ( nextPos >= exposureCompensationSupport.length()) {
                                nextPos = exposureCompensationSupport.length() - 1;
                            }
                        } else {
                            nextPos = curPos-1;
                            if ( nextPos < 0) {
                                nextPos = 0;
                            }
                        }

                        String newParam = exposureCompensationSupport.getString(nextPos);
                        setValue = newParam;
                        String strJsonSetCaptureMode = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"exposureCompensation\":\"" + newParam +"\"} } }";
                        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCaptureMode);
                        Log.d(TAG, "ChangeEvTask: Set exposureCompensation=" + newParam);

                    } else {
                        Log.d(TAG, "setEv=" + setEv);
                        if (setEv.equals("")) {
                            setEv=curExposureCompensation;
                        }

                        boolean mutchFlag = false;
                        double doubleEv = Double.parseDouble(setEv);
                        for (int i=0; i<exposureCompensationSupport.length(); i++) {
                            if ( doubleEv == exposureCompensationSupport.getDouble(i) ) {
                                mutchFlag = true;
                                break;
                            }
                        }

                        if (mutchFlag){
                            //送信
                            setValue = setEv;
                            String strJsonSetCaptureMode = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"exposureCompensation\":\"" + setValue +"\"} } }";
                            strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCaptureMode);
                        } else {
                            //パラメーターエラー
                            setValue = "ParamERR";
                        }
                    }
                } else {
                    //MANUALであることを通知
                    setValue = "Can'tSET";
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
        consumer.accept(setValue);
    }

}
