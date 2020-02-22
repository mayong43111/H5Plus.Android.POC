package io.dcloud.HelloH5;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Base64;

import java.io.IOException;

import io.dcloud.common.adapter.io.DHFile;

public class AudioRecorder {

    private static final int AUDIO_SAMPLE_RATE = 44100;

    private static AudioRecorder audioRecorder;

    private String fileFullPath = null;
    //录音状态
    private Status status = Status.STATUS_NO_READY;
    private MediaRecorder mRecorder;

    public static AudioRecorder getInstance() {
        if (audioRecorder == null) {
            audioRecorder = new AudioRecorder();
        }
        return audioRecorder;
    }

    public AudioRecorder() {

    }

    public void startRecord() {
        if (status == Status.STATUS_START) {
            this.stopRecord();
        }

        if (status == Status.STATUS_NO_READY || mRecorder == null) {
            this.createDefaultAudio();
        }
        //将录音状态设置成正在录音状态
        status = Status.STATUS_START;
        mRecorder.start();
    }

    public String stopRecord() {

        if (status == Status.STATUS_START) {
            mRecorder.stop();
            status = Status.STATUS_STOP;
            return release();
        }

        return "";
    }

    private String release() {
        status = Status.STATUS_NO_READY;

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        byte[] audiodata = DHFile.readAll(fileFullPath);
        DHFile.delete(fileFullPath);

        return android.util.Base64.encodeToString(audiodata, Base64.NO_WRAP);
    }

    private MediaRecorder createDefaultAudio() {
        mRecorder = new MediaRecorder();
        try {
            mRecorder.reset();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);// 使用设备默认音源
            fileFullPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/SoundRecorder/rx_english_audio_temp.mp4";
            if (!DHFile.isExist(fileFullPath)) {
                DHFile.createNewFile(DHFile.createFileHandler(fileFullPath));
            }
            mRecorder.setOutputFile(fileFullPath);
            mRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);// AMR_WB\AAC\AMR_NB\DEFAULT
            mRecorder.setMaxDuration(30);
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mRecorder;
    }

    /**
     * 录音对象的状态
     */
    public enum Status {
        //未开始
        STATUS_NO_READY,
        //预备
        STATUS_READY,
        //录音
        STATUS_START,
        //停止
        STATUS_STOP
    }
}
