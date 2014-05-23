package net.ednovak.swiftalk;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

public class TrackView extends View {
	
	private int track_num;
	private Paint p_bg;
	private Paint p_border;
	private Paint p_text;
	private Paint p_data;
	private Path path;
	private Shader shader;
	
	private int speed = 0;
	private short[] data;
	private double dMax;
	
	private final int DARK_BLUE = Color.rgb(70, 95, 255);
	private final int LIGHT_BLUE = Color.rgb(160, 207, 255);
	
	private GestureDetector singleTapDetector;
	private GestureDetector flingDetector;
	
	private static Toast tst;
	
	public TrackView(Context ctx, AttributeSet attrs){
		super(ctx, attrs);
		
		TypedArray a = ctx.getTheme().obtainStyledAttributes(attrs,R.styleable.TrackView, 0, 0);
		try{
			track_num = a.getInteger(R.styleable.TrackView_track_number, 0);
		}
		finally{
			a.recycle();
		}
		
		init();
		
	}
	
	public TrackView(Context ctx){
		super(ctx);
		init();
	}
	
	public TrackView(Context ctx, short[] data, int num){
		super(ctx);
		init();
		setTrackData(data);
		setTrackNumber(num);
	}
	
	private void init(){
		// Set up paints and shader
		p_bg = new Paint();
		p_text = new Paint();
		
		p_data = new Paint();
		p_data.setColor(DARK_BLUE);
		p_data.setStrokeWidth(1);
		p_data.setStyle(Paint.Style.STROKE);
		p_data.setAlpha(165);
		
		p_border = new Paint();
		
		path = new Path();
		shader = new Shader();
		
		
        // Create a gesture detector to handle onTouch messages
        singleTapDetector = new GestureDetector(TrackView.this.getContext(), new singleTapListener());
        flingDetector = new GestureDetector(TrackView.this.getContext(), new flingListener());

	}
	
	public void setTrackData(short[] d){
		data = d;
		dMax = absMax(d);
		//dMin = min(d);
	}
	
	public short[] getTrackData(){
		return data;
	}
	
	public int getTrackNumber(){
		return track_num;
	}
	
	public void setTrackNumber(int num){
		track_num = num;
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int mWidth = MeasureSpec.getSize(widthMeasureSpec);
		int mHeight = MeasureSpec.getSize(heightMeasureSpec);
		
		setMeasuredDimension(mWidth, mHeight);
	}
	
	protected void onDraw(Canvas c){
		super.onDraw(c);
		
		long start = System.currentTimeMillis();
		int h = getMeasuredHeight();
		int w = getMeasuredWidth();
		
		// Background color
		shader = new LinearGradient(0, 0, 0, h, Color.WHITE, Color.BLACK, TileMode.CLAMP);
		p_bg.setShader(shader); 
		p_bg.setAlpha(32);
		p_bg.setStyle(Paint.Style.FILL);
		c.drawRect(0, 0, w, h, p_bg);

		
		// Border
		//p_border.setColor(Color.rgb(0,0,0));
		//p_border.setStrokeWidth(2);
		//p_border.setStyle(Paint.Style.STROKE);
		//c.drawRect(0, 0, w, h, p_border);
		
		// Text
		String text = "Recording " + getTrackNumber();
		p_text.setColor(Color.rgb(0, 98, 98));
		p_text.setTextSize(20);
		p_text.setStyle(Paint.Style.FILL);
		float text_w = p_text.measureText(text); 
		int px = w / 2;
		int py = h / 2;
		c.drawText(text, px-text_w / 2, py, p_text);
		
		if(!isInEditMode() && data != null){
			double half_h = ((double)h / 2.0) - 2.0;
			path.moveTo(0,  (float)half_h);
			//Log.d("swiftalk", "w: " + w + "  h: " + h + "  half_h: " + half_h + "  dMax:" + dMax);
			
			if (data.length > 10*w){ // Plot Abridged
				for(int i = 0; i < w; i++){
					double tmp_data = (double)data[(int)(  ((double)i/(double)w * (double)data.length)  )];
					int y = (int)((( tmp_data / dMax ) * half_h) + half_h);
					int x = i;
					path.lineTo(x, y);
					//Log.d("swiftalk", "x :" + x + "  y:" + y + "  data[i]:" + data[i]);
				}
			}
			else{ // Plot Full
				for(int i = 0; i < data.length; i++){
					int y = (int)((( (double)data[i] / dMax ) * half_h) + half_h);
					int x = (int)((double)i/(double)data.length * w);
					path.lineTo(x, y);
					//Log.d("swiftalk", "x :" + x + "  y:" + y + "  data[i]:" + data[i]);
				}
			}
			c.drawPath(path, p_data);
		}
		long end = System.currentTimeMillis();
		Log.d("swiftalk", "finished drawing TrackView: " + (end - start) + "ms");
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		
		// Events are flowing with with things like (Down) and (Move) 
		//Log.d("swiftalk", "event: " + event.getAction());
		
		// First see if this might be a fling
		// r1 will be true when there is a down and then a move sequentially
		boolean r1 = flingDetector.onTouchEvent(event);
		// r1, true if this is a fling
		
		MotionEvent newEvent = MotionEvent.obtain(event);		
		if(r1){ // If there is a movement, I need to cancel the original down
			// This is because the original down also triggers a timer
			// for onLongClick() at the super.onTouchEvent
			newEvent.setAction(MotionEvent.ACTION_CANCEL);
		}
		
		// Pass the events to the onClick and onLongClick listeners
		// If there is a fling, the original event will have its timer canceled
		r1 = super.onTouchEvent(newEvent);

		// This just listens for single taps to animate the clicking of the button
		boolean r3 = singleTapDetector.onTouchEvent(event);
		// r3 is always true (cause it's only an animation)
		
		return r1; // continue processing events (True || False)
	}

