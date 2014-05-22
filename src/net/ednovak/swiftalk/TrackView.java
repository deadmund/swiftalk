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
import android.widget.Toast;

public class TrackView extends View {
	
	private int track_num;
	private Paint p_bg;
	private Paint p_border;
	private Paint p_text;
	private Paint p_data;
	private Path path;
	private Shader shader;
	
	private short[] data;
	private short dMax;
	
	private final int DARK_BLUE = Color.rgb(70, 95, 255);
	private final int LIGHT_BLUE = Color.rgb(160, 207, 255);
	
	private GestureDetector mDetector;
	
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
		p_data.setStrokeWidth(3);
		p_data.setStyle(Paint.Style.STROKE);
		p_data.setAlpha(165);
		
		p_border = new Paint();
		
		path = new Path();
		shader = new Shader();
		
		
        // Create a gesture detector to handle onTouch messages
        mDetector = new GestureDetector(TrackView.this.getContext(), new GestureListener());
        // Turn off long press--this control doesn't use it, and if long press is enabled,
        // you can't scroll for a bit, pause, then scroll some more (the pause is interpreted
        // as a long press, apparently)
        mDetector.setIsLongpressEnabled(false);

	}
	
	public void setTrackData(short[] d){
		data = d;
		dMax = absMax(d);
		//dMin = min(d);
		invalidate();
		requestLayout();
	}
	
	public short[] getTrackData(){
		return data;
	}
	
	public int getTrackNumber(){
		return track_num;
	}
	
	public void setTrackNumber(int num){
		track_num = num;
		invalidate();
		requestLayout();
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
		
		
		// Data
		if(!isInEditMode() && data != null){
		path.moveTo(0,  h/2);
		for(int i = 0; i < data.length; i++){	
			double half_h = (double)h/2.0;
			int y = (int)((( (double)data[i] / (double)dMax ) * half_h) + half_h);
			path.lineTo((int)((double)i/(double)data.length * w), y );
		}
		
		c.drawPath(path, p_data);
		
		long end = System.currentTimeMillis();
		Log.d("swiftalk", "finished drawing TrackView: " + (end - start) + "ms");
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		boolean res = mDetector.onTouchEvent(event);
		if(!res){
			return super.onTouchEvent(event);
		}
		return res;
	}	

	
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
			//animate().translationXBy(distanceX);
			return false;
		}
		
		public boolean onFling(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
			Log.d("swiftalk", "onFling");
			
			float sensitivity  = 50;
			if(e1.getX() - e2.getX() > sensitivity){
				slowDown();
				makeToast("Slow Down " + getTrackNumber() + "!");
				Log.d("swiftalk", "slow down");
				return true;
			}
			else if(e2.getX() - e1.getX() > sensitivity){
				speedUp();
				makeToast("Speed Up " + getTrackNumber() + "!");
				Log.d("swiftalk", "speed up");
				return true;
			}
			return false;
		}
		
		public boolean onDown(MotionEvent e){
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
    }
    
    private void slowDown(){
    	short[] origTrack = data;
    	short[] slowTrack = new short[origTrack.length*2];
    	
    	for(int i = 0; i<origTrack.length-1; i++){
    		slowTrack[i*2] = origTrack[i];
    		slowTrack[i*2 + 1] = (short)((origTrack[i] + origTrack[i+1]) / 2.0);
    	}
    	
    	data = slowTrack;
    }
    
	private short absMax(short[] d){
		short max = 0;
		for(int i = 0; i < d.length; i++){
			if (d[i] > max){
				max = d[i];
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

}
