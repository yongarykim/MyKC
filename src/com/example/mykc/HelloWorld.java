package com.example.mykc;

import java.lang.reflect.Field;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

/**
 * A simple demo. This shows more how to use jPCT-AE than it shows how to write
 * a proper application for Android. It includes basic activity management to
 * handle pause and resume...
 * 
 * @author EgonOlsen
 * 
 */
public class HelloWorld extends Activity implements SensorEventListener{

	// Used to handle pause and resume...
	private static HelloWorld master = null;

	private GLSurfaceView mGLView;
	private MyRenderer renderer = null;
	private FrameBuffer fb = null;
	private World world = null;
	private RGBColor back = new RGBColor(0,0,0,0);//,100);

	private float touchTurn = 0;
	private float touchTurnUp = 0;

	private float xpos = -1;
	private float ypos = -1;

	//private Object3D cube = null;
	private static int CUBE_NUM=10;
	private Object3D cubes[]= new Object3D[CUBE_NUM];
	
	private int fps = 0;

	private Light sun = null;

	
	//ky sensor
	private SensorManager sm=null;
	private Sensor accSensor = null;
	private Sensor magSensor = null;
	private Sensor gyroSensor = null;
	private float gravityData[] = new float[3];
	
	//acc
	private float accData[] = new float[3];
	private final float alpha=0.8f;
	//gyro
	private final float NS2S=1.0f/1000000000.0f;
	private final float[] deltaRotataionV=new float[4];
	private float timestamp;
	//ori
	private float oriR[]=new float[16];
	private float oriData[]=new float[3];
	//private float gravityTemp[] = new float[3];
	private float geoMagnetic[] = new float[3];
	
	
	protected void onCreate(Bundle savedInstanceState) {

		Logger.log("onCreate");

		if (master != null) {
			copy(master);
		}

		super.onCreate(savedInstanceState);
		mGLView = new GLSurfaceView(getApplication());

		mGLView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		
		/* JPCT
		mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
			public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
				// Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
				// back to Pixelflinger on some device (read: Samsung I7500)
				int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
				EGLConfig[] configs = new EGLConfig[1];
				int[] result = new int[1];
				egl.eglChooseConfig(display, attributes, configs, 1, result);
				return configs[0];
			}
		});*/

		renderer = new MyRenderer();
		mGLView.setRenderer(renderer);
		
		//mGLView.getHolder().setFormat( PixelFormat.TRANSLUCENT);//투명
		//mGLView.getHolder().setFormat( PixelFormat.RGBA_8888);//알파
		mGLView.getHolder().setFormat( PixelFormat.TRANSPARENT);//알파
		
		setContentView(mGLView);
		
		//ky sensor
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		magSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		//oriSensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		mGLView.onPause();
		
		//ky sensor
		sm.unregisterListener(this,accSensor);
		sm.unregisterListener(this,gyroSensor);
		sm.unregisterListener(this,magSensor);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
		
		//ky sensor
		sm.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI);
		sm.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI);
		sm.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void copy(Object src) {
		try {
			Logger.log("Copying data from master Activity!");
			Field[] fs = src.getClass().getDeclaredFields();
			for (Field f : fs) {
				f.setAccessible(true);
				f.set(this, f.get(src));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean onTouchEvent(MotionEvent me) {

		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			xpos = me.getX();
			ypos = me.getY();
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_UP) {
			xpos = -1;
			ypos = -1;
			touchTurn = 0;
			touchTurnUp = 0;
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_MOVE) {
			float xd = me.getX() - xpos;
			float yd = me.getY() - ypos;

			xpos = me.getX();
			ypos = me.getY();

			touchTurn = xd / -100f;
			touchTurnUp = yd / -100f;
			return true;
		}

		try {
			Thread.sleep(15);
		} catch (Exception e) {
			// No need for this...
		}

		return super.onTouchEvent(me);
	}

	protected boolean isFullscreenOpaque() {
		return true;
	}

	class MyRenderer implements GLSurfaceView.Renderer {

		private long time = System.currentTimeMillis();

		public MyRenderer() {
		}

		public void onSurfaceChanged(GL10 gl, int w, int h) {
			if (fb != null) {
				fb.dispose();
			}
			fb = new FrameBuffer(gl, w, h);

			if (master == null) {

				world = new World();
				world.setAmbientLight(20, 20, 20);

				sun = new Light(world);
				sun.setIntensity(250, 250, 250);

				// Create a texture out of the icon...:-)
				Texture texture = //new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.icon)), 64, 64));
									new Texture(BitmapHelper.convert(getResources().getDrawable(R.drawable.expo_stones)));
									
				
				TextureManager.getInstance().addTexture("texture", texture);

				
				for(int i=0; i<10;i++){
					cubes[i] = Primitives.getCube(0.5f);
					cubes[i].rotateY((float)Math.PI/4f);//look front
					cubes[i].translate(0, 0, i); 
					
					cubes[i].calcTextureWrapSpherical();
					cubes[i].setTexture("texture");
					
					cubes[i].strip();
					cubes[i].build();
					
					world.addObject(cubes[i]);
				}
				
				
				Camera cam = world.getCamera();
				cam.moveCamera(Camera.CAMERA_MOVEOUT, 5);
				cam.moveCamera(Camera.CAMERA_MOVEUP, 2);
				
				cam.lookAt(cubes[0].getTransformedCenter());

				SimpleVector sv = new SimpleVector();
				sv.set(cubes[0].getTransformedCenter());
				sv.y -= 10;
				sv.z -= 10;
				sun.setPosition(sv);
				MemoryHelper.compact();

				if (master == null) {
					Logger.log("Saving master Activity!");
					master = HelloWorld.this;
				}
			}
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		}

		public void onDrawFrame(GL10 gl) {
			if (touchTurn != 0) {
				//cube.rotateY(touchTurn);
				//world.getCamera().moveCamera(Camera.CAMERA_MOVEDOWN, touchTurn);
				
				//world.getCamera().rotateX(0.1f);
				Logger.log( "touch down");
				touchTurn = 0;
			}

			if (touchTurnUp != 0) {
				//cube.rotateX(touchTurnUp);
				//world.getCamera().moveCamera(Camera.CAMERA_MOVEUP,touchTurnUp);
				
				//world.getCamera().rotateY(0.1f);
				Logger.log( "touch UP============");
				touchTurnUp = 0;
			}

			fb.clear(back);
			world.renderScene(fb);
			world.draw(fb);
			fb.display();

			if (System.currentTimeMillis() - time >= 1000) {
				Log.d("JPCT", fps + "fps");
				fps = 0;
				time = System.currentTimeMillis();
			}
			fps++;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if(world==null || world.getCamera()==null)
			return;
		
		synchronized (this){
			if( event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
				//System.arraycopy(event.values,0, gravityTemp,0,3);//for ORIGIN
				
				gravityData[0] = alpha*gravityData[0] + (1-alpha)*event.values[0];
				gravityData[1] = alpha*gravityData[1] + (1-alpha)*event.values[1];
				gravityData[2] = alpha*gravityData[2] + (1-alpha)*event.values[2];
				
				accData[0]=event.values[0] - gravityData[0];
				accData[1]=event.values[1] - gravityData[1];
				accData[2]=event.values[2] - gravityData[2];
				
				//if(Math.abs(accData[2])>0.5f)
				//	Log.i("Sense", " X:"+ accData[0] + " Y:"+ accData[1] + " Z:"+ accData[2]);
				
				/*
				if( accData[2] > 0.5f )
						world.getCamera().moveCamera(Camera.CAMERA_MOVEDOWN, accData[2]/10f );
				if( accData[2] < -0.5f )
						world.getCamera().moveCamera(Camera.CAMERA_MOVEUP, -1f*accData[2]/10f );
				*/
				
			}
			
			else if (event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
				if( timestamp !=0 ) {
					final float dT = (event.timestamp - timestamp)* NS2S;
					
					float magnitude =  (float)Math.sqrt(    event.values[0]*event.values[0] + 
															event.values[1]*event.values[1]+event.values[2]*event.values[2]);
					
					if (magnitude > 0.2) { //need EPSILON tunning. 
						float axisX = event.values[0]/magnitude;
						float axisY = event.values[1]/magnitude;
						float axisZ = event.values[2]/magnitude;

						float thetaOverTwo = magnitude * dT/2.0f;
						
						if( Math.abs(axisX)>0.5f)
							Log.i("Sense", "GYRO: theta=" + thetaOverTwo +"    X="+axisX+ ", Y="+axisY+", Z="+axisZ );
						
						if( axisX>0.5f)
							world.getCamera().moveCamera(Camera.CAMERA_MOVEIN, axisX/10f);
						else if ( axisX<-0.7f)
							world.getCamera().moveCamera(Camera.CAMERA_MOVEOUT, -1f*axisX/12f);
						
						if( axisY>0.5f)
							world.getCamera().moveCamera(Camera.CAMERA_MOVELEFT, axisY/10f);
						else if ( axisY<-0.7f)
							world.getCamera().moveCamera(Camera.CAMERA_MOVERIGHT, -1f*axisY/12f);
					}
				}
				timestamp = event.timestamp;
			}
			else if (event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
				System.arraycopy(event.values,0, geoMagnetic,0,3);
			
				if( Math.abs(geoMagnetic[0])>0.5f || Math.abs(geoMagnetic[1])>0.5f  || Math.abs(geoMagnetic[2])>0.5f ){
					//Log.i("Sense", "Ori:Z="+geoMagnetic[0]+ ", xPitch="+geoMagnetic[1]+", yRoll="+geoMagnetic[2] );
				}
				
			}
			
		}
		

		//for Orientatation =========================================================
		  	//get oriR.  
//			SensorManager.getRotationMatrix(oriR, null, gravityTemp, geoMagnetic);  //gravity, geomagnetic ); //return true false
//		
//		SensorManager.getOrientation(oriR, oriData);
//		
//		if( Math.abs(oriData[0])>0.5f || Math.abs(oriData[1])>0.5f  || Math.abs(oriData[2])>0.5f )
//			Log.i("Sense", "Ori:Z="+oriData[0]+ ", x="+oriData[1]+", y="+oriData[2] );
//		//else	
//		//	Log.e("Sense", "WHAT SENSE:"+event.sensor.getType());
		
		
	}
}