package hu.bme.mit.soundtest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private Button btnStartListen;
    private TextView chordView1;
    private TextView chordView2;
    private TextView chordView3;

    private ListeningRunnable listeningRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] permissions = {Manifest.permission.RECORD_AUDIO};
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    permissions, Consts.PERMISSION_RECORD_AUDIO);

        } else {
            listeningRunnable = new ListeningRunnable(this);
        }

        setContentView(R.layout.activity_main);
        btnStartListen = findViewById(R.id.btnStartListen);
        chordView1 = findViewById(R.id.mainText);
        chordView2 = findViewById(R.id.mainText2);
        chordView3 = findViewById(R.id.mainText3);

        btnStartListen.setOnClickListener(btnListenOnClick);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){

        if(grantResults.length == 0) {
            this.finishAffinity();
        }

        //Only initialize the ListeningRunnable if the permission is granted
        //Finish Activity if it is denied
        for(int i = 0; i < permissions.length; ++i){
            if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)){
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    listeningRunnable = new ListeningRunnable(this);
                    btnStartListen.setOnClickListener(btnListenOnClick);
                } else {
                    Toast.makeText(this, getString(R.string.text_permission_denied), Toast.LENGTH_LONG).show();
                    this.finishAffinity();
                    return;
                }
            }
        }
    }

    //Close
    @Override
    protected void onPause() {
        stopRecording();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnStartListen.setText(getResources().getString(R.string.start_listening));
        super.onPause();
    }

    private View.OnClickListener btnListenOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!listeningRunnable.isRecording()) {
                try {
                    startRecording();
                } catch (IllegalStateException | IllegalArgumentException e) {
                    Log.e("ERROR", e.getMessage());
                    return;
                }

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                btnStartListen.setText(getResources().getString(R.string.stop_listening));
            } else {
                stopRecording();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                btnStartListen.setText(getResources().getString(R.string.start_listening));
            }
        }
        //}
    };

    private void startRecording() {
        Thread recordingThread = new Thread(listeningRunnable, "Listening thread");
        recordingThread.start();
    }

    private void stopRecording() {
        // stops the recording activity
        if (listeningRunnable != null && listeningRunnable.recorder != null) {
            chordView1.setAlpha(0.3f);
            chordView2.setAlpha(0.3f);
            chordView3.setAlpha(0.3f);
            listeningRunnable.stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (listeningRunnable != null)
            listeningRunnable.recorder.release();
    }

    //Called by ProcessingRunnable to display the detected chords
    void updateUI(ArrayList<ProcessingRunnable.ChordData> chordDiffs){
        if (chordDiffs.get(0).delta > 0.5){
            chordView1.setAlpha(0.3f);
            chordView2.setAlpha(0.3f);
            chordView3.setAlpha(0.3f);
            return;
        }

        chordView1.setAlpha(1);
        chordView2.setAlpha(1);
        chordView3.setAlpha(1);
        chordView1.setText(String.format(
                "%s%s",
                Chords.getNoteName(chordDiffs.get(0).note),
                chordDiffs.get(0).chordType.getNotation()));

        // If chord #2 gets a score close enough to chord #1
        if (chordDiffs.get(0).delta/chordDiffs.get(1).delta >= 0.9) {
            chordView2.setText(String.format(
                    "%s%s",
                    Chords.getNoteName(chordDiffs.get(1).note),
                    chordDiffs.get(1).chordType.getNotation()));

        } else {
            chordView2.setText("");
            chordView3.setText("");
            return;
        }

        // If chord #3 gets a score close enough to chord #2
        if (chordDiffs.get(1).delta/chordDiffs.get(2).delta >= 0.9) {
            chordView3.setText(String.format(
                    "%s%s",
                    Chords.getNoteName(chordDiffs.get(2).note),
                    chordDiffs.get(2).chordType.getNotation()));
        } else {
            chordView3.setText("");
        }
    }
}