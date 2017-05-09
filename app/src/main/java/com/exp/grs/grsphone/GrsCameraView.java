package com.exp.grs.grsphone;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.android.JavaCameraView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import static android.content.Context.WINDOW_SERVICE;
import static android.content.res.Configuration.*;

/**
 * @author qzhang53
 * this class creates the view for the camera
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GrsCameraView extends JavaCameraView implements PictureCallback {


    private static final String TAG = "GRSLOG(GrsCameraView)";
    private String mPictureFileName;


    public GrsCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged");
        Configuration c = new Configuration(newConfig);
        c.orientation = ORIENTATION_LANDSCAPE;
        super.onConfigurationChanged(c);
        // You could create the layout here by removing all views and rebuilding them
        // Perhaps by having a two xml layouts, one of which is "90 degrees out" ...
        // If you do make the layot here, make sure you don't clash with the constructor code!
        switch (newConfig.orientation) {
            case ORIENTATION_LANDSCAPE:
                // Make the layout for this orientation (as per above)
                break;
            case ORIENTATION_PORTRAIT:
                // Make the layout for this orientation (as per above)
                break;
            case ORIENTATION_SQUARE:
                // Make the layout for this orientation (as per above)
                break;
        }
    }

    /**
     * get the list of available preview size
     * @return
     */
    public List<Camera.Size> getPreviewSize()
    {

        boolean camera_closed=mCamera==null;
        if (camera_closed)
        {
            mCamera = Camera.open();
        }



        Camera.Parameters params=mCamera.getParameters();
        /*
        params.set("orientation", "portrait");

        setDisplayOrientation(mCamera, 90);
        try {
            mCamera.setPreviewDisplay(getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        params.setFocusMode(Parameters.FOCUS_MODE_MACRO);


        //mCamera.setDisplayOrientation(90);  // 세로모드
        mCamera.setParameters(params);

        List<Camera.Size> preview_size=params.getSupportedPreviewSizes();
        Collections.sort(preview_size, new CameraSizeComparator());
        if (camera_closed)
        {
            mCamera.release();
        }
        return preview_size;
    }

    protected void setDisplayOrientation(Camera camera, int angle){
        Method downPolymorphic;
        try
        {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, new Object[] { angle });
        }
        catch (Exception e1)
        {
        }
    }

    /**
     * function for manually select the focus region
     * @param tfocusRect
     */
    public void touchFocus(final Rect tfocusRect, int col, int row)
    {
        /*
        try {
            mCamera.stopFaceDetection();
            mCamera.cancelAutoFocus();
            //Convert from View's width and height to +/- 1000
            final android.graphics.Rect targetFocusRect = new android.graphics.Rect(
                    tfocusRect.x * 2000 / col - 1000,
                    tfocusRect.y * 2000 / row - 1000,
                    (tfocusRect.x + tfocusRect.width) * 2000 / row - 1000,
                    (tfocusRect.y + tfocusRect.height) * 2000 / col - 1000);

            final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
            focusList.add(focusArea);
            Parameters para = mCamera.getParameters();
            para.setFocusAreas(focusList);
            para.setMeteringAreas(focusList);
            Log.d(TAG, "touchFocuserror:");
            mCamera.setParameters(para);
        }
        catch (Exception e)
        {
            Log.d(TAG, "touchFocuserror:" + e);
        }
        */
    }
    public void getfocusstaus(Activity main) {

        try {
            if (main != null)
                mCamera.autoFocus((Camera.AutoFocusCallback) main);
        }
        catch (Exception e)
        {

        }
    }

    public void setDisplayOrientation(int degrees,int row, int col)
    {

        Camera.Parameters parameters = mCamera.getParameters();
        if (degrees == 0) {
            parameters.setPreviewSize(col, row);
            mCamera.setDisplayOrientation(90);

        }
        if (degrees == 90) {
            parameters.setPreviewSize(row,col);
            mCamera.setDisplayOrientation(0);

        }
        if (degrees == 180) {
            parameters.setPreviewSize(col, row);
            mCamera.setDisplayOrientation(270);

        }
        if (degrees == 270) {
            parameters.setPreviewSize(row, col);
            mCamera.setDisplayOrientation(180);

        }
        mCamera.setParameters(parameters);
        enableView();
        mCamera.startPreview();

    }

    public void takePicture() {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = "111.jpg";
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+mPictureFileName);
            fos.write(data);
            fos.close();
        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
    }
}

class CameraSizeComparator implements Comparator<Camera.Size>
{
    public int compare(Camera.Size a, Camera.Size b)
    {
        return a.width-b.width;
    }
}