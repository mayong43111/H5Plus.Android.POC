package io.dcloud.HelloH5;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by HXL on 16/8/11.
 * 用于实现录音   暂停录音
 */
public class AudioRecorder {
    private static AudioRecorder audioRecorder;

    private final static long MAX_RECORDING_DURATION = 29000;
    //音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    //采用频率
    //44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    //采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 44100;
    //声道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    //编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;

    //录音对象
    private AudioRecord audioRecord;

    //录音状态
    private Status status = Status.STATUS_NO_READY;

    //录音内容
    private byte[] audiodata;

    //线程池
    private ExecutorService mExecutorService;

    public AudioRecorder() {
        mExecutorService = Executors.newCachedThreadPool();
    }

    //单例模式
    public static AudioRecorder getInstance() {
        if (audioRecorder == null) {
            audioRecorder = new AudioRecorder();
        }
        return audioRecorder;
    }

    /**
     * 创建默认的录音对象
     */
    public void createDefaultAudio() {
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL, AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);

        status = Status.STATUS_READY;
    }

    /**
     * 开始录音
     */
    public void startRecord() {

        if (status == Status.STATUS_NO_READY || audioRecord == null) {
            this.createDefaultAudio();
        }
        if (status == Status.STATUS_START) {
            this.stopRecord();
        }

        audioRecord.startRecording();
        //将录音状态设置成正在录音状态
        status = Status.STATUS_START;
        //使用线程池管理线程
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                audiodata = null;
                byte[] tempdata = new byte[bufferSizeInBytes];
                int readsize = 0;
                //将录音状态设置成正在录音状态
                status = Status.STATUS_START;
                Date curDate = new Date(System.currentTimeMillis());

                while (status == Status.STATUS_START
                        || (new Date(System.currentTimeMillis()).getTime() - curDate.getTime()) > MAX_RECORDING_DURATION) {
                    readsize = audioRecord.read(tempdata, 0, bufferSizeInBytes);
                    if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                        audiodata = byteMerger(audiodata, tempdata);
                    }
                }
            }
        });
    }

    /**
     * 停止录音
     */
    public String stopRecord() {

        if (status != Status.STATUS_NO_READY && status != Status.STATUS_READY) {
            audioRecord.stop();
            status = Status.STATUS_STOP;
            return release();
        }

        return "";
    }

    /**
     * 释放资源
     */
    public String release() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }

        status = Status.STATUS_NO_READY;
        audiodata = addWaveHead(audiodata);//给裸数据加上头文件

        return android.util.Base64.encodeToString(audiodata, Base64.NO_WRAP);
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

    /**
     * 获取录音对象的状态
     *
     * @return
     */
    public Status getStatus() {
        return status;
    }

    private static byte[] byteMerger(byte[] bt1, byte[] bt2) {
        if (bt1 == null) {
            return bt2;
        }

        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);

        return bt3;
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private byte[] addWaveHead(byte[] bt1) {

        int channels = 2;
        long totalAudioLen = bt1.length;
        long longSampleRate = AUDIO_SAMPLE_RATE;
        long totalDataLen = totalAudioLen + 36;
        long byteRate = 16 * AUDIO_SAMPLE_RATE * channels / 8;

        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return byteMerger(header, bt1);
    }
}
