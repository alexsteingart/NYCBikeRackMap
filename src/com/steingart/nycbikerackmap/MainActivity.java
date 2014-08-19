package com.steingart.nycbikerackmap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.steingart.nycbikerackmap.KMLFileProcessor.Placemark;
import com.steingart.nycbikerackmap.quadtree.Point;
import com.steingart.nycbikerackmap.quadtree.QuadTree;

public class MainActivity extends Activity {

	
	private GoogleMap mMap;
	private QuadTree placemarks;
	private Map<Point, Marker> currentMarkers;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        new ReadKMLFileTask().execute("2013-cityracks.kml");
    }
    
    
 // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
    private class ReadKMLFileTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... filenames) {
            try {
                return loadKMLFile(filenames[0]);
            } catch (IOException e) {
            	e.printStackTrace();
                return getResources().getString(R.string.xml_error);
            } catch (XmlPullParserException e) {
            	e.printStackTrace();
                return getResources().getString(R.string.xml_error);
            }
        }

        @Override
        protected void onPostExecute(String result) {  
        	mMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        	
        	mMap.setOnCameraChangeListener(new CameraChangeListener());
        	

        	RefreshMap();

        }
    }
    
    private class CameraChangeListener implements OnCameraChangeListener{

		@Override
		public void onCameraChange(CameraPosition arg0) {


			RefreshMap();
			
		}
    	
    }
    
    
    String loadKMLFile(String filename)throws XmlPullParserException, IOException {
    	InputStream is = null;
    	
    	KMLFileProcessor kfp = new KMLFileProcessor();
    	try{
    		is = getAssets().open(filename);
    	
    		QuadTree temp= kfp.parse(is);
    		placemarks = temp;
    	}finally{
    		if(is!=null){
    			is.close();
    		}
    	}
    	
    	
    	return "Success";
    	
    }
    
    private void RefreshMap(){

    	LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
    	Point[] points = placemarks.searchWithin(curScreen.southwest.longitude, curScreen.southwest.latitude, curScreen.northeast.longitude, curScreen.northeast.latitude);
    	Map<Point, Marker> markers = new HashMap<Point, Marker>();
    	for(int k=0; k<points.length;  k++){
    		Placemark p = (Placemark)points[k].getValue();
	        markers.put(points[k], mMap.addMarker(new MarkerOptions()
	        .position(new LatLng(p.latitude, p.longitude))
	        .title(p.racks)
	        .snippet(p.address)
	        ));
	        
    	}
    	if(currentMarkers !=  null){
    		Collection<Point> markersToRemove = CollectionUtils.subtract(currentMarkers.keySet(), markers.keySet());

    		for(Iterator<Point> i = markersToRemove.iterator(); i.hasNext();){
    			Point p = i.next();
    			currentMarkers.get(p).remove();
    		}
    	}
    	currentMarkers = markers;
    		
    }
    
}