package jayyo.mybitmap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class MyMediaProjectionService extends Service {
    private static final String TAG = "MediaProjectService";

    private static MyMediaProjectionService mService = null;

    private VirtualDisplay aVirtualDisplay;//這個鬼東西很危險，一定要 自行 釋放，不然會一直存在關不掉
    private MediaProjection mMediaProjection;//這個鬼東西很危險，一定要 自行 釋放，不然會一直存在關不掉

    private Handler mHandler;
    private Runnable mRunnable;
    private Image image;
    private int widthPixels, heightPixels, densityDpi;

    public static MyMediaProjectionService get(){
        return mService;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "CREATE");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "CREATE");
        super.onCreate();

        mService = this;

        startForegroundNotification();

        HandlerThread handlerThread = new HandlerThread("recordScreenCapture");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mRunnable = () -> {
            openScreenCapture(widthPixels, heightPixels, densityDpi);
        };
        mHandler.postDelayed(mRunnable, 3000);

    }

//    private void doScreenShot(){
//        WindowManager systemService = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            systemService.getCurrentWindowMetrics();
//            systemService.getMaximumWindowMetrics();
//        } else {
//            systemService.getDefaultDisplay();
//        }
//
//        // Get device dimmensions
//        Display display = getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//
//        // Get root view
//        View view = getWindow().getDecorView().getRootView();
//
//        // Create the bitmap to use to draw the screenshot
//        final Bitmap bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_4444);
//        final Canvas canvas = new Canvas(bitmap);
//
//        // Get current theme to know which background to use
////        final Activity activity = this;
////        final Resources.Theme theme = activity.getTheme();
////        final TypedArray ta = theme
////                .obtainStyledAttributes(new int[] { android.R.attr.windowBackground });
////        final int res = ta.getResourceId(0, 0);
////        final Drawable background = activity.getResources().getDrawable(res, theme);
//
//        // Draw background
////        background.draw(canvas);
//
//        // Draw views
//        view.draw(canvas);
//
//        String path = "sdcard/Download";
//        File imageFile = new File(path, "tmpImage.jpg");
//
//        try {
//            FileOutputStream outputStream = new FileOutputStream(imageFile);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//            outputStream.flush();
//            outputStream.close();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }

    private void startForegroundNotification() {
        NotificationManager notificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        if (null != notificationManager) {
            String CHANNEL_ID = mService.getString(R.string.app_name);
            CharSequence name = mService.getString(R.string.app_name);
            String description = mService.getString(R.string.app_name);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);

            Intent notificationIntent = new Intent(mService, MainActivity.class);
            int flag = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                flag = PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(mService, 0,
                    notificationIntent, flag);

            Bundle bundle = new Bundle();

            Notification notification = new Notification.Builder(mService.getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.wallpaper)
                    .setColor(Color.BLUE)
                    .setExtras(bundle)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle("mService")
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .build();

            mService.startForeground(42302, notification);
        }
    }

    private void openScreenCapture(int aWidth, int aHeight, int aDpi) {
        try {
            ImageReader mImageReader = ImageReader.newInstance(aWidth, aHeight,
                    PixelFormat.RGBA_8888, 1);

            aVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture",
                    aWidth, aHeight, aDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mImageReader.getSurface(),
                    null,
                    mHandler);

            mImageReader.setOnImageAvailableListener((ImageReader.OnImageAvailableListener) reader -> {
                try {
                    image = reader.acquireLatestImage();
                    if (null != image) {
                        Log.i(TAG, "openScreenCapture" +
                                ", getWidth: " + image.getWidth() +
                                ", getHeight: " + image.getHeight());

                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();

                        String path = "sdcard/Download";
//                        String path = "storage/emulated/0/Download";

                        File dir = new File(path);
                        if (!dir.exists() || !dir.isDirectory()){
                            dir.mkdir();
                        }

                        File cacheFile = new File(path, "tmp.jpg");
                        OutputStream fOut = new FileOutputStream(cacheFile);
//                        fOut.write(buffer.array());

                        Bitmap bitmap = null;
                        bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);

                        fOut.flush();
                        fOut.close();

                        aVirtualDisplay.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != image) {
                        image.close();
                    }
                }
            }, mHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aVirtualDisplay != null){
            aVirtualDisplay.release();
            aVirtualDisplay = null;
        }

        if (mMediaProjection != null){
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mService != null){
            mService.stopForeground(Service.STOP_FOREGROUND_REMOVE);
            mService.stopForeground(true);
            mService.stopSelf();
            mService = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        String action = intent.getAction();

        widthPixels = intent.getIntExtra("widthPixels", 0);
        heightPixels = intent.getIntExtra("heightPixels", 0);
        densityDpi = intent.getIntExtra("densityDpi", 0);

        int mResultCode = intent.getIntExtra("resultCode", 0);
        Intent mResultData = intent.getParcelableExtra("data");

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getApplicationContext().
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, Objects.requireNonNull(mResultData));

        return super.onStartCommand(intent, flags, startId);
    }
}
