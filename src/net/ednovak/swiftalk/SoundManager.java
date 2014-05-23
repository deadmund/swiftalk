package net.ednovak.swiftalk;

import java.util.ArrayList;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SoundManager extends Activity {
	
	final private double Fs = 44100.0;
	
	private ArrayList <TrackView> tracks = new ArrayList<TrackView>();
	
	final private short[] tmp = new short[(int)Fs*10];
	private int tmpEnd = 0;
	
	private AudioRecord ar;
	private AudioTrack at;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sound_manager_layout);
    }
    
    public void newRecording(View v){
    	boolean recording = ((ToggleButton) v).isChecked();
    	//Log.d("swiftalk", "recording: " + recording);
    	if(recording){
    		startRecording();
    	}
    	else{
    		stopRecording();
    	}
    }
    
    private void startRecording(){
    	final int buffSize = AudioRecord.getMinBufferSize(
    			(int)Fs, 
    			AudioFormat.CHANNEL_IN_MONO,
    			AudioFormat.ENCODING_PCM_16BIT);
    	
    	ar = new AudioRecord(
    			MediaRecorder.AudioSource.MIC,
    			(int)Fs, 
    			AudioFormat.CHANNEL_IN_MONO,
    			AudioFormat.ENCODING_PCM_16BIT, buffSize);
    	
    	Log.d("swiftalk", "Recording Buffer size: " + buffSize);
    	Thread recThread = new Thread(new Runnable(){
    		public void run(){
    	    	while(ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
    	    		int new_samples = ar.read(tmp, tmpEnd, buffSize/2);
    	    		tmpEnd += new_samples;
    	    	}
    	    	Log.d("swiftalk", "Recording Thread Finished");
    		}
    	}, "Audio Recording Thread");
    	ar.startRecording();
    	recThread.start();
    }
    
    private void stopRecording() {
    	//Log.d("swiftalk", "rec length: " + tmpEnd + "  in s: " + tmpEnd / Fs);
    	long s = System.currentTimeMillis();
    	ar.stop();
    	long f = System.currentTimeMillis();
    	Log.d("swiftalk", "ar.stop: " + (f - s));
    	storeTmpAsTrack();
    	eraseTmp();
        long e = System.currentTimeMillis();
        Log.d("swiftalk", "stopRecording: " +( e - s));
    }
    
    private void eraseTmp(){
    	long s = System.currentTimeMillis();
    	for(int i = 0; i<tmp.length; i++){
    		tmp[i] = 0;
    	}
    	tmpEnd = 0;
    	
        long e = System.currentTimeMillis();
        Log.d("swiftalk", "eraseTmp: " +( e - s));
    }
    
    private void storeTmpAsTrack(){
    	long s = System.currentTimeMillis();
    	
    	// Get only the chunk of data that is non-zero
    	short[] tmp_chunk = new short[tmpEnd];
    	int i = 0;
    	try{
	    	for(i = 0; i < tmpEnd; i++){
	    		tmp_chunk[i] = tmp[i];
	    	}
    	}
    	catch(ArrayIndexOutOfBoundsException e){
    		Log.d("swiftalk", "i: " + i + "  tmp_chunk.length: " + tmp_chunk.length + "  tmp.length:" + tmp.length + "  tmpEnd: " + tmpEnd);
    	}
    	
    	int num = tracks.size()+1;;
    	
    	TrackView tv = new TrackView(this, tmp_chunk, num);
    	tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 70));
    	tv.setOnClickListener(new OnClickListener(){
    		@Override
    		public void onClick(View v){
    			Log.d("swiftalk", "onClick");
    			play( ((TrackView)v).getTrackData());
    		}
    	});
    	
        LinearLayout ll = (LinearLayout) findViewById(R.id.linlay);
        ll.addView(tv);
        tracks.add(tv);
        long e = System.currentTimeMillis();
        Log.d("swiftalk", "storeTmpAsTrack: " +( e - s));
    	
    }
    
    private void stopPlaying(){
    	at.stop();
    	at.pause();
    	at.flush();
    }
    
    private void play(short[] data){
    	final short[] d = data;
    	
    	if(at != null){
    		stopPlaying();
    	}
    	
    	int minSize = AudioTrack.getMinBufferSize((int)Fs,
    			AudioFormat.CHANNEL_OUT_MONO, 
    			AudioFormat.ENCODING_PCM_16BIT);
    	
    	final int track_size = Math.max(minSize, d.length);
		at =  new AudioTrack(AudioManager.STREAM_MUSIC,
			(int)Fs,
	    	AudioFormat.CHANNEL_OUT_MONO,
	    	AudioFormat.ENCODING_PCM_16BIT,
	    	track_size,
	    	AudioTrack.MODE_STREAM);
    	
    	Thread t = new Thread(){
    		@Override
    		public void run(){
	    		if(at != null){ 	
			    	if(at.getState() == AudioTrack.STATE_INITIALIZED){
			    		// Tried mode_static, it cut off the end of the sound
			    		at.play();
			    		at.write(d, 0, track_size);
			    		// Log.d("swiftalk", "Playing number: " + num + "  track_size: " + track_size + "  in s:" + track_size/Fs);
			    		at.stop();
			    		at.flush();
			    	}
			    	else{
			    		Toast.makeText(getApplicationContext(), "Slow Down!", Toast.LENGTH_SHORT).show();
			    	}
	    		}
    		}
    	};
    	t.start();

    }
}
