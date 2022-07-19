package com.test.cameradecod;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;

import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.test.cameradecod.MediaCodec.VideoDecoder;
import com.test.cameradecod.MediaCodec.VideoEncoder;
import com.test.cameradecod.base.MyApplication;
import com.test.cameradecod.receiver.OpenDevicesReceiver;
import com.test.cameradecod.receiver.UsbDetachedReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 车机端
 * yaojun
 */

public class MainActivity extends AppCompatActivity implements UsbDetachedReceiver.UsbDetachedListener, OpenDevicesReceiver.OpenDevicesListener, View.OnClickListener, VideoDecoder.IDecoderListener {

    private String TAG = "MainActivity-host";
    private static final int CONNECTED_SUCCESS = 0;
    private static final int RECEIVER_MESSAGE_SUCCESS = 1;
    private static final int SEND_MESSAGE_SUCCESS = 2;
    private static final String USB_ACTION = "com.tcl.navigator.hostchart";
    private TextView mLog;
    private EditText mMessage;
    private UsbDetachedReceiver mUsbDetachedReceiver;
    private ExecutorService mThreadPool;
    private UsbManager mUsbManager;
    private OpenDevicesReceiver mOpenDevicesReceiver;
    private TextView mError;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbEndpoint mUsbEndpointOut;
    private UsbEndpoint mUsbEndpointIn;
    private boolean mToggle = true;

    private boolean isDetached = false;
    private byte[] mBytes = new byte[1024];
    private boolean isReceiverMessage = true;
    private UsbInterface mUsbInterface;
    private StringBuffer mStringBuffer = new StringBuffer();
    private Context mContext;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTED_SUCCESS://车机和手机连接成功


                    loopReceiverMessage();
                    break;

                case RECEIVER_MESSAGE_SUCCESS://成功接受到数据

                    break;

                case SEND_MESSAGE_SUCCESS://成功发送数据

                    break;
                case 3:

