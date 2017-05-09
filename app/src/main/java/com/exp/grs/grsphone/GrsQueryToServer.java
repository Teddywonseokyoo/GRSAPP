package com.exp.grs.grsphone;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by freem on 2016-12-20.
 */

public class GrsQueryToServer extends AsyncTask<GrsPostdataValue, Integer,  String> {


    private Context mContext;
    private static final String TAG = "GRSLOG(QueryToServes)";
    GrsQueryListener grsQueryListener;

    public GrsQueryToServer (Context context){
        grsQueryListener = (GrsQueryListener)context;
        mContext = context;
    }
    @Override
    protected String doInBackground(GrsPostdataValue... params) {

        return multipartRequest(params[0].urlTo,params[0].value, params[0].data);
    }
    protected void onProgressUpdate(Integer... progress) {
       // setProgressPercent(progress[0]);
    }

    protected void onPostExecute(String result) {
       // showDialog("Downloaded " + result + " bytes");
        //Log.d(TAG, "PostExecute Msg : " + result);
        grsQueryListener.GrsServerResponse(result);
    }
    //http://107.170.216.212:8080/api/req_grs
    public String multipartRequest(String urlTo, String value, byte[] data) {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;
        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";
        String result = "";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        //String[] q = filepath.split("/");
        //int idx = q.length - 1;
        try {
           // File file = new File(filepath);
            //FileInputStream fileInputStream = new FileInputStream(file);
            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"userPhoto\"; filename=\"upload.jpg \"" + lineEnd);
            outputStream.writeBytes("Content-Type: " + "image/jpeg" + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.write(data, 0, data.length);
            outputStream.writeBytes(lineEnd);
            // Upload POST Data
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"fdata\"" + lineEnd);
            outputStream.writeBytes("Content-Type: application/json" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(value);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            if (200 != connection.getResponseCode()) {
                //throw new CustomException("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
            }
            inputStream = connection.getInputStream();
            result = this.convertStreamToString(inputStream);
            //fileInputStream.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        }
        catch (Exception e)
        {
            //logger.error(e);

        }
        //Log.d(TAG, "MultipartRequest Msg : " + result);
        return result;
    }


    public interface GrsQueryListener{
        public void GrsServerResponse(String msg);


    }





    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