	private class flingListener extends GestureDetector.SimpleOnGestureListener {
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
			//Log.d("swiftalk", "onFling");
			
			e1.setAction(MotionEvent.ACTION_CANCEL);
			
			float sensitivity  = 50;
			if(e1.getX() - e2.getX() > sensitivity){
				if(speed > -3){
					slowDown();
					makeToast("Slow Down " + getTrackNumber() + "!");
				}
				return true;
			}
			else if(e2.getX() - e1.getX() > sensitivity){
				if(speed < 4){
					speedUp();
					makeToast("Speed Up " + getTrackNumber() + "!");
				}
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onSingleTapUp(MotionEvent e){
			return false;
		}
		
	}

	
	private class singleTapListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e){
			//Log.d("swiftalk", "onDown");
			return true;
		}
		
		@Override // When onDown happend but there user has not moved or up yet
		public void onShowPress(MotionEvent e){
			//Log.d("swiftalk", "onShowPress");
		}
		
		
		@Override // User lifts tap
		public boolean onSingleTapUp(MotionEvent e){
			//Log.d("swiftalk", "onSingleTapUp");
			Animation a = AnimationUtils.loadAnimation(TrackView.this.getContext(), R.anim.quick_fade_blink);
			TrackView.this.startAnimation(a);
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e){
			//Log.d("swiftalk", "onSingleTapConfirmed");
			return false;
		}
	}

	
    public void speedUp(){
    	short[] origTrack = data;
    	short[] fastTrack = new short[origTrack.length/2];
    	
    	for(int i = 0; i < fastTrack.length; i++){
    		fastTrack[i] = origTrack[i*2];
    	}
    	
    	data = fastTrack;
    	speed++;
    }
    
    private void slowDown(){
    	
    	short[] origTrack = data;
    	short[] slowTrack = new short[origTrack.length*2];
    	
    	for(int i = 0; i<origTrack.length-1; i++){
    		slowTrack[i*2] = origTrack[i];
    		slowTrack[i*2 + 1] = (short)((origTrack[i] + origTrack[i+1]) / 2.0);
    	}
    	
    	data = slowTrack;
    	speed--;
    }
    
	private double absMax(short[] d){
		double max = 0;
		for(int i = 0; i < d.length; i++){
			if (Math.abs(d[i]) > max){
				max = Math.abs(d[i]);
			}
		}
		return max;
	}
	
	private void makeToast(String text){
		if(tst != null){
			tst.cancel();
		}
		tst = Toast.makeText(TrackView.this.getContext(), text, Toast.LENGTH_SHORT);
		tst.show();
	}
	
	public void darken(){
		
	}

}
