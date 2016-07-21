package org.zxing.scan.manager;

import android.graphics.Rect;


public interface ScanCallback {

    public Rect getCropRect();

    public void onResult(ZXing zxing);

}
