package com.otaliastudios.cameraview.demo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

    private CameraView camera;

    private boolean mCapturingPicture;

    // To show stuff in the callback
    private static String type = "";
    private static String meta = "";
    private long mCaptureTime;

    private final int THICKNESS = 30;

    private double frameXHana = 0.25;
    private double frameYHana = 0.1;

    private double frameXDBLife = 0.1;
    private double frameYDBLife = 0.15;

    private final int reducedWidth = 1920;
    private static int reducedHeight = 0;
    //private final int cropX = frameX+THICKNESS;
    //private final int cropY = frameY-THICKNESS;
    private static int frameWidth = 0;
    private static int frameHeight = 0;
    private static int rootWidth = 0;
    private static int rootHeight = 0;

    private String PURPOSE = "Hana"; //Hana or DBLife, crop

    private final String IPADDR = "125.132.250.244";
    private final String DIR = "/api/pilot/upload/";
    private final int PORT = 801;

//    private final String IPADDR = "10.122.64.248";
//    private final String DIR = "/api/pilot/upload/";
//    private final int PORT = 8000;

    final int CONTEXT_MENU_HANA = 1;
    final int CONTEXT_MENU_DBLIFE = 2;
    final int CONTEXT_MENU_CROPHANA = 3;
    final int CONTEXT_MENU_CROPDB = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_camera);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        camera = findViewById(R.id.camera);
        camera.addCameraListener(new CameraListener() {
            public void onCameraOpened(CameraOptions options) {
                //onOpened();
                if (PURPOSE.equals("Hana")) {
                    createOverlay(frameXHana, frameYHana);
                }
                else if (PURPOSE.equals("DBLife")) {
                    createOverlayA4(frameXDBLife);
                }
            }
            public void onPictureTaken(byte[] jpeg) {

                savePicture(jpeg, "original");
                //byte[] croppedImg = cropPicture(jpeg,
                //                                    frameX,
                //                                    frameY,
                //                                    rootWidth*(1-frameX*2) / rootWidth,
                //                                    rootHeight*(1-frameY*2) / rootHeight);
                if (PURPOSE.contains("Hana")) {
                    jpeg = resizePicture(jpeg);
                }
                //savePicture(resizedImg, "resized");
                final byte[] finalJpeg = jpeg;
                Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendPicture(finalJpeg);
                    }
                });
                th.start();
            }


        });

        findViewById(R.id.capturePhoto).setOnClickListener(this);
        findViewById(R.id.edit).setOnClickListener(this);
    }

    private void sendPicture(byte[] jpeg) {
        type ="";
        meta="";
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://"+IPADDR+":"+Integer.toString(PORT)+DIR);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setCharset(Charset.forName("UTF-8"))
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] scaledJpeg = baos.toByteArray();

        if(PURPOSE.contains("DB")) {

            ContentBody cb = new ByteArrayBody(jpeg, "pic.jpg");
            builder.addPart("content", cb);

            builder.addTextBody("purpose", PURPOSE);
            builder.addTextBody("width", Integer.toString(bitmap.getHeight()));
            builder.addTextBody("height", Integer.toString(bitmap.getWidth()));
            //int bmHeight = bitmap.getHeight();
            //int bmWidth = bitmap.getWidth();
            //double ratio = ((double) rootHeight / rootWidth) * ((double) bitmap.getHeight() / bitmap.getWidth());
            //ratio = 1.8;
            builder.addTextBody("ref_vertex_x", Double.toString(frameXDBLife));
            builder.addTextBody("ref_vertex_y", Double.toString(frameYDBLife));
            builder.addTextBody("symm_crop", "True");
        }
        else if (PURPOSE.contains("Hana")){

            ContentBody cb = new ByteArrayBody(scaledJpeg, "pic.jpg");
            builder.addPart("content", cb);

            builder.addTextBody("purpose", PURPOSE);
            builder.addTextBody("width", Integer.toString(scaledBitmap.getWidth()));
            builder.addTextBody("height", Integer.toString(scaledBitmap.getHeight()));
            builder.addTextBody("ref_vertex_x", Double.toString(frameYHana));
            builder.addTextBody("ref_vertex_y", Double.toString(frameXHana));
            builder.addTextBody("symm_crop", "False");
        }

        try {
            httpPost.setEntity(builder.build());
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpEntity != null){
                //InputStream is = httpEntity.getContent();
                String content = EntityUtils.toString(httpEntity);
                JSONObject jsonObject = new JSONObject(content);
                if (PURPOSE.contains("Crop") || PURPOSE.contains("DBLife")){
                    String imageString = jsonObject.getString("image");
                    byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
                    Bitmap decoded = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                    decoded.compress(Bitmap.CompressFormat.JPEG, 100, baos2);
                    byte[] byteImg = baos2.toByteArray();
                    type = "";
                    onPicture(byteImg);

                }
                else{

                    type = jsonObject.getString("type");
                    meta = jsonObject.getString("meta");
                    //JSONObject metaObject = new JSONObject(meta);
                    //meta = metaObject.getString("meta");
                    onPicture(scaledJpeg);
                }

            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Error e){
            e.printStackTrace();
        }
    }

    private byte[] resizePicture(byte[] jpeg) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        int bmWidth = bitmap.getWidth();
        int bmHeight = bitmap.getHeight();
        float reduceRatio = (float) 1.0;
        if (reducedWidth < bmWidth) {
            reduceRatio = (float) reducedWidth / (float) bmWidth;
        }
        reducedHeight = (int) (bmHeight*reduceRatio);
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, reducedWidth, reducedHeight, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteImg = baos.toByteArray();
        newBitmap.recycle();
        return byteImg;
    }

    private void createOverlayA4(double frameX) {
        ImageView overlayTop = findViewById(R.id.top);
        ImageView overlayLeft = findViewById(R.id.left);
        ImageView overlayRight = findViewById(R.id.right);
        ImageView overlayBottom = findViewById(R.id.bottom);
        CoordinatorLayout root = findViewById(R.id.root);
        int width = root.getWidth();
        int height = root.getHeight();

        rootWidth = width;
        rootHeight = height;

        frameWidth = (int)(width * (1.0 - frameX*2));
        frameHeight = (int)(frameWidth*1.414);
        frameYDBLife = (rootHeight - frameHeight) / 2.0 / rootHeight;
        CoordinatorLayout.LayoutParams topLP = new CoordinatorLayout.LayoutParams(frameWidth, THICKNESS);
        topLP.setMargins((int)(width*frameX), (int)(height*frameYDBLife-THICKNESS), 0, 0);
        overlayTop.setLayoutParams(topLP);

        CoordinatorLayout.LayoutParams bottomLP = new CoordinatorLayout.LayoutParams(frameWidth, THICKNESS);
        bottomLP.setMargins((int)(width*frameX), (int)(height - height*frameYDBLife), 0, 0);
        overlayBottom.setLayoutParams(bottomLP);

        CoordinatorLayout.LayoutParams leftLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS*2);
        leftLP.setMargins((int)(width*frameX-THICKNESS), (int)(height*frameYDBLife-THICKNESS), 0, 0);
        overlayLeft.setLayoutParams(leftLP);

        CoordinatorLayout.LayoutParams rightLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS*2);
        rightLP.setMargins((int)(width*frameX) + frameWidth, (int)(height*frameYDBLife-THICKNESS), 0, 0);
        overlayRight.setLayoutParams(rightLP);
    }

    private void createOverlay(double frameX, double frameY) {
        ImageView overlayTop = findViewById(R.id.top);
        ImageView overlayLeft = findViewById(R.id.left);
        ImageView overlayRight = findViewById(R.id.right);
        ImageView overlayBottom = findViewById(R.id.bottom);

        CoordinatorLayout root = findViewById(R.id.root);
        int width = root.getWidth();
        int height = root.getHeight();

        rootWidth = width;
        rootHeight = height;

        frameWidth = (int)(width * (1.0 - frameX*2));
        frameHeight = (int)(height * (1.0 - frameY*2));

        CoordinatorLayout.LayoutParams topLP = new CoordinatorLayout.LayoutParams(frameWidth, THICKNESS);
        topLP.setMargins((int)(width*frameX), (int)(height*frameY-THICKNESS), 0, 0);
        overlayTop.setLayoutParams(topLP);

        CoordinatorLayout.LayoutParams bottomLP = new CoordinatorLayout.LayoutParams(0, 0);
        overlayBottom.setLayoutParams(bottomLP);

        CoordinatorLayout.LayoutParams leftLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS);
        leftLP.setMargins((int)(width*frameX-THICKNESS), (int)(height*frameY-THICKNESS), 0, 0);
        overlayLeft.setLayoutParams(leftLP);

        CoordinatorLayout.LayoutParams rightLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS);
        rightLP.setMargins((int)(width*frameX) + frameWidth, (int)(height*frameY-THICKNESS), 0, 0);
        overlayRight.setLayoutParams(rightLP);
    }

    private void message(String content, boolean important) {
        int length = important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, content, length).show();
    }


    private void onPicture(byte[] jpeg) {
        mCapturingPicture = false;

        PicturePreviewActivity.setImage(jpeg);
        Intent intent = new Intent(CameraActivity.this, PicturePreviewActivity.class);

        intent.putExtra("type", type);
        intent.putExtra("result", "");
        intent.putExtra("meta", meta);
        //intent.putExtra("nativeHeight", mCaptureNativeSize.getHeight());
        startActivity(intent);
        //AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle("인식결과: OCR 일반").setMessage("<5422751+ +00024500109998180115+ +38001< <11<").show();
        //mCaptureTime = 0;
        //mCaptureNativeSize = null;
    }

    private void savePicture(byte[] jpeg, String filename) {
        File storeDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "hana");

        File file = createFile(storeDir, filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jpeg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createFile(File storeDir, String filename) {
        if (!storeDir.exists()) {
            storeDir.mkdirs();
        }
        long time = System.currentTimeMillis();
        File file = new File(storeDir.getPath() + File.separator + filename + "_" + Long.toString(time) + ".jpg");
        return file;
    }

    private byte[] cropPicture (byte[] jpeg, double startXRatio, double startYRatio, double widthRatio, double heightRatio){
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        //Matrix matrix = new Matrix();
        //matrix.postRotate(90);
        //bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        int bmWidth = bitmap.getWidth();
        int bmHeight = bitmap.getHeight();

        int bmStartX = (int) (bmWidth * startYRatio);
        int bmStartY = (int) (bmHeight * startXRatio);

        int bmCropWidth = (int) (bmWidth * heightRatio);
        int bmCropHeight = (int) (bmHeight * widthRatio);

        Bitmap croppedImg = Bitmap.createBitmap(bitmap, bmStartX, bmStartY, bmCropWidth, bmCropHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        croppedImg.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteImg = baos.toByteArray();
        croppedImg.recycle();
        return byteImg;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.capturePhoto: capturePhoto(); break;
            case R.id.edit:
                registerForContextMenu(view);
                openContextMenu(view);
                break;
        }
    }

    @Override
    public void onBackPressed() {
//        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
//        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
//            b.setState(BottomSheetBehavior.STATE_HIDDEN);
//            return;
//        }
        super.onBackPressed();
    }

    private void capturePhoto() {
        if (mCapturingPicture) return;
        mCapturingPicture = true;
        message("Capturing picture...", false);
        camera.capturePicture();
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        menu.setHeaderTitle("Choose Mode");
        menu.add(Menu.NONE, CONTEXT_MENU_HANA, Menu.NONE, "고지서 / 지폐");
        menu.add(Menu.NONE, CONTEXT_MENU_DBLIFE, Menu.NONE, "동의서");
        menu.add(Menu.NONE, CONTEXT_MENU_CROPHANA, Menu.NONE, "자르기 (하나)");
        menu.add(Menu.NONE, CONTEXT_MENU_CROPDB, Menu.NONE, "자르기 (DB)");
    }

    @Override
    public boolean onContextItemSelected (MenuItem item){
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
            case CONTEXT_MENU_HANA: {
                PURPOSE = "Hana";
                createOverlay(frameXHana, frameYHana);
            }
            break;
            case CONTEXT_MENU_DBLIFE: {
                PURPOSE = "DBLife";
                createOverlayA4(frameXDBLife);
            }
            break;
            case CONTEXT_MENU_CROPHANA: {
                PURPOSE = "CropHana";
                createOverlay(frameXHana, frameYHana);
            }
            break;
            case CONTEXT_MENU_CROPDB: {
                PURPOSE = "CropDB";
                createOverlayA4(frameXDBLife);
            }
            break;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onValueChanged(Control control, Object value, String name) {
        if (!camera.isHardwareAccelerated() && (control == Control.WIDTH || control == Control.HEIGHT)) {
            if ((Integer) value > 0) {
                message("This device does not support hardware acceleration. " +
                        "In this case you can not change width or height. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        control.applyValue(camera, value);
        return true;
    }

    //region Boilerplate

    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isStarted()) {
            camera.start();
        }
    }


    private void findRectangle(Mat src) throws Exception {
        Mat blurred = src.clone();
        Imgproc.medianBlur(src, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;

        for (int c = 0; c < 3; c++) {
            int ch[] = { c, 0 };
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
            for (int t = 0; t < thresholdLevel; t++) {
                if (t == 0) {
                    Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                    // ?
                } else {
                    Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                            Imgproc.THRESH_BINARY,
                            (src.width() + src.height()) / 200, t);
                }

                Imgproc.findContours(gray, contours, new Mat(),
                        Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours) {
                    MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                    double area = Imgproc.contourArea(contour);
                    approxCurve = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp, approxCurve,
                            Imgproc.arcLength(temp, true) * 0.02, true);

                    if (approxCurve.total() == 4 && area >= maxArea) {
                        double maxCosine = 0;

                        List<Point> curves = approxCurve.toList();
                        for (int j = 2; j < 5; j++) {

                            double cosine = Math.abs(angle(curves.get(j % 4),
                                    curves.get(j - 2), curves.get(j - 1)));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3) {
                            maxArea = area;
                            maxId = contours.indexOf(contour);
                        }
                    }
                }
            }
        }

        if (maxId >= 0) {
            Imgproc.drawContours(src, contours, maxId, new Scalar(255, 0, 0,
                    .8), 8);

        }
    }

    private double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }



    //endregion
}
