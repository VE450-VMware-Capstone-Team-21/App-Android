package com.google.ar.sceneform.samples.gltf.flowgate;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/*
 * This class is for sending *one* request to *one* url to get asset information and perform corresponding action
 * Similar functions as `displayAssetInfoOnScreen` can be written:
 * add arguments (e.g. location of barcode) &
 * define a new actor to determine what to do when receiving response (e.g. display with AR)
 */
public class flowgateClientWorker {
    String urlString;   // `urlString` is the string of url to be requested
    String sensorQuery = "https://202.121.180.32//apiservice/v1/assets/server/4e45aa5d59ed4f9b9e3b5" +
            "d493bf7e3d2/realtimedata?starttime=1603217266200&duration=300000";
    //int stepFlag;
    String displayBuffer;

    public flowgateClientWorker(String urlString){
        this.urlString = urlString;
        //this.stepFlag = 0;
    }

    /*
     * `context` is the class of MainActivity,
     * `receivedToken` is the received token used for authorization
     * `actor` is used to determine what to do when receiving response
     * EFFECTS: obtain asset info by sending GET request to `this.urlString`,
     *          and `actor` acts
     */
    public void getAssetInfo(Context context, String receivedToken, flowgateClientActor actor) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, urlString, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            actor.act(response);
                            /*if (stepFlag == 0) {
                                //urlString = sensorQuery;
                                stepFlag = 1;
                            }*/
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Log.d("flowgateClientWorker", "setAssetInfo: " + response.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error maybe throw exception/display message on screen
                        Log.w("flowgateClientWorker",
                                "setAssetInfo: response error - " + error.toString());
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/json");
                params.put("Authorization", "Bearer " + receivedToken);
                params.put("Accept", "application/json");
                return params;
            }
        };

        /*
        JsonObjectRequest jsonSensorRequest = new JsonObjectRequest
                (Request.Method.GET, sensorQuery, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            actor.act(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.w("flowgateClientWorker",
                                "setAssetInfo: response error - " + error.toString());
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/json");
                params.put("Authorization", "Bearer " + receivedToken);
                params.put("Accept", "application/json");
                return params;
            }
        };

         */

        MySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
        //urlString = sensorQuery;
        //stepFlag = 1;
        //MySingleton.getInstance(context).addToRequestQueue(jsonSensorRequest);
    }

    /*
     * EFFECTS: obtain asset info by sending GET request to `this.urlString`,
     *          and display received info on `textView`
     */
    public void displayAssetInfoOnScreen(Context context, String receivedToken, TextView textView){
        flowgateClientActor actor = new flowgateClientActor() {
            @Override
            public void act(JSONObject response) throws JSONException {

                /*
                if (stepFlag == 1){
                    String data = response.toString();
                    displayBuffer = displayBuffer + "\n" + data;
                    textView.setText(displayBuffer);
                    return;
                }*/

                String id = response.getString("id");
                String assetNumber = response.getString("assetNumber");
                String assetName = response.getString("assetName");
                String category = response.getString("category");
                String manufacturer = response.getString("manufacturer");
                String model = response.getString("model");
                String mountingSide = response.getString("mountingSide");
                String location = response.getString("building") + " building, \n" +
                        response.getString("floor") + " th floor, \n" +
                        "room " + response.getString("room") +
                        ",\nrow " + response.getString("row") +
                        ",\ncolumn " + response.getString("col") +
                        ",\ncabinet: " + response.getString("cabinetName");

                String disp =
                        "Asset Number: " + assetNumber + "\n" +
                        "Asset Name: " + assetName  + "\n" +
                        "Category: " + category  + "\n" +
                        "Manufacturer: " + manufacturer  + "\n" +
                        "Model: " + model  + "\n" +
                        "Mounting side: " + mountingSide  + "\n" +
                        "Detailed location: \n" + location;

                displayBuffer = disp;

                textView.setText(disp);
            }
        };
        getAssetInfo(context, receivedToken, actor);
    }

}
