#include <jni.h>
#include <string>
#include <vector>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <android/log.h>


using namespace cv;

#define LOG_TAG "ObjectDetection"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

extern "C" {

CascadeClassifier ccobject;

    int process(Mat img_input, Mat &img_result)
    {
        cvtColor( img_input, img_result, CV_RGBA2GRAY);
        return(0);
    }

    int detectAndDisplay( Mat img_input, Mat &img_result,Mat &img_crop_result)
    {
        std::vector<Rect> gasm;
        std::vector<int> rejectLevels;
        std::vector<double> levelWeights;
        Rect max(0,0,0,0) ;
        Mat frame_gray;
        cvtColor( img_input, frame_gray, CV_BGR2GRAY );
        equalizeHist( frame_gray, frame_gray );

        //-- Detect faces
        //ccobject.detectMultiScale( frame_gray, gasm,rejectLevels,levelWeights ,1.1, 1, 0, Size(360*2, 160*2),Size(360*4, 160*4), true);
        ccobject.detectMultiScale(frame_gray,gasm,1.1,3,0,Size(img_input.rows/3.5, img_input.cols/3.5));
        //CascadeClassifier::detectMultiScale( const Mat& image, vector<Rect>& objects, vector<int>& rejectLevels, vector<double>& levelWeights, double scaleFactor, int minNeighbors, int flags, Size minObjectSize, Size maxObjectSize, bool outputRejectLevels )
        for( size_t i = 0; i < gasm.size(); i++ )
        {
            /*
            if( gasm[i].x - 20 > 0 )
            {
                gasm[i].x = gasm[i].x - 20;
                gasm[i].width = gasm[i].width +20;
            }
            else
            {
                gasm[i].x = 0;
                gasm[i].width = gasm[i].width +gasm[i].x;
            }
            */
            rectangle(img_input,gasm[i],Scalar(0,0,255),3,8,0);
            if(max.width < gasm[i].width)
                max = gasm[i];
        }

        if(gasm.size() > 0) {
            img_crop_result = img_input(Rect(max.x+2, max.y+2, max.width-2, max.height-2));
            std::string strwight = "find object : " +
                                   static_cast<std::ostringstream *>( &(std::ostringstream() <<
                                                                        gasm.size()))->str() +
                                   " size object w/h :" +
                                   static_cast<std::ostringstream *>( &(std::ostringstream() <<
                                                                        gasm[0].width))->str() +
                                   " / " +
                                   static_cast<std::ostringstream *>( &(std::ostringstream() <<
                                                                        gasm[0].height))->str();
            cv::Point textpoint(10, 40);
            cv::putText(img_input, strwight, textpoint, 1, 1.1, Scalar(255, 0, 0));
        }
        img_result = img_input;
        return(gasm.size());
    }

    void SimplestCB(Mat& in, Mat& out, float percent) {
        assert(in.channels() == 3);
        assert(percent > 0 && percent < 100);
        float half_percent = percent / 200.0f;
        std::vector<Mat> tmpsplit; split(in,tmpsplit);
        for(int i=0;i<3;i++) {
            //find the low and high precentile values (based on the input percentile)
            Mat flat; tmpsplit[i].reshape(1,1).copyTo(flat);
            cv::sort(flat,flat,CV_SORT_EVERY_ROW + CV_SORT_ASCENDING);
            int lowval = flat.at<uchar>(cvFloor(((float)flat.cols) * half_percent));
            int highval = flat.at<uchar>(cvCeil(((float)flat.cols) * (1.0 - half_percent)));
            //cout << lowval << " " << highval << endl;
            //saturate below the low percentile and above the high percentile
            tmpsplit[i].setTo(lowval,tmpsplit[i] < lowval);
            tmpsplit[i].setTo(highval,tmpsplit[i] > highval);
            //scale the channel
            normalize(tmpsplit[i],tmpsplit[i],0,255,NORM_MINMAX);
        }
        merge(tmpsplit,out);
    }




    JNIEXPORT jint JNICALL Java_com_exp_grs_grsphone_MainActivity_convertNativeLib(JNIEnv*, jobject, jlong addrInput, jlong addrResult,jint command)
    {
        Mat &img_input = *(Mat *) addrInput;
        Mat &img_result = *(Mat *) addrResult;
        int conv = -1;
        if (command == 1)
        {
            conv = process(img_input, img_result);
        }

        int ret = (jint) conv;
        return ret;
    }
    JNIEXPORT jlong JNICALL Java_com_exp_grs_grsphone_MainActivity_nativeCreateObject(JNIEnv* jenv, jobject, jstring jFileName, jint faceSize)
    {
        const char* jnamestr = jenv->GetStringUTFChars(jFileName, NULL);
        String object_cascade_name(jnamestr);
        jlong  result = 6;

        if( !ccobject.load( object_cascade_name ) )
        {
            result = 3;
        }
        LOGD("Java_org_opencv_samples_facedetect_DetectionBasedTracker_nativeCreateObject");

        return result;
    }

    JNIEXPORT jint JNICALL Java_com_exp_grs_grsphone_MainActivity_nativeDetectObject(JNIEnv*, jobject, jlong addrInput, jlong addrResult, jlong addrCropResult)
    {
        int ret = 1;
        Mat &img_input = *(Mat *) addrInput;
        Mat &img_result = *(Mat *) addrResult;
        Mat &img_crop_result = *(Mat *) addrCropResult;
        ret = detectAndDisplay( img_input, img_result, img_crop_result);
        return (jint) ret;
    }


    JNIEXPORT jint JNICALL Java_com_exp_grs_grsphone_GrsNativeImageProcessing_convertNativeLib(JNIEnv *env, jobject instance,
                                                                    jlong matAddrInput,
                                                                    jlong matAddrResult,
                                                                    jint process)
    {


    }
    JNIEXPORT jlong JNICALL Java_com_exp_grs_grsphone_GrsNativeImageProcessing_nativeCreateObject(JNIEnv *env, jobject instance,
                                                                      jstring cascadeName_,
                                                                      jint minFaceSize)
    {
        const char *cascadeName = env->GetStringUTFChars(cascadeName_, 0);
        // TODO

        env->ReleaseStringUTFChars(cascadeName_, cascadeName);
    }
    JNIEXPORT jint JNICALL Java_com_exp_grs_grsphone_GrsNativeImageProcessing_nativeDetectObject(JNIEnv *env, jobject instance,
                                                                      jlong matAddrInput,
                                                                      jlong matAddrResult,
                                                                      jlong matAddrCropResult)
    {
    // TODO
    }
}
