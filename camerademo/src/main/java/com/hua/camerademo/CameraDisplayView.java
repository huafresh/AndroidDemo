package com.hua.camerademo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.view.OrientationEventListener.ORIENTATION_UNKNOWN;

/**
 * @author zhangsh
 * @version V1.0
 * @date 2020-02-19 16:43
 */

public class CameraDisplayView extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mRotation = 0;

    public CameraDisplayView(Context context) {
        this(context, null);
    }

    public CameraDisplayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraDisplayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "");
        setupCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "holder = " + holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "");
        releaseCamera();
    }

    private void setupCamera(SurfaceHolder holder) {
        try {
            mCamera = Camera.open(mCameraId);
            mCamera.setPreviewDisplay(holder);
            setCameraDisplayOrientation(((Activity) getContext()), mCameraId, mCamera);
            printSupportedSize();
            Camera.Parameters parameters = mCamera.getParameters();
            resizeSurfaceSizeToFitPreviewSize(parameters);
            setPictureSize(parameters);
            // mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printSupportedSize() {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size supportedPictureSize : supportedPreviewSizes) {
            Log.d("@@@hua", String.format("support preview size[%s:%s]", supportedPictureSize.width, supportedPictureSize.height));
        }
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        for (Camera.Size supportedPictureSize2 : supportedPictureSizes) {
            Log.d("@@@hua", String.format("support picture size[%s:%s]", supportedPictureSize2.width, supportedPictureSize2.height));
        }
    }

    private void resizeSurfaceSizeToFitPreviewSize(Camera.Parameters parameters) {
        Camera.Size size = findProperSize(parameters.getSupportedPreviewSizes(), getHeight(), getWidth());
        if (size != null) {
            parameters.setPreviewSize(size.width, size.height);
            int previewRatio = size.width / size.height;
            int surfaceRatio = getHeight() / getWidth();
            int newWidth = getWidth();
            int newHeight = getHeight();
            if (previewRatio > surfaceRatio) {
                // 此时surface比例要变大，要不增大分子，要不减少分母，因为surfaceView本身已经
                // match_parent了，所以不能再增大了，故需要固定分子，减少分母；
                newWidth = getWidth() / previewRatio;
            } else if (previewRatio < surfaceRatio) {
                // 反之即可
                newHeight = getHeight() * previewRatio;
            }
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.width = newWidth;
            layoutParams.height = newHeight;
            setLayoutParams(layoutParams);
        }
    }

    private void setPictureSize(Camera.Parameters parameters) {
        Camera.Size size = findProperSize(parameters.getSupportedPictureSizes(), getHeight(), getWidth());
        if (size != null) {
            parameters.setPictureSize(size.width, size.height);
        }
    }

    /**
     * 找出最合适的尺寸，规则如下：
     * 1.将尺寸按比例分组，找出比例最接近屏幕比例的尺寸组
     * 2.在比例最接近的尺寸组中找出最接近屏幕尺寸且大于屏幕尺寸的尺寸
     * 3.如果没有找到，则忽略2中第二个条件再找一遍，应该是最合适的尺寸了
     */
    private static Camera.Size findProperSize(List<Camera.Size> sizeList, int expectWidth, int expectHeight) {
        if (expectWidth <= 0 || expectHeight <= 0 || sizeList == null) {
            return null;
        }

        List<List<Camera.Size>> ratioListList = new ArrayList<>();
        for (Camera.Size size : sizeList) {
            addRatioList(ratioListList, size);
        }

        final float surfaceRatio = (float) expectWidth / expectHeight;
        List<Camera.Size> bestRatioList = null;
        float ratioDiff = Float.MAX_VALUE;
        for (List<Camera.Size> ratioList : ratioListList) {
            float ratio = (float) ratioList.get(0).width / ratioList.get(0).height;
            float newRatioDiff = Math.abs(ratio - surfaceRatio);
            if (newRatioDiff < ratioDiff) {
                bestRatioList = ratioList;
                ratioDiff = newRatioDiff;
            }
        }

        Camera.Size bestSize = null;
        int diff = Integer.MAX_VALUE;
        assert bestRatioList != null;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - expectWidth) + Math.abs(size.height - expectHeight);
            if (size.height >= expectHeight && newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        if (bestSize != null) {
            return bestSize;
        }

        diff = Integer.MAX_VALUE;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - expectWidth) + Math.abs(size.height - expectHeight);
            if (newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        return bestSize;
    }

    private static void addRatioList(List<List<Camera.Size>> ratioListList, Camera.Size size) {
        float ratio = (float) size.width / size.height;
        for (List<Camera.Size> ratioList : ratioListList) {
            float mine = (float) ratioList.get(0).width / ratioList.get(0).height;
            if (ratio == mine) {
                ratioList.add(size);
                return;
            }
        }

        List<Camera.Size> ratioList = new ArrayList<>();
        ratioList.add(size);
        ratioListList.add(ratioList);
    }


    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mCameraInfo = null;
        mRotation = 0;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "");
    }

    public void takePicture(PictureCallback pictureCallback) {
        if (mCamera != null) {
            mCamera.takePicture(new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                    Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "");
                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "raw");

                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "post view");

                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Log.e("@@@hua", Thread.currentThread().getStackTrace()[2].getMethodName() + ":" + "jpeg");
                    if (pictureCallback != null) {
                        try {
                            InputStream dataStream = new ByteArrayInputStream(data);
                            ExifInterface exifInterface = new ExifInterface(dataStream);
                            int rotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            if (rotation != 0) {
                                // 需要旋转
                                Matrix matrix = new Matrix();
                                matrix.postRotate(90);
                                Bitmap rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                pictureCallback.onPictureTaken(rotateBitmap);
                            } else {
                                pictureCallback.onPictureTaken(bitmap);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mCamera.startPreview();
                }
            });
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
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
            default:
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            // compensate the mirror
            result = (360 - result) % 360;
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    public void onOrientationChanged(int orientation) {
        if (orientation == ORIENTATION_UNKNOWN || mCamera == null) {
            return;
        }
        Camera.CameraInfo info = getCameraInfo();
        orientation = (orientation + 45) / 90 * 90;
        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }
        if (rotation != mRotation) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(rotation);
            mCamera.setParameters(parameters);
        }
    }

    private Camera.CameraInfo getCameraInfo() {
        if (mCameraInfo == null) {
            mCameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, mCameraInfo);
        }
        return mCameraInfo;
    }

    public void switchCamera() {
        int newId;
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            newId = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            newId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        mCameraId = newId;
        releaseCamera();
        setupCamera(getHolder());
    }


    public interface PictureCallback {
        void onPictureTaken(Bitmap bitmap);
    }
}