                    break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST", "Granted");
            init();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

    }

    private void init() {
        initView();
        initData();
    }

    private TextureView mCameraTexture;
    private TextureView mDecodeTexture;

    private VideoDecoder mVideoDecoder;
    private VideoEncoder mVideoEncoder;
    private Camera mCamera;
    private int mPreviewWidth = 640;
    private int mPreviewHeight = 480;

    private final static String MIME_FORMAT = "video/avc"; //support h.264

    private void initView() {
        mCameraTexture = (TextureView) findViewById(R.id.camera);
        mDecodeTexture = (TextureView) findViewById(R.id.decode);
        mCameraTexture.setSurfaceTextureListener(mCameraTextureListener);
        mDecodeTexture.setSurfaceTextureListener(mDecodeTextureListener);
        mCameraTexture.setRotation(90);
        mDecodeTexture.setRotation(90);
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera(surfaceTexture, mPreviewWidth, mPreviewHeight);
            }
        });
        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeCamera();
            }
        });
    }


    HandlerThread mHandlerThread = new HandlerThread("PhoneThread");
    Handler mThreadHandler;

    private Camera.PreviewCallback mPreviewCallBack = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            Log.i(TAG, "onPreviewFrame");
            byte[] i420bytes = new byte[bytes.length];
            //from YV20 TO i420
            System.arraycopy(bytes, 0, i420bytes, 0, mPreviewWidth * mPreviewHeight);
            System.arraycopy(bytes, mPreviewWidth * mPreviewHeight + mPreviewWidth * mPreviewHeight / 4, i420bytes, mPreviewWidth * mPreviewHeight, mPreviewWidth * mPreviewHeight / 4);
            System.arraycopy(bytes, mPreviewWidth * mPreviewHeight, i420bytes, mPreviewWidth * mPreviewHeight + mPreviewWidth * mPreviewHeight / 4, mPreviewWidth * mPreviewHeight / 4);
            if (mVideoEncoder != null) {
                mVideoEncoder.inputFrameToEncoder(i420bytes);
                Log.i(TAG, "mVideoEncoder.inputFrameToEncoder");
            }
            mVideoDecoder.decodeFrame(i420bytes,0,i420bytes.length);
        }
    };
    public  byte[] getEncoder() {
        return mVideoEncoder.pollFrameFromEncoder();
    }


    SurfaceTexture surfaceTexture;
    private TextureView.SurfaceTextureListener mCameraTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            MainActivity.this.surfaceTexture = surfaceTexture;
            mVideoEncoder = new VideoEncoder(MIME_FORMAT, mPreviewWidth, mPreviewHeight);
            mVideoEncoder.startEncoder();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            if (mVideoEncoder != null) {
                mVideoEncoder.release();
            }
           MainActivity.this.surfaceTexture = null;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };
    byte[] bytesddd;

    @Override
    public void OnImageDataListener(byte[] dataSources) {
        Log.i(TAG, "OnImageDataListener =" + dataSources);
        bytesddd = dataSources;
        mThreadHandler.post(mBackgroundRunnable);
    }

    Runnable mBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            sendMessageToPoint(bytesddd);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void sendMessageToPoint(byte[] buffer) {
        if (buffer != null && buffer.length != 0) {
            int pack = buffer.length / 16384;
            if (buffer.length % 16384 > 0) pack = pack + 1;
            for (int i = 0; i < pack; i++) {
                byte[] newBuffer = Arrays.copyOfRange(buffer, i * 16384, 16384 + i * 16384);
                if (mUsbDeviceConnection != null) {
                    mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, newBuffer, 0, newBuffer.length, 0);
                }
            }
        }
    }

    private TextureView.SurfaceTextureListener mDecodeTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            System.out.println("----------" + i + " ," + i1);
            mVideoDecoder = new VideoDecoder(MIME_FORMAT,
                    new Surface(surfaceTexture),
                    mPreviewWidth, mPreviewHeight);
            Log.i(TAG, "mVideoDecoder");
            mVideoDecoder.setEncoder(mVideoEncoder);
            mVideoDecoder.setDecoderListener(MainActivity.this);
            mVideoDecoder.startDecoder();

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            mVideoDecoder.stopDecoder();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };


    private void openCamera(SurfaceTexture texture, int width, int height) {
        if (texture == null) {
            Log.e(TAG, "openCamera need SurfaceTexture");
            return;
        }
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper());
        mCamera = Camera.open(0);
        try {
            mCamera.setPreviewTexture(texture);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.YV12);
            List<Camera.Size> list = parameters.getSupportedPreviewSizes();
            for (Camera.Size size : list) {
                System.out.println("----size width = " + size.width + " size height = " + size.height);
            }

            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(mPreviewCallBack);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mCamera = null;
        }
    }

    private void closeCamera() {
        if (mCamera == null) {
            Log.e(TAG, "Camera not open");
            return;
        }
        mCamera.stopPreview();
        mCamera.release();
    }

    private void initData() {
        mContext = getApplicationContext();

        mUsbDetachedReceiver = new UsbDetachedReceiver(this);
        IntentFilter intentFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbDetachedReceiver, intentFilter);

        mThreadPool = Executors.newFixedThreadPool(5);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        openDevices();
    }

    /**
     * 打开设备 , 让车机和手机端连起来
     */
    private void openDevices() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(USB_ACTION), 0);
        IntentFilter intentFilter = new IntentFilter(USB_ACTION);
        mOpenDevicesReceiver = new OpenDevicesReceiver(this);
        registerReceiver(mOpenDevicesReceiver, intentFilter);

        //列举设备(手机)
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (deviceList != null) {
            for (UsbDevice usbDevice : deviceList.values()) {
                int productId = usbDevice.getProductId();
                if (productId != 377 && productId != 7205) {
                    if (mUsbManager.hasPermission(usbDevice)) {
                        initAccessory(usbDevice);
                    } else {
                        mUsbManager.requestPermission(usbDevice, pendingIntent);
                    }
                }
            }
        } else {
            mError.setText("请连接USB");
        }
    }

    /**
     * 发送命令 , 让手机进入Accessory模式
     *
     * @param usbDevice
     */
    private void initAccessory(UsbDevice usbDevice) {
        UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(usbDevice);
        if (usbDeviceConnection == null) {
            mError.setText("请连接USB");
            return;
        }

        //根据AOA协议打开Accessory模式
        initStringControlTransfer(usbDeviceConnection, 0, "Google, Inc."); // MANUFACTURER
        initStringControlTransfer(usbDeviceConnection, 1, "AccessoryChat"); // MODEL
        initStringControlTransfer(usbDeviceConnection, 2, "Accessory Chat"); // DESCRIPTION
        initStringControlTransfer(usbDeviceConnection, 3, "1.0"); // VERSION
        initStringControlTransfer(usbDeviceConnection, 4, "http://www.android.com"); // URI
        initStringControlTransfer(usbDeviceConnection, 5, "0123456789"); // SERIAL
        usbDeviceConnection.controlTransfer(0x40, 53, 0, 0, new byte[]{}, 0, 100);
        usbDeviceConnection.close();
        MyApplication.printLogDebug("initAccessory success");
        initDevice();
    }

    private void initStringControlTransfer(UsbDeviceConnection deviceConnection, int index, String string) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), 100);
    }

    /**
     * 初始化设备(手机) , 当手机进入Accessory模式后 , 手机的PID会变为Google定义的2个常量值其中的一个 ,
     */
    private void initDevice() {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (mToggle) {
                    SystemClock.sleep(1000);
                    HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
                    Collection<UsbDevice> values = deviceList.values();
                    if (!values.isEmpty()) {
                        for (UsbDevice usbDevice : values) {
                            int productId = usbDevice.getProductId();
                            if (productId == 0x2D00 || productId == 0x2D01) {
                                if (mUsbManager.hasPermission(usbDevice)) {
                                    mUsbDeviceConnection = mUsbManager.openDevice(usbDevice);
                                    if (mUsbDeviceConnection != null) {
                                        mUsbInterface = usbDevice.getInterface(0);
                                        int endpointCount = mUsbInterface.getEndpointCount();
                                        for (int i = 0; i < endpointCount; i++) {
                                            UsbEndpoint usbEndpoint = mUsbInterface.getEndpoint(i);
                                            if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                                                if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                                    mUsbEndpointOut = usbEndpoint;
                                                } else if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                                    mUsbEndpointIn = usbEndpoint;
                                                }
                                            }
                                        }
                                        if (mUsbEndpointOut != null && mUsbEndpointIn != null) {
                                            MyApplication.printLogDebug("connected success");
                                            mHandler.sendEmptyMessage(CONNECTED_SUCCESS);
                                            mToggle = false;
                                            isDetached = true;
                                        }
                                    }
                                } else {
                                    mUsbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(mContext, 0, new Intent(""), 0));
                                }
                            }
                        }
                    } else {
                        finish();
                    }
                }
            }
        });
    }

    /**
     * 接受消息线程 , 此线程在设备(手机)初始化完成后 , 就一直循环接受消息
     */
    private void loopReceiverMessage() {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(1000);
                while (isReceiverMessage) {
                    /**
                     * 循环接受数据的地方 , 只接受byte数据类型的数据
                     */
                    if (mUsbDeviceConnection != null && mUsbEndpointIn != null) {
                        int i = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, mBytes, mBytes.length, 3000);
                        MyApplication.printLogDebug(i + "");
                        if (i > 0) {
                            mStringBuffer.append(new String(mBytes, 0, i) + "\n");
                            mHandler.sendEmptyMessage(RECEIVER_MESSAGE_SUCCESS);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void usbDetached() {
        if (isDetached) {
            finish();
        }
    }

    @Override
    public void openAccessoryModel(UsbDevice usbDevice) {
        initAccessory(usbDevice);
    }

    @Override
    public void openDevicesError() {
        mError.setText("USB连接错误");
    }

    @Override
    public void onClick(View v) {
        final String messageContent = mMessage.getText().toString();
        if (!TextUtils.isEmpty(messageContent)) {
            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    /**
                     * 发送数据的地方 , 只接受byte数据类型的数据
                     */
                    int i = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, messageContent.getBytes(), messageContent.getBytes().length, 3000);
                    if (i > 0) {//大于0表示发送成功
                        mHandler.sendEmptyMessage(SEND_MESSAGE_SUCCESS);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();

        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }
        mUsbEndpointIn = null;
        mUsbEndpointOut = null;
        mToggle = false;
        isReceiverMessage = false;
        mThreadPool.shutdownNow();
        unregisterReceiver(mUsbDetachedReceiver);
        unregisterReceiver(mOpenDevicesReceiver);
    }


}
