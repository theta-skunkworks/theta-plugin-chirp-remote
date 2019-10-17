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

import org.json.JSONException;
import org.json.JSONObject;
import java.util.function.Consumer;


public class ChangeExposureDelayTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChgExpDelay";

    public static final int EXPOSURE_DELAY_CUR = -2;
    public static final int EXPOSURE_DELAY_TGGLE = -1;
    public static final int EXPOSURE_DELAY_OFF = 0;
    public static final int EXPOSURE_DELAY_MAX = 10;


    private int setExposureDelay = 0;

    private Consumer<String> consumer ;

    public ChangeExposureDelayTask(Consumer<String> consumer, int inputDelay) {
        this.consumer = consumer;
        if ( EXPOSURE_DELAY_CUR<=inputDelay && inputDelay<=EXPOSURE_DELAY_MAX ) {
            setExposureDelay = inputDelay ;
        } else {
            setExposureDelay = EXPOSURE_DELAY_TGGLE;
        }
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

                if ( (setExposureDelay == EXPOSURE_DELAY_TGGLE) || (setExposureDelay == EXPOSURE_DELAY_CUR) ) {
                    String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"exposureDelay\", \"_latestEnabledExposureDelayTime\"] } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);

                    JSONObject output = new JSONObject(strResult);
                    JSONObject results = output.getJSONObject("results");
                    JSONObject options = results.getJSONObject("options");
                    int curExposureDelay = options.getInt("exposureDelay");
                    int latestEnabledExposureDelayTime = options.getInt("_latestEnabledExposureDelayTime");

                    if ( setExposureDelay == EXPOSURE_DELAY_CUR ) {
                        setExposureDelay = curExposureDelay;
                    } else {
                        if ( curExposureDelay == EXPOSURE_DELAY_OFF ) {
                            setExposureDelay = latestEnabledExposureDelayTime;
                        } else {
                            setExposureDelay = EXPOSURE_DELAY_OFF;
                        }
                    }
                }

                setValue = String.valueOf(setExposureDelay);
                String strJsonSetCaptureMode = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"exposureDelay\":\"" + String.valueOf(setExposureDelay) +"\"} } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetCaptureMode);
                Log.d(TAG, "ChangeExposureDelayTask: Set exposureDelay=" + String.valueOf(setExposureDelay));

            } else {
                //実行不可(BUSY)
                setExposureDelay = -1;
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
