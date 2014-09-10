package org.mapsforge.android.maps.mapgenerator.mbtiles;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.MapRenderer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

/**
 * @author Robert Oehler class which uses a local mbtiles database to render tiles for the mapsforge library
 */

public class MbTilesDatabaseRenderer implements MapRenderer {

	private final static String TAG = MbTilesDatabaseRenderer.class.getSimpleName();

	private MbTilesDatabase db;

	private boolean isDBOpen = false;

	public MbTilesDatabaseRenderer(final Context pContext, final String dbName) {

		this.db = new MbTilesDatabase(pContext, dbName);

	}

	/**
	 * called from MapWorker: executes a mapgeneratorJob and modifies the @param bitmap which will be the result
	 * according to the parameters inside @param mapGeneratorJob
	 */
	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {

		final Tile tile = mapGeneratorJob.tile;

		long localTileX = tile.tileX;
		long localTileY = tile.tileY;

		// conversion needed to fit the MbTiles coordinate system
		final int[] tmsTileXY = googleTile2TmsTile(localTileX, localTileY, tile.zoomLevel);

		// Log.d(TAG,String.format("Tile requested %d %d is now %d %d", tile.tileX, tile.tileY, tmsTileXY[0],
		// tmsTileXY[1]));

		byte[] rasterBytes = null;
		Bitmap decodedBitmap = null;
		int[] pixels = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];

		rasterBytes = this.db.getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]),
				Byte.toString(tile.zoomLevel));

		if (rasterBytes == null) {

			// got nothing,return to zoom for higher zoom levels
			if (tile.zoomLevel > 11) { // TODO register the "real" max zoom level and compare here
				return false;
			}
			// got nothing,make white pixels for lower zoom levels
			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = 0xff << 24 | (0xff << 16) | (0xff << 8) | 0xff;
			}
		} else {

			decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);

			// check if the input stream could be decoded into a bitmap
			if (decodedBitmap != null) {
				// copy all pixels from the decoded bitmap to the color array
				decodedBitmap.getPixels(pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
				decodedBitmap.recycle();
			} else {
				for (int i = 0; i < pixels.length; i++) {
					pixels[i] = Color.WHITE;
				}
			}
		}
		if (bitmap == null) {
			Bitmap.Config conf = Bitmap.Config.ARGB_8888;
			bitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, conf);
		}

		// copy all pixels from the color array to the tile bitmap
		bitmap.setPixels(pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);

		return true;
	}

	/**
	 * @return the center point of this database data's bounding box
	 */
	@Override
	public GeoPoint getStartPoint() {

		this.db.openDataBase();
		final BoundingBox bb = this.db.getBoundingBox();
		this.db.close();
		try {
			return bb.getCenterPoint();
		} catch (NullPointerException e) {
			return new GeoPoint(43.7242359188, 10.9463005959);
		}
	}

	/**
	 * @return the default start zoom level of this renderer
	 */
	@Override
	public Byte getStartZoomLevel() {

		// this could be read from the db too, but actually Mapsforge uses the zoom the user is using anyway
		return Byte.valueOf((byte) 8);
	}

	/**
	 * @return the max zoom level this renderer has data for
	 */
	@Override
	public byte getZoomLevelMax() {

		// this could be read from the db too, but to allow to zoom deeper higher levels are possible
		return 22;
	}

	/**
	 * @return the currently used db file name
	 */
	@Override
	public String getFileName() {

		return this.db.getDBName();
	}

	@Override
	public void start() {

		if (!this.isDBOpen) {
			this.db.openDataBase();
			this.isDBOpen = true;
		}

	}

	@Override
	public void stop() {

		if (this.isDBOpen) {
			this.db.close();
			this.isDBOpen = false;
		}
	}

	@Override
	public boolean isWorking() {

		return this.isDBOpen;
	}

	/**
	 * closes and destroys any resources needed
	 */
	@Override
	public void destroy() {

		if (this.db != null) {
			this.db = null;
		}

	}

	/**
	 * Converts Google tile coordinates to TMS Tile coordinates.
	 * <p>
	 * Code copied from: http://code.google.com/p/gmap-tile-generator/
	 * </p>
	 * 
	 * @param tx
	 *            the x tile number.
	 * @param ty
	 *            the y tile number.
	 * @param zoom
	 *            the current zoom level.
	 * @return the converted values.
	 */

	public static int[] googleTile2TmsTile(long tx, long ty, byte zoom) {
		return new int[] { (int) tx, (int) ((Math.pow(2, zoom) - 1) - ty) };
	}

}
