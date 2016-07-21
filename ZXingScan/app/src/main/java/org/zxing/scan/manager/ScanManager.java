package org.zxing.scan.manager;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager;

import org.zxing.scan.decode.DecodeManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class ScanManager implements Callback {

    private final String TAG = "Manager";
    private boolean DEBUG = false;

    private Camera mCamera;
    private boolean isPreview = false;
    private Config mConfig;
    private AutoFocus mAutofocus;
    private Context context;
    private ScanCallback mCallback;
    private DecodeManager mDecodeManager;

    public ScanManager(Context context, ScanCallback callback) {
        this.context = context;
        this.mCallback = callback;
        mDecodeManager = new DecodeManager(this, DecodeManager.DecodeMode.ALL);
    }

    public void openDriver(SurfaceHolder holder) {

        if (isOpen())
            return;

        holder.removeCallback(this);
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (DEBUG)
            Log.d(TAG, "surfaceCreated ");
        try {
            if (isOpen())
                return;
            mCamera = open();
            mCamera.setPreviewDisplay(holder);
            createConfig(mCamera);
            startPreview();

        } catch (IOException e) {
            relase();
            e.printStackTrace();
        }
    }

    private void createConfig(Camera camera) {
        if (mConfig == null)
            mConfig = new Config(context, camera);
        mConfig.setDesiredCameraParameters(camera);
    }

    private Camera open() {
        int cameraid = getCameraBackId();
        if (cameraid < 0) {
            return null;
        }
        return Camera.open(cameraid);
    }

    private int getCameraBackId() {
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras <= 0) {
            return -1;
        }
        int index = 0;
        while (index < numCameras) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                break;
            }
            index++;
        }
        return index;
    }

    private void relase() {
        if (mCamera != null) {
            isPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean isOpen() {
        return mCamera != null;
    }

    public ScanCallback getCamerCallback() {
        return mCallback;
    }

    public void setOneShotPreviewCallback() {
        if (isPreview)
            mCamera.setOneShotPreviewCallback(mDecodeManager);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (DEBUG)
            Log.d(TAG, "surfaceDestroyed");
        isPreview = false;
        holder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        relase();
    }

    public boolean isPreview() {
        return isPreview;
    }

    public Point getCameraResolution() {
        return mConfig.cameraResolution;
    }

    public Point getScreenResolution() {
        return mConfig.screenResolution;
    }

    public void startPreview() {
        Camera camera = mCamera;
        if (camera != null) {
            isPreview = true;
            camera.startPreview();
            mAutofocus = new AutoFocus(camera);
            mAutofocus.start();
            mDecodeManager.decode();
        }
    }

    public void stopPreview() {
        Camera camera = mCamera;

        if (mAutofocus != null) {
            mAutofocus.stop();
            mAutofocus = null;
            mDecodeManager.unsubscribe();
            mDecodeManager = null;
        }
        if (camera != null) {
            camera.stopPreview();
        }
        relase();

    }

    public Size getPreviewSize() {
        if (null != mCamera) {
            return mCamera.getParameters().getPreviewSize();
        }
        return null;
    }

    /**
     * 分辨率等基本信息
     *
     * @author TDD-08
     */
    private class Config {

        private static final int MIN_PREVIEW_PIXELS = 480 * 320;
        private static final double MAX_ASPECT_DISTORTION = 0.15;
        // 屏幕分辨率
        private Point screenResolution;
        // 相机分辨率
        private Point cameraResolution;

        public Config(Context context, Camera camera) {

            WindowManager manager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point theScreenResolution = new Point();
            theScreenResolution = getDisplaySize(display);

            screenResolution = theScreenResolution;

            /** 因为换成了竖屏显示，所以不替换屏幕宽高得出的预览图是变形的 */
            Point screenResolutionForCamera = new Point();
            screenResolutionForCamera.x = screenResolution.x;
            screenResolutionForCamera.y = screenResolution.y;

            if (screenResolution.x < screenResolution.y) {
                screenResolutionForCamera.x = screenResolution.y;
                screenResolutionForCamera.y = screenResolution.x;
            }

            Camera.Parameters parameters = camera.getParameters();
            cameraResolution = findBestPreviewSizeValue(parameters,
                    screenResolutionForCamera);

        }

        @SuppressWarnings("deprecation")
        private Point getDisplaySize(final Display display) {
            final Point point = new Point();
            try {
                display.getSize(point);
            } catch (NoSuchMethodError ignore) {
                point.x = display.getWidth();
                point.y = display.getHeight();
            }
            return point;
        }

        public void setDesiredCameraParameters(Camera camera) {
            Camera.Parameters parameters = camera.getParameters();

            if (parameters == null) {
                if (DEBUG)
                    Log.e(TAG, "parameters is null");
                return;
            }
            if (DEBUG)
                Log.w(TAG, "setPreviewSize cameraResolution.x "
                        + cameraResolution.x + " cameraResolution.y "
                        + cameraResolution.y);
            parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
            camera.setParameters(parameters);

            Camera.Parameters afterParameters = camera.getParameters();
            Size afterSize = afterParameters.getPreviewSize();
            if (afterSize != null
                    && (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize.height)) {
                cameraResolution.x = afterSize.width;
                cameraResolution.y = afterSize.height;
            }

            /** 设置相机预览为竖屏 */
            camera.setDisplayOrientation(90);
        }

        /**
         * 从相机支持的分辨率中计算出最适合的预览界面尺寸
         *
         * @param parameters
         * @param screenResolution
         * @return
         */
        private Point findBestPreviewSizeValue(Camera.Parameters parameters,
                                               Point screenResolution) {
            List<Size> rawSupportedSizes = parameters
                    .getSupportedPreviewSizes();
            if (rawSupportedSizes == null) {
                Size defaultSize = parameters.getPreviewSize();
                return new Point(defaultSize.width, defaultSize.height);
            }

            // Sort by size, descending
            List<Size> supportedPreviewSizes = new ArrayList<Size>(
                    rawSupportedSizes);
            Collections.sort(supportedPreviewSizes,
                    new Comparator<Size>() {
                        @Override
                        public int compare(Size a, Size b) {
                            int aPixels = a.height * a.width;
                            int bPixels = b.height * b.width;
                            if (bPixels < aPixels) {
                                return -1;
                            }
                            if (bPixels > aPixels) {
                                return 1;
                            }
                            return 0;
                        }
                    });

            double screenAspectRatio = (double) screenResolution.x
                    / (double) screenResolution.y;

            // Remove sizes that are unsuitable
            Iterator<Size> it = supportedPreviewSizes.iterator();
            while (it.hasNext()) {
                Size supportedPreviewSize = it.next();
                int realWidth = supportedPreviewSize.width;
                int realHeight = supportedPreviewSize.height;
                if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                    it.remove();
                    continue;
                }

                boolean isCandidatePortrait = realWidth < realHeight;
                int maybeFlippedWidth = isCandidatePortrait ? realHeight
                        : realWidth;
                int maybeFlippedHeight = isCandidatePortrait ? realWidth
                        : realHeight;

                double aspectRatio = (double) maybeFlippedWidth
                        / (double) maybeFlippedHeight;
                double distortion = Math.abs(aspectRatio - screenAspectRatio);
                if (distortion > MAX_ASPECT_DISTORTION) {
                    it.remove();
                    continue;
                }

                if (maybeFlippedWidth == screenResolution.x
                        && maybeFlippedHeight == screenResolution.y) {
                    Point exactPoint = new Point(realWidth, realHeight);
                    return exactPoint;
                }
            }

            // If no exact match, use largest preview size. This was not a great
            // idea on older devices because
            // of the additional computation needed. We're likely to get here on
            // newer Android 4+ devices, where
            // the CPU is much more powerful.
            if (!supportedPreviewSizes.isEmpty()) {
                Size largestPreview = supportedPreviewSizes.get(0);
                Point largestSize = new Point(largestPreview.width,
                        largestPreview.height);
                return largestSize;
            }

            // If there is nothing at all suitable, return current preview size
            Size defaultPreview = parameters.getPreviewSize();
            Point defaultSize = new Point(defaultPreview.width,
                    defaultPreview.height);

            return defaultSize;
        }

    }

    /**
     * 自动对焦，每3500毫秒对焦一次
     *
     * @author TDD-08
     */
    private class AutoFocus implements AutoFocusCallback {

        private static final long AUTO_FOCUS_INTERVAL_MS = 3500;

        private Camera mCamera;
        private boolean useAutoFocus;

        public AutoFocus(Camera camera) {
            this.mCamera = camera;
            String currentFocusMode = camera.getParameters().getFocusMode();
            useAutoFocus = Camera.Parameters.FOCUS_MODE_AUTO
                    .equals(currentFocusMode)
                    || Camera.Parameters.FOCUS_MODE_MACRO
                    .equals(currentFocusMode);
        }

        public void start() {
            if (useAutoFocus && isPreview)
                mCamera.autoFocus(this);
        }

        public void stop() {
            subscriber.unsubscribe();
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            doAutoTask(camera);
            if (DEBUG)
                Log.d(TAG, "onAutoFocus->" + success);
        }

        private void doAutoTask(final Camera camera) {

            Observable.create(new OnSubscribe<Void>() {

                @Override
                public void call(Subscriber<? super Void> arg0) {
                    arg0.onNext(null);
                }
            }).delay(AUTO_FOCUS_INTERVAL_MS, TimeUnit.MILLISECONDS)
                    .observeOn(Schedulers.io()).subscribe(subscriber);

        }

        private Subscriber<Void> subscriber = new Subscriber<Void>() {

            @Override
            public void onNext(Void arg0) {
                start();
            }

            @Override
            public void onError(Throwable arg0) {
                arg0.printStackTrace();
            }

            @Override
            public void onCompleted() {

            }
        };

    }

}
