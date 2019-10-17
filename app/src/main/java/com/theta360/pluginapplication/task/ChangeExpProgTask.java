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


public class ChangeExpProgTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChgExpProg";

    public static final String MINUS = "-";
    public static final String PLUS  = "+";

    private String setExpProg;
    private Map<String, String>  expProgApi2Disp;

    private Consumer<String> consumer ;

    public ChangeExpProgTask(Consumer<String> consumer, Map<String, String>  expProgApi2Disp, String inExpProg) {
        this.expProgApi2Disp = expProgApi2Disp;
        this.consumer = consumer;
        setExpProg = inExpProg;
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

                String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"exposureProgram\", \"exposureProgramSupport\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);
                Log.d(TAG, "debug result=" + strResult);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                int curExposureProgram = options.getInt("exposureProgram");
                JSONArray expProgSupport = options.getJSONArray("exposureProgramSupport");


                if ( setExpProg.equals(MINUS) || setExpProg.equals(PLUS) ) {
                    int curPos=-1;
                    for (int i=0; i<expProgSupport.length(); i++) {
                        if ( curExposureProgram == expProgSupport.getInt(i) ) {
                            curPos = i;
                            break;
                        }
                    }

                    int nextPos=0;
                    if ( setExpProg.equals(PLUS) ) {
                        nextPos = curPos+1;
                        if ( nextPos >= expProgSupport.length()) {
                            nextPos = 0;
                        }
                    } else {
                        nextPos = curPos-1;
                        if ( nextPos < 0) {
                            nextPos = expProgSupport.length() - 1;
                        }
                    }

                    int newParam = expProgSupport.getInt(nextPos);
                    setValue = String.valueOf(newParam);
                    String strJsonSetExpProg = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"exposureProgram\":\"" + setValue +"\"} } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetExpProg);

                } else {
                    Log.d(TAG, "setExpProg=" + setExpProg);
                    if (setExpProg.equals("")) {
                        setExpProg=String.valueOf(curExposureProgram);
                    }

                    boolean mutchFlag = false;
                    for (int i=0; i<expProgSupport.length(); i++) {
                        if ( setExpProg.equals( expProgSupport.getString(i) ) ) {
                            mutchFlag = true;
                            break;
                        }
                    }
                    if (mutchFlag){
                        //送信
                        setValue = String.valueOf(setExpProg);
                        String strJsonSetExpProg = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"exposureProgram\":\"" + setValue +"\"} } }";
                        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetExpProg);
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
        if ( expProgApi2Disp.containsKey(setValue) ) {
            setResult = expProgApi2Disp.get(setValue);
        } else {
            setResult = setValue;
        }
        Log.d(TAG, "result expProg=" + setResult);
        consumer.accept(setResult);
    }

}
