package wj.redpacket.com.arredpacket;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback,View.OnTouchListener {
    static{ System.loadLibrary("opencv_java3"); }
    private final float scaleRate = 0.25f;          //缩放比例
    private final int SIMILAR_NUM = 3;            //连续相似次数

    private int similarCount = 0;
    private Bitmap mlastBitmap;
    private boolean isRiskMove;
    private int mRiskLastX;
    private int mRiskLastY;
    private int phoneWidth;
    private int phoneHeight;
    private Button mHideRed;
    private Button mFindeRed;
    private Camera mCamera;
    private ImageView mImageView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap mHideBitmap;
    private Bitmap mFindBitmap;
    private boolean mCurrentStateHide = false;      //true是藏  false是找
    private AtomicBoolean mFindRed = new AtomicBoolean(false);      //线程安全boolean

    protected Handler mHandler = new Handler();

    private static final int REQUEST_CODE_CAMERA_AND_RECORD_AUDIO_PERMISSION = 1;
    private static final String[] REQUEST_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                    Manifest.permission.READ_EXTERNAL_STORAGE :
                    Manifest.permission.WRITE_EXTERNAL_STORAGE /* a place holder for API lower than 16 */,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowManager wm = (WindowManager) MainActivity.this.getSystemService(MainActivity.this.WINDOW_SERVICE);
        phoneHeight = wm.getDefaultDisplay().getHeight();
        phoneWidth = wm.getDefaultDisplay().getWidth();
        mImageView = (ImageView)findViewById(R.id.imageView);
        mImageView.setVisibility(View.GONE);
        mImageView.setOnTouchListener(this);
        mImageView.setOnClickListener(this);
        mHideRed = (Button)findViewById(R.id.hide_red);
        mFindeRed = (Button)findViewById(R.id.find_red);
        mSurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        mSurfaceView.setZOrderOnTop(false);
        mHideRed.setOnClickListener(this);
        mFindeRed.setOnClickListener(this);
        mFindeRed.setEnabled(false);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        checkPermission();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = CameraUtil.open(this);
        CameraUtil.chooseBestPreviewSize(mCamera);          //选择最佳预览size，和图片格式
        CameraUtil.setCameraDisplayOrientation(this, mCamera);      //设置图片的方向
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }
        startCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHandler.removeCallbacksAndMessages(null);
        if (mCamera != null) {
            mCamera.release();
        }
    }

    private void checkPermission() {
        if (!isAllPermissionGranted(REQUEST_PERMISSIONS)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS,
                        REQUEST_CODE_CAMERA_AND_RECORD_AUDIO_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS,
                        REQUEST_CODE_CAMERA_AND_RECORD_AUDIO_PERMISSION);
            }
        }
    }

    private boolean isAllPermissionGranted(String[] permission) {
        if (permission != null) {
            boolean hasUnGranted = false;
            for (String s : permission) {
                if (ContextCompat.checkSelfPermission(this, s)
                        != PermissionChecker.PERMISSION_GRANTED) {
                    hasUnGranted = true;
                }
            }
            return !hasUnGranted;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.hide_red:
                mCurrentStateHide = true;
                takeOneFrame();     //截取一帧
                break;
            case R.id.find_red:
                mFindRed.set(false);
                mCurrentStateHide = false;
                mHideRed.setEnabled(false);
                mFindeRed.setEnabled(false);
                startCamera();
                mHandler.postDelayed(mSchedualTakeOneFrame, 1000);      //设置定时器1S后调用函数
                break;
            case R.id.imageView:
                if(isRiskMove){
                    isRiskMove=false;
                    return;
                }
                break;
        }
    }

    private void startCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();  //停止预览
                mCamera.setPreviewDisplay(mSurfaceHolder);  //修改设置
            } catch (Exception e) {
                e.printStackTrace();
            }
            mCamera.startPreview();     //重新打开相机预览
        }
    }

    private Runnable mSchedualTakeOneFrame=new Runnable() {
        @Override
        public void run() {
            takeOneFrame();
            mHandler.postDelayed(this, 100);        //0.1s后调用自身
        }
    };

    private void takeOneFrame() {
        if (mCamera == null){
            return;
        }
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {            //对获取的一帧进行操作
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Camera.Size size = camera.getParameters().getPreviewSize();
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        if (mCurrentStateHide) {
                            mHideBitmap = bitmap;
                            //BitmapCompare.setHidedPicKeyPoints(bitmap);
                            Match.setHidedPicKeyPoints(bitmap);
                            runOnUiThread(new Runnable() {      //更新主线程
                                @Override
                                public void run() {
                                    mFindeRed.setEnabled(true);
                                    Bitmap previewImage = CameraUtil.getSmallBitmap(scaleRate,mHideBitmap);
                                    //Toast.makeText(MainActivity.this,"height"+wm.getDefaultDisplay().getHeight()+"width"+wm.getDefaultDisplay().getWidth(),Toast.LENGTH_SHORT).show();
                                    mImageView.setImageBitmap(previewImage);
                                    mImageView.setVisibility(View.VISIBLE);
                                }
                            });
                            try {
                                mCamera.stopPreview();          //定格这一帧到surface
                                Thread.sleep(1000);
                                mCamera.startPreview();
                            } catch (Exception e) {
                                //// TODO: 17/4/4
                            }
                        } else {
                              if (mlastBitmap == null){
                                  mlastBitmap =bitmap;          //mlastBitmap记录上一张bitmap
                              }else{
                                  mFindBitmap = bitmap;
                                  float similar = BitmapCompare.similarity(mlastBitmap, mFindBitmap);
                                  if (similar >0.86f){
                                      similarCount++;
                                      if (similarCount == SIMILAR_NUM){
                                          //ORB相似性计算
                                          Log.i("Image 开始","dddddd");
                                          Match.setFindedPicKeyPoints(bitmap);
                                          Match.getGoodMatch();
                                          Match.calculPic(Match.findedPic);
                                          Match.calculPic(Match.hidedPic);
                                          boolean ans = Match.matchCluster();
                                          if (ans){
                                              runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      Toast.makeText(MainActivity.this,"找到了！！",Toast.LENGTH_SHORT).show();
                                                      mImageView.setVisibility(View.GONE);
                                                      mHandler.removeCallbacksAndMessages(null);
                                                      mHideRed.setEnabled(true);
                                                  }
                                              });
                                          }else{
                                              runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      Toast.makeText(MainActivity.this,"没找到！！",Toast.LENGTH_SHORT).show();
                                                  }
                                              });
                                          }

                                      }
                                  }else{
                                      similarCount = 0;
                                  }
                                  mlastBitmap = bitmap;
                              }
