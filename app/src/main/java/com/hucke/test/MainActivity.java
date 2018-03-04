package com.hucke.test;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    String[] mplist;
    MediaPlayer mp;
    Button btnStart;
    Button btnStop;
    Button btnPause;
    Button btnForward;
    Button btnBackward;
    Button btnForward3;
    Button btnBackward3;

    SeekBar sb;
    TextView tvProgress;    // 재생시간을 보여주는 텍스트
    ListView mp3ListView;
    ArrayAdapter<String> mp3ListViewAdapter;

    //SD카드 경로
    String sdPath;
    ArrayList<String> mp3List;

    boolean mode = false;
    boolean isPlaying = false;
    int playIndex = 0;

    int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    // 시크바를 업데이트 하기 위한 Thread
    class MyThread extends Thread {
        @Override
        public void run() { // 쓰레드가 시작되면 콜백되는 메서드
            // 씨크바 막대기 조금씩 움직이기 (노래 끝날 때까지 반복)
            while(isPlaying) {
                int progress = mp.getCurrentPosition();
                sb.setProgress(progress);

                // 시간표시를 업데이트 하기 위해 메시지를 보냄
                handler.sendEmptyMessage(0);
                try {
                    Thread.sleep(500);  // 500ms 마다 업데이트
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 진행시간 표시를 업데이트 하기 위한 메시지 핸들러
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0) {
                updateProgressedTime(mp.getDuration()/1000, mp.getCurrentPosition()/1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.click_btn_start);
        btnStart.setOnClickListener(mListener);

        btnStop = findViewById(R.id.click_btn_stop);
        btnStop.setOnClickListener(mListener);

        btnPause = findViewById(R.id.click_btn_pause);
        btnPause.setOnClickListener(mListener);

        btnForward3 = findViewById(R.id.click_btn_forw3s);
        btnForward3.setOnClickListener(mListener);
        btnForward = findViewById(R.id.click_btn_forw5s);
        btnForward.setOnClickListener(mListener);

        btnBackward3 = findViewById(R.id.click_btn_backw3s);
        btnBackward3.setOnClickListener(mListener);
        btnBackward = findViewById(R.id.click_btn_backw5s);
        btnBackward.setOnClickListener(mListener);

        sb = findViewById(R.id.seekBar_progress);
        sb.setOnSeekBarChangeListener(mSbListener);

        tvProgress = findViewById(R.id.textProgress);

        mp3ListView = findViewById(R.id.listOfMusic);

        // 2.SD카드 사용가능 여부 판단
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) == false) {
            Toast.makeText(this, "SDCard error!", Toast.LENGTH_LONG).show();
            finish();
            return;
         }

         if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
             Toast.makeText(this, "저장소 접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
             if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                 // 사용자가 임의로 권한을 취소시킨 경우, 권한 재요청
                 ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
             } else {
                 // 최초로 권한을 요청하는 경우 (첫 실행)
                 ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
             }
         }

        //SD카드 디렉토리(폴더)
        File sdDir;

        // 3.SD카드 경로에서  FilenameFilter 사용하여  mp3파일 추출
        sdPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/";
        sdDir = new File(sdPath);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3") || name.endsWith(".m4a");
            }
        };

        // 4.추출한 mp3파일들을 파일목록 List에 저장
        mplist = sdDir.list(filter);
        if(mplist == null) {
            Toast.makeText(this, "재생할 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (mplist.length == 0) {
            Toast.makeText(this, "재생할 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mp3List = new ArrayList<String>();
        for(String s : mplist) {
//            mp3List.add(sdPath + s);
            mp3List.add(s);
        }

        mp3ListViewAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mp3List);
        mp3ListView.setAdapter(mp3ListViewAdapter);
        mp3ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                playIndex = position;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Stop();
    }

    View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.click_btn_start:
                    Play();
                    break;
                case R.id.click_btn_stop:
                    Stop();
                    break;
                case R.id.click_btn_pause:
                    Pause();
                    if(mode)
                        btnPause.setText("RESUME");
                    else
                        btnPause.setText("PAUSE");
                    break;
                case R.id.click_btn_forw3s:
                    Forward(3000);
                    break;
                case R.id.click_btn_forw5s:
                    Forward(5000);
                    break;
                case R.id.click_btn_backw3s:
                    Backward(3000);
                    break;
                case R.id.click_btn_backw5s:
                    Backward(5000);
                    break;
                default:
                    break;
            }
        }
    };

    SeekBar.OnSeekBarChangeListener mSbListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStopTrackingTouch(SeekBar seekBar) {
//            Toast.makeText(getApplicationContext(), "onStopTrackingTouch", Toast.LENGTH_SHORT).show();
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
//            Toast.makeText(getApplicationContext(), "onStartTrackingTouch", Toast.LENGTH_SHORT).show();
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//            Toast.makeText(getApplicationContext(), "onProgressChanged", Toast.LENGTH_SHORT).show();
            if(fromUser) {
                mp.seekTo(progress);
            }
        }
    };

    private void Play(){
        Stop();

//        mp = MediaPlayer.create(MainActivity.this, R.raw.love);
        mp = new MediaPlayer();
        try {
            mp.setDataSource(sdPath + mplist[playIndex]);
            mp.prepare();
        } catch(Exception e) {
            e.printStackTrace();
        }
        mp.setLooping(false); // 무한반복 끔.
        int fullTime = mp.getDuration();
        sb.setMax(fullTime);

//        Toast.makeText(getApplicationContext(), Integer.toString(mp.getDuration()), Toast.LENGTH_SHORT).show();

        mp.start();
        mode = false;
        new MyThread().start();
        isPlaying = true;
    }

    private void Stop(){
        if(mp!=null){
            // 음악 종료
            isPlaying = false; // 쓰레드 종료.
            mp.stop();
            mp.release();
            mp = null;
            sb.setProgress(0);
            updateProgressedTime(0, 0);
        }
    }

    private void Pause(){
        if(mp!=null){
            if(mode) {
                mp.start();
                mode = false;
            }
            else {
                mp.pause();
                mode = true;
            }
        }
    }

    private void Forward(int timeMs) {
        if (mp != null) {
            int timePos;
            timePos = mp.getCurrentPosition();
            mp.pause();
            timePos = timePos + timeMs;
            mp.seekTo(timePos);
            mp.start();
//            String msg;
//            msg = Integer.toString(timePos);
//            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void Backward(int timeMs) {
        if (mp != null) {
            int timePos;
            timePos = mp.getCurrentPosition();
            mp.pause();
            timePos = timePos - timeMs;
            if(timePos < 0)
                timePos = 0;
            mp.seekTo(timePos);
            mp.start();
        }
    }

    private void updateProgressedTime(int total_sec, int prog_sec) {
        String text;
        int total_min = total_sec / 60;
        int prog_min = prog_sec / 60;
        text = Integer.toString(prog_min) + "m" + Integer.toString(prog_sec % 60) + "s / " + Integer.toString(total_min) + "m" + Integer.toString(total_sec % 60) + "s";
        tvProgress.setText(text);
    }
}
