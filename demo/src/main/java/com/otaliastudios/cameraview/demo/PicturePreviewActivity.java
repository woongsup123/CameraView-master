package com.otaliastudios.cameraview.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.otaliastudios.cameraview.CameraUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;


public class PicturePreviewActivity extends Activity {

    private static WeakReference<byte[]> image;

    public static void setImage(@Nullable byte[] im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        final ImageView imageView = findViewById(R.id.image);
        final MessageView typeMessageView = findViewById(R.id.type);
        final MessageView resultMessageView = findViewById(R.id.result);
        final MessageView metaMessageView = findViewById(R.id.meta);
        final String type = getIntent().getStringExtra("type");
        final String result = getIntent().getStringExtra("result");
        final String meta = getIntent().getStringExtra("meta");
        byte[] b = image == null ? null : image.get();
        if (b == null) {
            finish();
            return;
        }

        CameraUtils.decodeBitmap(b, 1000, 1000, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);

                // approxUncompressedSize.setTitle("Approx. uncompressed size");
                // approxUncompressedSize.setMessage(getApproximateFileMegabytes(bitmap) + "MB");
                resultMessageView.setVisibility(View.VISIBLE);
                metaMessageView.setVisibility(View.VISIBLE);
                typeMessageView.setTitle("이미지 종류");
                try {
                    JSONObject jsonMeta = new JSONObject(meta);

                    if (type.equals("NOTE")) {
                        String currency = jsonMeta.getString("currency");
                        String amount = jsonMeta.getString("amount");
                        String confidence = jsonMeta.getString("confidence");
                        typeMessageView.setMessage("지폐");
                        resultMessageView.setTitle("지폐 정보");
                        resultMessageView.setMessage(currency + " " + amount);
                        metaMessageView.setTitle("정확도");
                        metaMessageView.setMessage(confidence.substring(0, 4));


                    } else {
                        String type = jsonMeta.getString("type");
                        String code = jsonMeta.getString("code");
                        typeMessageView.setMessage("고지서");
                        resultMessageView.setTitle("고지서 종류");
                        metaMessageView.setTitle("고지서 정보");
                        if (type.equals("") || type.equals("null")) {
                            typeMessageView.setMessage("지원하지 않는 문서 양식입니다.");
                            resultMessageView.setVisibility(View.GONE);
                            metaMessageView.setVisibility(View.GONE);
                        } else
                        if (code.equals("") || code.equals("null")) {
                            resultMessageView.setMessage(type);
                            metaMessageView.setVisibility(View.GONE);
                        } else {
                            resultMessageView.setMessage(type);
                            metaMessageView.setMessage(code);
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // AspectRatio finalRatio = AspectRatio.of(bitmap.getWidth(), bitmap.getHeight());
                // actualResolution.setTitle("Actual resolution");
                // actualResolution.setMessage(bitmap.getWidth() + "x" + bitmap.getHeight() + " (" + finalRatio + ")");
            }
        });

    }

    private static float getApproximateFileMegabytes(Bitmap bitmap) {
        return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024 / 1024;
    }

}
