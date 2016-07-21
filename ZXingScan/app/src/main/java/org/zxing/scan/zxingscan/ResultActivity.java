package org.zxing.scan.zxingscan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {


    private TextView mTextView;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        mTextView = (TextView) findViewById(R.id.tv_result);
        mImageView = (ImageView) findViewById(R.id.imv_result);

        Bundle bundle = getIntent().getExtras();
        String result = bundle.getString("result");
        mTextView.setText(result);

        byte[] bytes = bundle.getByteArray("byte");
        Bitmap barcode = null;
        if (bytes != null) {
            barcode = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            barcode = barcode.copy(Bitmap.Config.RGB_565, true);
        }

        mImageView.setImageBitmap(barcode);


    }
}
