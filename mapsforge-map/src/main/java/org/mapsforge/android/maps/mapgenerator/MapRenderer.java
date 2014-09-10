package org.mapsforge.android.maps.mapgenerator;

import org.mapsforge.core.model.GeoPoint;

/**
 * an interface which defines what a 'background' renderer must be able to
 * 
 * @author Robert Oehler
 */
public interface MapRenderer {

	public boolean executeJob(MapGeneratorJob mapGeneratorJob, android.graphics.Bitmap bitmap);

	public GeoPoint getStartPoint();

	public Byte getStartZoomLevel();

	public byte getZoomLevelMax();

	public String getFileName();

	public void start();

	public void stop();

	public boolean isWorking();

	public void destroy();

}
