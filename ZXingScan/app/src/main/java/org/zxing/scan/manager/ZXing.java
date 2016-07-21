package org.zxing.scan.manager;

import android.graphics.Bitmap;

import com.google.zxing.Result;

public class ZXing {

    private int code;
    private Result result;
    private Bitmap bitmap;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public String toString() {
        return "{code:" + code + ", result:" + result + ", bitmap:" + bitmap
                + "}";
    }

}
