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


public class ChangeIsoTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChgISO";

    public static final String MINUS = "-";
    public static final String PLUS  = "+";


    private String setIso;

    private Consumer<String> consumer ;

    public ChangeIsoTask(Consumer<String> consumer, String inputIso) {
        this.consumer = consumer;
        setIso = inputIso;
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

                String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"iso\", \"isoSupport\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);
                Log.d(TAG, "debug result=" + strResult);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                String curIso = options.getString("iso");
                JSONArray isoSupport = options.getJSONArray("isoSupport");

                if ( (setIso.equals(MINUS)) || (setIso.equals(PLUS)) ) {
                    int curPos=-1;
                    for (int i=0; i<isoSupport.length(); i++) {
                        if ( curIso.equals( isoSupport.getString(i) ) ) {
                            curPos = i;
                            break;
                        }
                    }

                    int nextPos=0;
                    if ( setIso.equals(PLUS) ) {
                        nextPos = curPos+1;
                        if ( nextPos >= isoSupport.length()) {
                            nextPos = isoSupport.length() - 1;
                        }
                    } else {
                        nextPos = curPos-1;
                        if ( nextPos < 0) {
                            nextPos = 0;
                        }
                    }

                    String newParam = isoSupport.getString(nextPos);
                    setValue = newParam;
                    String strJsonSetSS = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"iso\":\"" + setValue +"\"} } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetSS);

                } else {
                    if (setIso.equals("")) {
                        setIso=curIso;
                    }
                    boolean mutchFlag = false;
                    for (int i=0; i<isoSupport.length(); i++) {
                        if ( setIso.equals(isoSupport.getString(i)) ) {
                            mutchFlag = true;
                            break;
                        }
                    }
                    if (mutchFlag){
                        //送信
                        setValue = setIso;
                        String strJsonSetSS = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"iso\":\"" + setValue +"\"} } }";
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
        String setResult = setValue;
        if ( setValue.equals("0")) {
            setResult = "auto";
        }
        Log.d(TAG, "result ISO=" + setResult);
        consumer.accept(setResult);
    }

}
