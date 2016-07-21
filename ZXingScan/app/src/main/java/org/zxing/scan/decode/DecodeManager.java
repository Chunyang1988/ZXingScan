package org.zxing.scan.decode;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.zxing.scan.manager.ScanCallback;
import org.zxing.scan.manager.ScanManager;
import org.zxing.scan.manager.ZXing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;

public class DecodeManager extends Thread implements PreviewCallback {

    public static enum DecodeMode {

        BARCODE, QRCODE, ALL

    }

    private static class DecodeHandler extends Handler {

    }

    public static final String BARCODE_BITMAP = "barcode_bitmap";

    private MultiFormatReader multiFormatReader;
    private ScanManager mScanManager;
    private DecodeHandler mHandler;
    private boolean isRuning = false;

    // private CaptureActivity activity;

    public DecodeManager(ScanManager manager, DecodeMode decodeMode) {
        this.mScanManager = manager;
        init(decodeMode);
    }

    private void init(DecodeMode decodeMode) {

        EnumMap<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(
                DecodeHintType.class);
        Collection<BarcodeFormat> decodeFormats = new ArrayList<BarcodeFormat>();
        decodeFormats.addAll(EnumSet.of(BarcodeFormat.AZTEC));
        decodeFormats.addAll(EnumSet.of(BarcodeFormat.PDF_417));

        switch (decodeMode) {
            case BARCODE:
                decodeFormats.addAll(DecodeFormatManager.getBarCodeFormats());
                break;

            case QRCODE:
                decodeFormats.addAll(DecodeFormatManager.getQrCodeFormats());
                break;

            case ALL:
                decodeFormats.addAll(DecodeFormatManager.getBarCodeFormats());
                decodeFormats.addAll(DecodeFormatManager.getQrCodeFormats());
                break;

            default:
                break;
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
    }

    public void decode() {
        if (isRuning) {
            mScanManager.setOneShotPreviewCallback();
        } else
            start();
    }

    public void unsubscribe() {
        if (isRuning) {
            // Looper.myLooper().quit();
            isRuning = false;
        }
    }

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        isRuning = true;
        mHandler = new DecodeHandler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                final ZXing zxing = decode((byte[]) msg.obj, msg.arg1, msg.arg2);
                if (zxing.getCode() < 0) {
                    mScanManager.setOneShotPreviewCallback();
                } else {
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        Handler mainThread = new Handler(Looper.getMainLooper());
                        mainThread.post(new Runnable() {

                            public void run() {
                                mScanManager.getCamerCallback().onResult(
                                        zxing);
//                                mScanManager.setOneShotPreviewCallback();
                            }
                        });
                        return;
                    }

                }
            }
        };
        mScanManager.setOneShotPreviewCallback();
        Looper.loop();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!mScanManager.isPreview())
            return;
        Point cameraResolution = mScanManager.getCameraResolution();
        Message mess = mHandler.obtainMessage();
        mess.obj = data;
        mess.arg1 = cameraResolution.x;
        mess.arg2 = cameraResolution.y;
        mess.sendToTarget();
    }

    private ZXing decode(byte[] data, int width, int height) {
        Size size = mScanManager.getPreviewSize();

        // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++)
                rotatedData[x * size.height + size.height - y - 1] = data[x + y
                        * size.width];
        }

        // 宽高也要调整
        int tmp = size.width;
        size.width = size.height;
        size.height = tmp;

        Result rawResult = null;
        PlanarYUVLuminanceSource source = buildLuminanceSource(rotatedData,
                size.width, size.height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }

        ZXing zxing = new ZXing();

        if (rawResult != null) {
            zxing.setCode(0);
            zxing.setResult(rawResult);
            zxing.setBitmap(bundleThumbnail(source));
        } else {
            zxing.setCode(-1);
        }

        Log.e(BARCODE_BITMAP, zxing.toString());
        return zxing;
    }

    private static Bitmap bundleThumbnail(PlanarYUVLuminanceSource source) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height,
                Bitmap.Config.ARGB_8888);
        return bitmap;
        // ByteArrayOutputStream out = new ByteArrayOutputStream();
        // bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        // return out.toByteArray();
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on
     * the format of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
                                                         int width, int height) {
        ScanCallback callback = mScanManager.getCamerCallback();
        if (callback == null)
            return null;
        Rect rect = callback.getCropRect();
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left,
                rect.top, rect.width(), rect.height(), false);
    }
}
