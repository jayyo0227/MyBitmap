package jayyo.mybitmap;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static int REQUEST_CODE = 42302;
    private Intent mMediaProjectService;
    private Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        Log.d(TAG, "threadId: " + Thread.currentThread().getId());

//        requestMediaProjectionPermission();
        askPermission();

        mRunnable = () -> {
            doScreenShot();
            new Handler().postDelayed(mRunnable, 3000);
        };
        new Handler().postDelayed(mRunnable, 0);

//        ImageView imageView = findViewById(R.id.imageView);
//        Drawable resImage = getResources().getDrawable(R.drawable.wallpaper);
//        Bitmap  bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
//        Bitmap aBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper);

//        clearCacheFile();//Android 11 以上沒有作用
//
//        checkCacheStorage();
//        new Handler().postDelayed(this::setImageBitmap, 1000);
//        new Handler().postDelayed(() -> saveTempBitmap("cacheBitmap.png"), 2000);//暫存空間大約才 6MB，爆掉會 crash
//        new Handler().postDelayed(() -> checkCacheStorage(), 10000);
    }

    private void clearCacheFile() {//Android 11 沒有作用
        Intent intent = new Intent(StorageManager.ACTION_CLEAR_APP_CACHE);
        startActivityForResult(intent, 999);
    }

    public void checkCacheStorage() {
        new Thread(() -> {
            StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            try {
                long bytesLength = storageManager.getCacheQuotaBytes(storageManager.getUuidForPath(getCacheDir()));
                Log.d(TAG, "getCacheQuotaBytes: " + bytesLength + " Bytes");

                bytesLength = storageManager.getCacheSizeBytes(storageManager.getUuidForPath(getCacheDir()));
                Log.d(TAG, "getCacheSizeBytes: " + bytesLength + " Bytes");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void saveTempBitmap(String filename) {
        try {
            Log.d(TAG, "getCacheDir: " + getApplicationContext().getCacheDir());

//            String filename = "cacheBitmap.png";
//            File cacheFile = File.createTempFile(filename, null, getApplicationContext().getCacheDir());//副檔名只會是 .tmp，不推薦
            File cacheFile = new File(getApplicationContext().getCacheDir(), filename);//這個寫法好一點，可以決定暫存的副檔名
            OutputStream fOut = new FileOutputStream(cacheFile);

            Bitmap aBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper);
            Bitmap outputImage = formatBitmap(aBitmap, 1920, 1080);
            outputImage.compress(Bitmap.CompressFormat.PNG, 0, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setImageBitmap() {
        ImageView imageView = findViewById(R.id.imageView);

        Bitmap aBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper);
        Bitmap outputImage = formatBitmap(aBitmap, 960, 540);

        imageView.setImageBitmap(outputImage);
    }

    public Bitmap formatBitmap(Bitmap inputImage, float newWidth, float Height) {
//        Bitmap inputImage = BitmapFactory.decodeByteArray(input, 0, input.length);
        Log.d(TAG, "formatBitmap, Config: " + inputImage.getConfig().name() +
                ", Height: " + inputImage.getHeight() +
                ", Width: " + inputImage.getWidth());

        float scaleX = ((float) newWidth / inputImage.getWidth());
        float scaleY = ((float) Height / inputImage.getHeight());

        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);

        Bitmap bitmap = Bitmap.createBitmap(inputImage, 0, 0,
                inputImage.getWidth(), inputImage.getHeight(),
                matrix, true);
        return bitmap;
    }

    private void doScreenShot(){
        WindowManager systemService = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        // Get device dimmensions
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Get root view
        View view = getWindow().getDecorView().getRootView();

        // Create the bitmap to use to draw the screenshot
        final Bitmap bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_4444);
        final Canvas canvas = new Canvas(bitmap);

        // Get current theme to know which background to use
//        final Activity activity = this;
//        final Resources.Theme theme = activity.getTheme();
//        final TypedArray ta = theme
//                .obtainStyledAttributes(new int[] { android.R.attr.windowBackground });
//        final int res = ta.getResourceId(0, 0);
//        final Drawable background = activity.getResources().getDrawable(res, theme);

        // Draw background
//        background.draw(canvas);

        // Draw views
        view.draw(canvas);

        String path = "sdcard/Download";
        File imageFile = new File(path, "tmpImage.jpg");

        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void askPermission() {
        String[] permissions = new String[2];
        permissions[0] = Manifest.permission.READ_EXTERNAL_STORAGE;
        permissions[1] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        requestPermissions(permissions, 1);
    }

    public void requestMediaProjectionPermission() {
        Log.i(TAG, "requestMediaProjectionPermission");
        Activity activity = (Activity) this;

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode +
                ", resultCode:" + resultCode +
                ", Intent:" + data);
        super.onActivityResult(requestCode, resultCode, data);

        if (Activity.RESULT_OK == resultCode && REQUEST_CODE == requestCode){
            mMediaProjectService = new Intent(MainActivity.this, MyMediaProjectionService.class);

            mMediaProjectService.putExtra("resultCode", resultCode);
            mMediaProjectService.putExtra("data", data);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

            mMediaProjectService.putExtra("widthPixels", displayMetrics.widthPixels);
            mMediaProjectService.putExtra("heightPixels", displayMetrics.heightPixels);
            mMediaProjectService.putExtra("densityDpi", displayMetrics.densityDpi);

            mMediaProjectService.putExtra("density", displayMetrics.density);
            mMediaProjectService.putExtra("scaledDensity", displayMetrics.scaledDensity);
            mMediaProjectService.putExtra("xdpi", displayMetrics.xdpi);
            mMediaProjectService.putExtra("ydpi", displayMetrics.ydpi);

            startForegroundService(mMediaProjectService);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaProjectService != null){
            stopService(mMediaProjectService);
            mMediaProjectService = null;
        }
    }
}