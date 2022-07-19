package com.test.cameradecod;


//视频播放器


import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ByteBuffer;


public class H264PlayView/* extends SurfaceView*/ {

/*    MediaCodec mediaCodec;

    int width = 1280;
    int height = 720;
    int fps = 30;

    boolean playing = false;

    int frameIndex;

    ThreadUtils.Action onSurfaceChange;

    public H264PlayView(Context context) {
        this(context, null);
    }

    public H264PlayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet);
    }

    //控件初始化
    protected void init(Context context, AttributeSet attributeSet) {
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            @SneakyThrows
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (onSurfaceChange != null)
                    onSurfaceChange.runAndPostException();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                stop();
            }
        });
    }

    public void setVideoInfo(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    @SneakyThrows
    public void start() {
        if (playing)
            return;
        mediaCodec = MediaCodec.createDecoderByType("video/avc");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaCodec.configure(mediaFormat, getHolder().getSurface(), null, 0);
        mediaCodec.start();
        playing = true;
    }

    @SneakyThrows
    public void stop() {
        if (!playing)
            return;
        playing = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
    }

    //解码帧数据到Surface
    public boolean decodeFrame(byte[] buffer, int offset, int length) {

        if (!playing)
            return true;

        // get input buffer index
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100);
        if (inputBufferIndex < 0) return false;
        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
        inputBuffer.clear();
        inputBuffer.put(buffer, offset, length);
        mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, frameIndex * fps, 0);
        frameIndex++;

        // get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return true;
    }

    //设置初始化回调
    public H264PlayView onSurfaceChange(ThreadUtils.Action onSurfaceChange) {
        this.onSurfaceChange = onSurfaceChange;
        return this;
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }*/
}



