package net.ednovak.swiftalk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SoundManager extends Activity {
	
	final Context ctx = this;
	LinearLayout ll;
	
	final private double Fs = 44100.0;
	
	final private short[] tmp = new short[(int)Fs*10];
	private int tmpEnd = 0;
	
	private AudioRecord ar;
	private AudioTrack at;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sound_manager_layout);
        
        ll = (LinearLayout) findViewById(R.id.linlay);
    }
    
    public void newRecording(View v){
    	boolean recording = ((ToggleButton) v).isChecked();
    	//Log.d("swiftalk", "recording: " + recording);
    	if(recording){  // BEGIN RECORDING
    		if (ll.getChildCount() < 4){ // ONLY CAN STORE 4 TRACKS!
    			startRecording();
        		recLightAnimation(recording);
    		}
    		else{
    			((ToggleButton)v).setChecked(false);
    			Toast.makeText(ctx, "You must delete a recording first!", Toast.LENGTH_SHORT).show();
    		}
    	}

    	else{ // STOP RECORDING
			stopRecording();
			recLightAnimation(recording);
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

    	final TrackView tv = new TrackView(this, tmp_chunk, ll.getChildCount()+1);
    	tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 70));
    	tv.setOnClickListener(new OnClickListener(){
    		@Override
    		public void onClick(View v){
    			Log.d("swiftalk", "onClick");
    			play( ((TrackView)v).getTrackData());
    		}
    	});
    	
        ll.addView(tv);
    	
    	tv.setLongClickable(true); // not sure if this is needed
    	tv.setOnLongClickListener(new OnLongClickListener() {
    		@Override
    		public boolean onLongClick(View v){
    			Log.d("swiftalk", "SoundManager LongClick");
    			AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
    			adb.setTitle("Delete Recording");
    			adb.setMessage("Delete Recording " + tv.getTrackNumber() + "?");
    			adb.setIcon(android.R.drawable.ic_dialog_alert);
    			adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    			    public void onClick(DialogInterface dialog, int whichButton) {
    			        ll.removeView(tv);
    			        renumber();
    			        ll.invalidate();
    			    }});
    			adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
    				public void onClick(DialogInterface dialog, int whichButton){
    					dialog.cancel();
    				}
    			});
    			AlertDialog ad = adb.create();
    			ad.show();
    			
    			return false;
    		}
    	});



        long e = System.currentTimeMillis();
        Log.d("swiftalk", "storeTmpAsTrack: " +( e - s));
    	
    }
    
    private void stopPlaying(){
    	try{
    		at.stop();
    		at.pause();
    		at.flush();
    	}
    	catch (Exception e){};
    }
    
    private void play(short[] data){
    	Log.d("swiftalk", "on play");
    	final short[] d = data;
    	
    	stopPlaying();
    	
    	final int buffSize = AudioTrack.getMinBufferSize((int)Fs,
    			AudioFormat.CHANNEL_OUT_MONO, 
    			AudioFormat.ENCODING_PCM_16BIT)*2;
    	
		at =  new AudioTrack(AudioManager.STREAM_MUSIC,
			(int)Fs,
	    	AudioFormat.CHANNEL_OUT_MONO,
	    	AudioFormat.ENCODING_PCM_16BIT,
	    	buffSize,
	    	AudioTrack.MODE_STREAM);
    	
    	Thread t = new Thread(){
    		@Override
    		public void run(){
	    		if(at != null){ 	
			    	if(at.getState() == AudioTrack.STATE_INITIALIZED){
			    		// Tried mode_static, it cut off the end of the sound
			    		at.play();
			    		writeToBuffer(d, buffSize);
			    		stopPlaying();
			    	}
	    		}
    		}
    	};
    	t.start();

    }
    
    private void writeToBuffer(short[] data, int buff_size){
    	short[] chunk = new short[buff_size/2];
    	int i = 0;
    	while(i < data.length){
    		
    		// make a chunk
    		for(int j = 0; i < data.length && j < chunk.length; j++){
    			chunk[j] = data[i];
    			i++;
    		}
    		
    		// write the chunk
    		int a = at.write(chunk, 0, chunk.length);
    		//Log.d("swiftalk", "wrote " + a + " shorts");
    	}
    }
    
    private void recLightAnimation(boolean r){
    	ImageView iv = (ImageView)findViewById(R.id.rec_image);
    	if(r){
    		iv.setVisibility(View.VISIBLE);
    		Animation a = AnimationUtils.loadAnimation(ctx, R.anim.blink);
    		iv.startAnimation(a);
    	}
    	else{
    		iv.clearAnimation();
    		iv.setVisibility(View.INVISIBLE);
    	}
    }
    
    private void renumber(){
    	for(int i = 0; i < ll.getChildCount(); i++){
    		TrackView tv = (TrackView)(ll.getChildAt(i));
    		tv.setTrackNumber(i+1);
    		tv.invalidate();
    	}
    }
}
