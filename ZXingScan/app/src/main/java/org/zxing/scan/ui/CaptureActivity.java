package org.zxing.scan.ui;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.zxing.scan.manager.ScanCallback;
import org.zxing.scan.manager.ScanManager;
import org.zxing.scan.manager.ZXing;
import org.zxing.scan.zxingscan.R;

public class CaptureActivity extends Activity implements ScanCallback {

    private SurfaceView mSurface;
    private ScanManager mCamerManager;
    private RelativeLayout scanCropView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        mSurface = (SurfaceView) findViewById(R.id.surface_view);

        mCamerManager = new ScanManager(this, this);

        scanCropView = (RelativeLayout) findViewById(R.id.search_layout);


        final ImageView scanLine = (ImageView) findViewById(R.id.imv_scan_line);

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.95f);
        animation.setDuration(4500);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamerManager.openDriver(mSurface.getHolder());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamerManager.stopPreview();
    }

    public ScanManager getCameraManager() {
        return mCamerManager;
    }

    public Rect getCropRect() {
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int x = location[0];
        int y = location[1];
        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        Rect rect = new Rect(x, y, x + cropWidth, y + cropHeight);

        return rect;
    }

    @Override
    public void onResult(ZXing zxing) {
    }
}
