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
        final MessageView imageClassMessageView = findViewById(R.id.image_class);
        final MessageView typeMessageView = findViewById(R.id.type);
        final MessageView codeMessageView = findViewById(R.id.code);
        final MessageView userCodeMessageView = findViewById(R.id.user_code);
        final MessageView confidenceMessageView = findViewById(R.id.confidence);
        imageClassMessageView.setVisibility(View.GONE);
        typeMessageView.setVisibility(View.GONE);
        codeMessageView.setVisibility(View.GONE);
        userCodeMessageView.setVisibility(View.GONE);
        typeMessageView.setVisibility(View.GONE);
        confidenceMessageView.setVisibility(View.GONE);
        final String type = getIntent().getStringExtra("type");
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


                try {
                    JSONObject jsonMeta = new JSONObject(meta);

                    if (type.equals("NOTE")) {
                        imageClassMessageView.setVisibility(View.VISIBLE);
                        typeMessageView.setVisibility(View.VISIBLE);
                        confidenceMessageView.setVisibility(View.VISIBLE);
                        imageClassMessageView.setTitle("이미지 종류");
                        String currency = jsonMeta.getString("currency");
                        String amount = Integer.toString(jsonMeta.getInt("amount"));
                        String confidenceLevel = Double.toString(
                                jsonMeta.getDouble("confidence_level")
                        );
                        imageClassMessageView.setMessage("지폐");
                        typeMessageView.setTitle("지폐 정보");
                        typeMessageView.setMessage(currency + " " + amount);
                        confidenceMessageView.setTitle("정확도");
                        confidenceMessageView.setMessage(confidenceLevel.substring(0, 4));


                    } else if (type.equals("BILL")){
                        imageClassMessageView.setVisibility(View.VISIBLE);
                        typeMessageView.setVisibility(View.VISIBLE);
                        codeMessageView.setVisibility(View.VISIBLE);
                        userCodeMessageView.setVisibility(View.VISIBLE);
                        confidenceMessageView.setVisibility(View.VISIBLE);
                        String type = jsonMeta.getString("type");
                        String code = jsonMeta.getString("code");
                        String confidenceLevel = Double.toString(
                                jsonMeta.getDouble("confidence_level")
                        );
                        String userCode;
                        try{
                            userCode = jsonMeta.getString("user_code");
                        } catch (JSONException e){
                            userCode = "";
                        }
                        imageClassMessageView.setTitle("이미지 종류");
                        imageClassMessageView.setMessage("고지서");

                        typeMessageView.setTitle("고지서 종류");
                        userCodeMessageView.setTitle("분류");
                        codeMessageView.setTitle("코드");
                        confidenceMessageView.setTitle("정확도");

                        if (type.equals("") || type.equals("null")) {
                            imageClassMessageView.setMessage("지원하지 않는 문서 양식입니다.");
                            codeMessageView.setVisibility(View.GONE);
                            userCodeMessageView.setVisibility(View.GONE);
                            typeMessageView.setVisibility(View.GONE);
                            confidenceMessageView.setVisibility(View.GONE);
                        } else if (code.equals("") || code.equals("null")) {
                            typeMessageView.setMessage(type);
                            userCodeMessageView.setVisibility(View.GONE);
                            typeMessageView.setVisibility(View.GONE);
                            confidenceMessageView.setVisibility(View.GONE);
                        } else {
                            codeMessageView.setMessage(code);
                            if (userCode.equals("") || userCode.equals("null")) {
                                userCodeMessageView.setVisibility(View.GONE);
                            }
                            else{
                                userCodeMessageView.setMessage(userCode);
                            }
                            typeMessageView.setMessage(type);
                            confidenceMessageView.setMessage(confidenceLevel);
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