//                            mFindBitmap = bitmap;
//                            float similar = BitmapCompare.similarity(mHideBitmap, mFindBitmap);
//                            Log.e("MainActivity", "" + similar);
//                            try {
//                                if (similar > 85.555f) {
//                                    if (mFindRed.get()) {
//                                        return;
//                                    }
//                                    mFindRed.set(true);
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            Toast.makeText(MainActivity.this, getString(R.string.find_success), Toast.LENGTH_SHORT).show();
//                                            mImageView.setVisibility(View.GONE);
//                                            mHandler.removeCallbacksAndMessages(null);
//                                            mHideRed.setEnabled(true);
//                                        }
//                                    });
//                                }
//                            } catch (Exception e) {
//                                //// TODO: 17/4/4
//                            }
                        }
                    }
                }).start();
            }
        });
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                isRiskMove = true;
                //计算距离上次移动了多远
                int deltaX = x - mRiskLastX;
                int deltaY = y - mRiskLastY;
                int translationX = (int) (mImageView.getTranslationX() + deltaX);
                int translationY = (int) (mImageView.getTranslationY() + deltaY);

                mImageView.setTranslationX(translationX);
                mImageView.setTranslationY(translationY);
                break;
            case MotionEvent.ACTION_UP:
                if (mImageView.getX() - mSurfaceView.getX() < 0 ){          //碰左壁处理
                    mImageView.setX(mSurfaceView.getX());
                }else if ( mImageView.getX() > (mSurfaceView.getX() + mSurfaceView.getWidth() - mImageView.getWidth())){        //碰右壁处理
                    mImageView.setX(mSurfaceView.getX() + mSurfaceView.getWidth() - mImageView.getWidth());
                }
                if (mImageView.getY() - mSurfaceView.getY() < 0 ){      //碰上壁处理
                    mImageView.setY(mSurfaceView.getY());
                }else if(mImageView.getY() > (mSurfaceView.getY() + mSurfaceView.getHeight() - mImageView.getHeight())){        //碰下壁处理
                    mImageView.setY(mSurfaceView.getY() + mSurfaceView.getHeight() - mImageView.getHeight());
                }
                break;
            default:
                break;
        }
        //记录上次手指离开时的位置
        mRiskLastX = x;
        mRiskLastY = y;
        return false;
    }


}
