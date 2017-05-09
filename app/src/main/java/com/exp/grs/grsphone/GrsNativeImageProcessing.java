package com.exp.grs.grsphone;

/**
 * Created by freem on 2016-12-19.
 */

public class GrsNativeImageProcessing {

    public native int convertNativeLib(long matAddrInput, long matAddrResult, int process);
    public native long nativeCreateObject(String cascadeName, int minFaceSize);
    public native int nativeDetectObject(long matAddrInput, long matAddrResult, long matAddrCropResult);



}




