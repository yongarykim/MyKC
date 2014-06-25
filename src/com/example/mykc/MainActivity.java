package com.example.mykc;

import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends Activity {

	private GoogleMap googleMap;
	private Handler handler;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        try{
        	initilizeMap();
        }catch (Exception e){
        	e.printStackTrace();
        }
        
    }
    
    /**
     * functin to load map. if map is not created this will create for me
     */
    private void initilizeMap(){
    	if (googleMap ==null){
    		googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
    		
    		if(googleMap == null){
    			Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
    		}else{ //success make
    			
    			googleMap.getUiSettings().setZoomControlsEnabled(false);
    			googleMap.setBuildingsEnabled(false);
    			
    			// Getting Current Location
    			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();// Creating a criteria object to retrieve provider
    			String provider = locationManager.getBestProvider(criteria, true);
    	        Location location = locationManager.getLastKnownLocation(provider);
    	        
    			
    			//camera position
    			CameraPosition cameraPosition = new CameraPosition.Builder().target(
    	                new LatLng(location.getLatitude(), location.getLongitude())).zoom(30).tilt(78.69f).build(); //tan(78.69f)=0.2, 
    			
    			
    			//googleMap.setMyLocationEnabled(true);
    			//googleMap.animateCamera( CameraUpdateFactory.newCameraPosition(cameraPosition) ); //need to MyPosition.
    			
    			
    			handler = new Handler();
    			googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition) , 5000, 
    						new CancelableCallback(){
    							@Override
    							public void onFinish(){
    								
    								handler.postDelayed(new Runnable(){
    			                		public void run(){
		    								//new activity
		    				    	    	MainActivity.this.startActivity(new Intent(MainActivity.this, HelloWorld.class));
    			                		}
    								},100);
    				    	    	
    							}

								@Override
								public void onCancel() {
								}
    						}
    					); //callback
    		}
    		
    	}
    }

    
    
    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }
    
//	@Override
//	protected boolean isRouteDisplayed() {
//		// TODO Auto-generated method stub
//		return false;
//	}
}
