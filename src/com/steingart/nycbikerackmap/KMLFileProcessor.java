package com.steingart.nycbikerackmap;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

import com.google.android.gms.maps.model.Marker;
import com.steingart.nycbikerackmap.quadtree.QuadTree;

public class KMLFileProcessor {
    // We don't use namespaces
    private static final String ns = null;
   
    public QuadTree parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }
	
    private QuadTree readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        QuadTree placemarks = new QuadTree(  -74.310019, 40.492060, -73.521688, 40.966607);

        parser.require(XmlPullParser.START_TAG, ns, "Document");
        int counter = 0;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("Placemark")) {
            	Placemark pm = readPlacemark(parser);
            	if(pm.longitude==0 || pm.latitude==0){
            		
            	}else{
            		placemarks.set(pm.longitude, pm.latitude, pm);
            		counter++;
            	}
            } else {
                skip(parser);
            }
        }  
        return placemarks;
    }
    
    
    public class Placemark {
    	public final String name;
    	public final String address;
    	public final String racks;
    	public final float latitude;
    	public final float longitude;
    	
    	private Placemark(String name, String address, String racks, float latitude, float longitude) {
    	    this.name = name;
    	    this.address = address;
    	    this.racks = racks;
    	    this.latitude = latitude;
    	    this.longitude = longitude;
    	}
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
	// to their respective "read" methods for processing. Otherwise, skips the tag.
	private Placemark readPlacemark(XmlPullParser parser) throws XmlPullParserException, IOException {
	    parser.require(XmlPullParser.START_TAG, ns, "Placemark");
	    String name = null;
	    String address = null;
	    String racks = null;
	    float latitude = 0;
	    float longitude = 0;
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String tag = parser.getName();
	        if (tag.equals("name")) {
	            name = readName(parser);
	        } else if (tag.equals("address")) {
	            address = readAddress(parser);
	        } else if (tag.equals("ExtendedData")) {
	            racks = readRacks(parser);
	        } else if(tag.equals("Point")){
	        	float[] ret = readCoordinates(parser);
	        	latitude = ret[1];
	        	longitude = ret[0];
	        } else {
	            skip(parser);
	        }
	    }
	    return new Placemark(name, address, racks, latitude, longitude);
	}

	// Processes title tags in the feed.
	private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "name");
	    String name = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "name");
	    return name;
	}
	  
	// Processes link tags in the feed.
	private String readAddress(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "address");
	    String address = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "address");
	    return address;
	}

	// Processes summary tags in the feed.
	private String readRacks(XmlPullParser parser) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, ns, "ExtendedData");
	    parser.nextTag();
	    parser.nextTag();
	    String racks= readText(parser);
	    //parser.require(XmlPullParser.END_TAG, ns, "ExtendedData");
	    //System.out.println(parser.next());
	    //System.out.println(parser.getName());
	    parser.nextTag();
	    parser.nextTag();
	    
	    return racks;
	}

	// For the tags title and summary, extracts their text values.
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}
	
	private float[] readCoordinates(XmlPullParser parser) throws IOException, XmlPullParserException{
		parser.require(XmlPullParser.START_TAG, ns, "Point");
		parser.nextTag();
	    String[] coordinates= readText(parser).split(",");
	    parser.nextTag();
	    //parser.require(XmlPullParser.END_TAG, ns, "Point");
	    float[] flcoords = new float[coordinates.length];
	    for(int k=0; k<coordinates.length; k++){
	    	flcoords[k] = Float.valueOf(coordinates[k]);
	    }
	    return flcoords;
	}
	
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }

}

