package com.test.cameradecod.MediaCodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;

import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class use for Decode Video Frame Data and show to SurfaceTexture
 * Created by zj on 2018/7/29 0029.
 */
public class VideoDecoder {
    private final static String TAG = "VideoEncoder";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface mSurface;
    private int mViewWidth;
    private int mViewHeight;

    private VideoEncoder mVideoEncoder;
    private Handler mVideoDecoderHandler;
    private HandlerThread mVideoDecoderHandlerThread = new HandlerThread("VideoDecoder");
    private IDecoderListener mIDecoderListener;
    int i;
    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int id) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(id);
            inputBuffer.clear();
            byte[] dataSources = null;
            if (mVideoEncoder != null) {
              //  dataSources = mVideoEncoder.pollFrameFromEncoder();
                //   Log.i(TAG, "dataSources =" + dataSources);
                if (mIDecoderListener != null && dataSources != null) {
                    mIDecoderListener.OnImageDataListener(dataSources);
                }
            }

            Log.i(TAG,"i="+i);

           // dataSources = dataSou;
            int length = 0;
            if (dataSources != null) {
                inputBuffer.put(dataSources);
                length = dataSources.length;
            }

            mediaCodec.queueInputBuffer(id, 0, length, 0, 0);
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int id, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(id);
            MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
            if (mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0) {
                byte[] buffer = new byte[outputBuffer.remaining()];
                outputBuffer.get(buffer);
            }
            mMediaCodec.releaseOutputBuffer(id, true);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "------> onError");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, "------> onOutputFormatChanged");
        }
    };

    public VideoDecoder(String mimeType, Surface surface, int viewwidth, int viewheight) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mimeType);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        if (surface == null) {
            return;
        }

        this.mViewWidth = viewwidth;
        this.mViewHeight = viewheight;
        this.mSurface = surface;

        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new Handler(mVideoDecoderHandlerThread.getLooper());

        mMediaFormat = MediaFormat.createVideoFormat(mimeType, mViewWidth, mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1920 * 1280);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    }

    public void setEncoder(VideoEncoder videoEncoder) {
        this.mVideoEncoder = videoEncoder;
    }
    byte[] dataSou;
    public void setEncoder(byte[] data) {
        this.dataSou = data;
    }

    public void startDecoder() {
        if (mMediaCodec != null && mSurface != null) {
            mMediaCodec.setCallback(mCallback, mVideoDecoderHandler);
            mMediaCodec.configure(mMediaFormat, mSurface, null, CONFIGURE_FLAG_DECODE);
            mMediaCodec.start();
        } else {
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec is init correct");
        }
    }

    public void stopDecoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release() {
        if (mMediaCodec != null) {
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }
    int frameIndex;
    int fps = 30;
    //解码帧数据到Surface
    public boolean decodeFrame(byte[] buffer, int offset, int length) {

        // get input buffer index
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(100);
        if (inputBufferIndex < 0) return false;
        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
        inputBuffer.clear();
        inputBuffer.put(buffer, offset, length);
        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, frameIndex * fps, 0);
        frameIndex++;

        // get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return true;
    }



    public void setDecoderListener(IDecoderListener listener) {
        this.mIDecoderListener = listener;
    }


    public interface IDecoderListener {
        public void OnImageDataListener(byte[] dataSources);
    }
}
