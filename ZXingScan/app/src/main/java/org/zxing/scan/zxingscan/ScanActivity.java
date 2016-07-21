package org.zxing.scan.zxingscan;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import org.zxing.scan.manager.ZXing;
import org.zxing.scan.ui.CaptureActivity;

import java.io.ByteArrayOutputStream;

public class ScanActivity extends CaptureActivity {

    private static final int REQUEST_CODE = 0; // 请求码
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WAKE_LOCK
    };
    private PermissionsChecker mPermissionsChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPermissionsChecker = new PermissionsChecker(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 缺少权限时, 进入权限配置页面
        if (mPermissionsChecker.lacksPermissions(PERMISSIONS)) {
            startPermissionsActivity();
        }
    }

    private void startPermissionsActivity() {
        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
        if (requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {
            finish();
        }
    }

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
