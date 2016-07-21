package org.zxing.scan.zxingscan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import org.zxing.scan.manager.ZXing;
import org.zxing.scan.ui.CaptureActivity;

import java.io.ByteArrayOutputStream;

public class ScanActivity extends CaptureActivity {

    @Override
    public void onResult(ZXing zxing) {
        super.onResult(zxing);
        if (zxing.getCode() >= 0) {
            Intent intent = new Intent(this, ResultActivity.class);
            String result = zxing.getResult().getText();
            Bitmap bitmap = zxing.getBitmap();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
            Bundle bundle = new Bundle();
            bundle.putString("result", result);
            bundle.putByteArray("byte", out.toByteArray());
            intent.putExtras(bundle);
            startActivity(intent);
            this.finish();
        }
    }
}
