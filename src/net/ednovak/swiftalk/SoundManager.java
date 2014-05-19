package net.ednovak.swiftalk;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

public class SoundManager extends Activity {
	
	final private double Fs = 44100.0;
	private short[][] tracks = new short[10][];
	
	final private short[] tmp = new short[(int)Fs*10];
	private int tmpEnd = 0;
	
	private AudioRecord ar;
	private int btCount = 0;

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
    	
    	Thread recThread = new Thread(new Runnable(){
    		public void run(){
    			gatherAudioSamples(buffSize);
    		}
    	}, "Audio Recording Thread");
    	ar.startRecording();
    	recThread.start();
    }
    
    private void stopRecording() {
    	//Log.d("swiftalk", "rec length: " + tmpEnd + "  in s: " + tmpEnd / Fs);
    	ar.stop();
    	ar.release();
    	storeTmpAsTrack();
    	eraseTmp();
    }
    
    private void gatherAudioSamples(int buffSize){
    	while(ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
    		//Fs/10 is always 1/10 of a second
    		int new_samples = ar.read(tmp, tmpEnd, buffSize/2);
    		tmpEnd += new_samples;
    		//Log.d("swiftalk", "new samples: " + new_samples);
    		
    		//Log.d("swiftalk", "new samples" + tmp[tmpEnd-1] + " " + tmp[tmpEnd-2] + " " + tmp[tmpEnd-3]);
    	}
    }


    private void storeTmpAsTrack(){
    	// ASk the user where
    	
    	tracks[btCount] = new short[tmpEnd];
    	for(int i = 0; i < tmpEnd; i++){
    		tracks[btCount][i] = tmp[i];
    	}
    	
    	addButton(btCount);
    	btCount += 1;
    }
    
    private void eraseTmp(){
    	for(int i = 0; i<tmp.length; i++){
    		tmp[i] = 0;
    	}
    	tmpEnd = 0;
    }
    
    private void addButton(int number){
    	final int num = number;
    	Button bt = new Button(this);
    	bt.setText("Recording " + number++);
    	bt.setId(number);
        bt.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout ll = (LinearLayout) findViewById(R.id.linlay);
    	ll.addView(bt);
    	
    	//Log.d("swifttalk", "Button should be up!");
    	
    	bt.setOnClickListener(new OnClickListener(){
    		@Override
    		public void onClick(View v){
    			play(num);
    		}
    	});
    	
    	bt.setOnLongClickListener(new OnLongClickListener(){
    		@Override
    		public boolean onLongClick(View v){
    			slowDown(num);
    			return true;
    		}
    	});
    }
    
    private void play(int num){
    	int minSize = AudioTrack.getMinBufferSize((int)Fs,
    			AudioFormat.CHANNEL_OUT_MONO, 
    			AudioFormat.ENCODING_PCM_16BIT);
    	
    	int track_size = Math.max(minSize, tracks[num].length);
    	
    	AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC,
    			(int)Fs,
    			AudioFormat.CHANNEL_OUT_MONO,
    			AudioFormat.ENCODING_PCM_16BIT,
    			track_size,
    			AudioTrack.MODE_STREAM);
    	
    	// Tried mode_static, it cut off the end of the sound
    	at.play();
    	at.write(tracks[num], 0, track_size);
    	//Log.d("swiftalk", "Playing number: " + num + "  track_size: " + track_size + "  in s:" + track_size/Fs);
    	at.stop();
    }
    
    private void slowDown(int num){
    	short[] origTrack = tracks[num];
    	short[] slowTrack = new short[origTrack.length*2];
    	
    	for(int i = 0; i<origTrack.length-1; i++){
    		slowTrack[i*2] = origTrack[i];
    		slowTrack[i*2 + 1] = (short)((origTrack[i] + origTrack[i+1]) / 2.0);
    	}
    	
    	tracks[num] = slowTrack;	
    }
    
    private void speedUp(int num){
    	short[] origTrack = tracks[num];
    	short[] fastTrack = new short[origTrack.length/2];
    	
    	for(int i = 0; i < fastTrack.length; i++){
    		fastTrack[i] = origTrack[i*2];
    	}
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sound_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //A placeholder fragment containing a simple view.
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.sound_manager_frag, container, false);
            return rootView;
        }
    }
    */

}
