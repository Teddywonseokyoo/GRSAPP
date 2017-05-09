package com.exp.grs.grsphone;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2,View.OnTouchListener,Camera.AutoFocusCallback, GrsQueryToServer.GrsQueryListener
{
    private Mat img_input;
    private Mat img_result;
    private Mat img_crop_result;
    private static final String TAG = "GRSLOG(MainActivity)";
    private Activity m_mainactivity;
    //private CameraBridgeViewBase mOpenCvCameraView;

    private GrsCameraView mOpenCvCameraView;
    //private PortraitCameraView mOpenCvCameraView;
    public native int convertNativeLib(long matAddrInput, long matAddrResult, int process);
    public native long nativeCreateObject(String cascadeName, int minFaceSize);
    public native int nativeDetectObject(long matAddrInput, long matAddrResult, long matAddrCropResult);
    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS  = {"android.permission.CAMERA"};

    ImageView m_grsresultview;
    Bitmap m_grsbm;
    Handler m_handler = null;
    static boolean m_crop_result = false;


    ImageScanner scanner;
    TextView scanText;
    TextView valueText;
    String m_SerialNo="";
    TextToSpeech tts;


    int m_result_count = 0;
    boolean m_focus_status = false;
    private Handler autoFocusHandler;

    private List<Camera.Size> preview_size;
    private List<Size> focus_size;
    private Size win_size;
    private Rect win;
    private int row, col;

    GrsQueryToServer task;

    //layout
    FrameLayout m_grssplash;
    FrameLayout m_grsmain;
    private int currentApiVersion;

    private OrientationListener orientationListener;


    //button
    private ImageButton m_grsbackbtn;
    private ImageButton m_grsretakebtn;
    private RotateAnimation m_rotateanim90;
    private RotateAnimation m_rotateanim0;



    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Slash
        m_grssplash = (FrameLayout)findViewById(R.id.grssplash);
        m_grsmain =  (FrameLayout)findViewById(R.id.grsmain);
        openSplash();
        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {

            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                    {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility)
                        {
                            if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                            {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }

        //mOpenCvCameraView = (PortraitCameraView)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView = (GrsCameraView)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        orientationListener = new OrientationListener(this);

        Handler hsplash = new Handler();
        hsplash.postDelayed(new SplashThread(), 1000);


        //Button
        m_grsbackbtn = (ImageButton)findViewById(R.id.back_btn);
        m_grsretakebtn = (ImageButton)findViewById(R.id.retake_btn);
        m_rotateanim0 = (RotateAnimation) AnimationUtils.loadAnimation(this,R.anim.grsrotatebtn_0);
        m_rotateanim90 = (RotateAnimation) AnimationUtils.loadAnimation(this,R.anim.grsrotatebtn_90);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        m_mainactivity = this;
        m_grsresultview = (ImageView) findViewById(R.id.imageView);
        autoFocusHandler = new Handler();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
        scanText = (TextView)findViewById(R.id.scanText);
        valueText = (TextView)findViewById(R.id.valueText);
        task = new GrsQueryToServer(this);
        m_handler = new Handler()
        {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            public void handleMessage(Message msg)
            {
                scanText.setText("Serial No. " + m_SerialNo);
                m_grsresultview.setImageBitmap(m_grsbm);
                //send imageg
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                m_grsbm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                //send to server
                String jsonformdata ="{ \"userid\": \"freemanrws@hotmail.com\", \"dauthkey\": \""+m_SerialNo+"\" }";
                task.execute(new GrsPostdataValue("http://182.226.37.90:8083/api/req_grs2",jsonformdata,byteArray));
                try {
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File (sdCard.getAbsolutePath() + "/camtest");
                    dir.mkdirs();
                    String fileName = String.format("%d.jpg", System.currentTimeMillis());
                    File outFile = new File(dir, fileName);
                    FileOutputStream fos = new FileOutputStream(outFile);
                    fos.write(byteArray);
                    fos.flush();
                    fos.close();
                    Log.d(TAG, "onPictureTaken - wrote bytes: " + byteArray.length + " to " + outFile.getAbsolutePath());
                } catch (java.io.IOException e) {
                    Log.e("PictureDemo", "Exception in photoCallback", e);
                }
                ConvertTextToSpeech("Taken Picture");
            }
        };
    }


    @Override protected void onStart() {
        orientationListener.enable();
        super.onStart();
    }

    @Override protected void onStop() {
        orientationListener.disable();
        super.onStop();
    }





    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }

    class SplashThread implements Runnable{
        @Override
        public void run() {
            preview_size=mOpenCvCameraView.getPreviewSize();
            focus_size = new ArrayList<Size>();
            focus_size.add(new Size(16, 16));
            focus_size.add(new Size(24, 24));
            focus_size.add(new Size(32, 32));
            focus_size.add(new Size(48, 48));
            focus_size.add(new Size(64, 64));
            focus_size.add(new Size(96, 96));
            focus_size.add(new Size(128, 128));
            win_size=focus_size.get(2);
            win=new Rect();
            Log.d(TAG, "preview_size Result : "+preview_size.size());
            setPreviewSize(preview_size.get(preview_size.size()-1));
            mOpenCvCameraView.setOnTouchListener(MainActivity.this);

            if(tts == null) {
                tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            int result = tts.setLanguage(Locale.US);
                            if (result == TextToSpeech.LANG_MISSING_DATA ||
                                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("error", "This Language is not supported");
                            } else {
                                //ConvertTextToSpeech("Initilization OK");
                            }
                        } else
                            Log.e("error", "Initilization Failed!");
                    }
                });
            }
            closeSplash();
        }
    }

    private  void openSplash()
    {
        Log.d(TAG, "openSplash");
        m_grssplash.setVisibility(View.VISIBLE);
    }
    private  void closeSplash()
    {
        Log.d(TAG, "closeSplash");
        m_grssplash.setVisibility(View.GONE);
        m_grsmain.setVisibility(View.VISIBLE);
    }

    private boolean hasPermissions(String[] permissions) {
        int ret = 0;
        for (String perms : permissions){
            ret = checkCallingOrSelfPermission(perms);
            if (!(ret == PackageManager.PERMISSION_GRANTED)){
                return false;
            }
        }
        return true;
    }
    private void requestNecessaryPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    InputStream is = getResources().openRawResource(R.raw.gcascade1);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    File mCascadeFile = new File(cascadeDir, "gcascade1.xml");
                    FileOutputStream os = null;
                    try {
                        os = new FileOutputStream(mCascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    long result = nativeCreateObject(mCascadeFile.getAbsolutePath(), 0);
                    Log.d(TAG, "nativeCreateObject Result : "+result);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }

        }
    };
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean camreaAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if (!camreaAccepted  )
                        {
                            showDialogforPermission("To allow Permission");
                            return;
                        }else
                        {

                        }
                    }
                }
                break;
        }
    }
    private void showDialogforPermission(String msg) {
        final AlertDialog.Builder myDialog = new AlertDialog.Builder(  MainActivity.this);
        myDialog.setTitle("Notification");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }
            }
        });
        myDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        m_crop_result = false;

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    @Override
    public void onCameraViewStarted(int width, int height)
    {
        /*
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.d(TAG, "getWindowManager : " + rotation);
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        mOpenCvCameraView.setDisplayOrientation(degrees,row,col);
        */
        Log.d(TAG, "onCameraViewStarted ORGSIze width / height : "+width +" / "+ height);
        //closeSplash();
    }
    @Override
    public void onCameraViewStopped() {

        Log.d(TAG, "onCameraViewStopped");
        //openSplash();
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        img_input = inputFrame.rgba();
        img_result = new Mat();
        img_crop_result = new Mat();
        Log.d(TAG, "onCameraFrame ORGSIze width / height : "+img_input.width() +" / "+ img_input.height());
        m_result_count = nativeDetectObject(img_input.getNativeObjAddr(), img_result.getNativeObjAddr(), img_crop_result.getNativeObjAddr());
        Log.d(TAG, "convertNativeLib Result width / height / m_crop_result: "+img_crop_result.width() +" / "+ img_crop_result.height()+ " " + m_crop_result);
        if(m_result_count > 0 )
        {
            Log.d(TAG, "m_crop_result : "+ m_crop_result + "/  m_focus_status : " + m_focus_status);
            // convert to bitmap:
            if(m_crop_result == false ) {
                //barcode
                m_grsbm = Bitmap.createBitmap(img_crop_result.cols(), img_crop_result.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_crop_result, m_grsbm);
                int width = m_grsbm.getWidth();
                int height = m_grsbm.getHeight();
                int[] pixels = new int[width * height];
                m_grsbm.getPixels(pixels, 0, width, 0, 0, width, height);
                Image myImage = new Image(width, height, "RGB4");
                myImage.setData(pixels);
                int result = scanner.scanImage(myImage.convert("Y800"));
                Log.d(TAG, "Barcode Result : "+ result);
                if(result != 0)
                {
                    SymbolSet syms = scanner.getResults();
                    for (Symbol sym : syms) {
                        Log.d(TAG, "Barcode Result : "+sym.getData());
                        m_SerialNo = sym.getData();
                        //barcodeScanned = true;
                    }
                    Message message = m_handler.obtainMessage();
                    m_handler.sendMessage(message);
                    //mOpenCvCameraView.getfocusstaus(this);
                    m_crop_result = true;
                }
            }
            else
            {
                //이미지 검사로직
            }
        }
        Log.d(TAG, "convertNativeLib Result : "+m_result_count);
        return img_result;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        Log.d(TAG,"onTouch event");
        //mOpenCvCameraView.getfocusstaus(this);
        //Rect rect=new Rect((int)event.getX(), (int)event.getY(), (int)win_size.width, (int)win_size.height);
        //mOpenCvCameraView.touchFocus(rect, col, row);
        return false;
    }
    public void setPreviewSize(Camera.Size sz)
    {
        if (mOpenCvCameraView != null)
        {
            mOpenCvCameraView.disableView();
        }
        Log.d(TAG, "setPreviewSize height : "+sz.height);
        Log.d(TAG, "setPreviewSize width : "+sz.width);
        row=sz.height;
        col=sz.width;
        mOpenCvCameraView.getfocusstaus(this);
        //mImageSignature=new ImageSignature(row, col);
        mOpenCvCameraView.setMaxFrameSize(col, row);
        mOpenCvCameraView.enableView();

    }
    @Override
    public void onAutoFocus(boolean success, Camera camera)
    {
        Log.d(TAG, "onAutoFocus:" + success);
        autoFocusHandler.postDelayed(doAutoFocus, 1000);
        //m_focus_status =success;
        // mOpenCvCameraView.takePicture();
    }
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            //if (previewing)
            if(m_mainactivity != null)
                mOpenCvCameraView.getfocusstaus(m_mainactivity);
        }
    };

    @Override
    public void GrsServerResponse(String msg) {
        Log.d(TAG, "GrsServerResponse : " + msg);
        try {
            JSONObject reader = new JSONObject(msg);
            Log.d(TAG, "G VALUE : " + reader.getString("gaugeid"));
            valueText.setText("G VALUE : " + reader.getString("gaugeid"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void ConvertTextToSpeech(String text) {
        // TODO Auto-generated method stub
        if(text==null||"".equals(text))
        {
            text = "Content not available";
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }else
            tts.speak(text+"is saved", TextToSpeech.QUEUE_FLUSH, null);
    }

    private class OrientationListener extends OrientationEventListener {
        final int ROTATION_O    = 1;
        final int ROTATION_90   = 2;
        final int ROTATION_180  = 3;
        final int ROTATION_270  = 4;

        private int rotation = 0;
        public OrientationListener(Context context) { super(context); }

        @Override public void onOrientationChanged(int orientation) {
            if( (orientation < 35 || orientation > 325) && rotation!= ROTATION_O){ // PORTRAIT
                rotation = ROTATION_O;
                Log.d(TAG, "OrientationListener ROTATION_0");
                //m_grsbackbtn.startAnimation(m_rotateanim);
              //  menuButton.startAnimation(toPortAnim);
                m_grsbackbtn.startAnimation(m_rotateanim0);
                m_grsretakebtn.startAnimation(m_rotateanim0);
            }
            else if( orientation > 145 && orientation < 215 && rotation!=ROTATION_180){ // REVERSE PORTRAIT
                rotation = ROTATION_180;
                Log.d(TAG, "OrientationListener ROTATION_180");
              //  menuButton.startAnimation(toPortAnim);
            }
            else if(orientation > 55 && orientation < 125 && rotation!=ROTATION_270){ // REVERSE LANDSCAPE
                rotation = ROTATION_270;
                Log.d(TAG, "OrientationListener ROTATION_270");
               // menuButton.startAnimation(toLandAnim);
            }
            else if(orientation > 235 && orientation < 305 && rotation!=ROTATION_90){ //LANDSCAPE
                rotation = ROTATION_90;
                Log.d(TAG, "OrientationListener ROTATION_90");
                m_grsbackbtn.startAnimation(m_rotateanim90);
                m_grsretakebtn.startAnimation(m_rotateanim90);
            }
        }
    }


}
