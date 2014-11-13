/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.mapgenerator.mbtiles;

import org.mapsforge.core.model.BoundingBox;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * class which connects to a database from downloaded zip bundle on the sdcard. If it is not yet installed in the
 * Android application path, it is copied in there. This follows a tutorial found here:
 * http://www.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/
 * 
 * @author Robert Oehler
 */

public class MbTilesDatabase extends SQLiteOpenHelper {

	private static final String TAG = MbTilesDatabase.class.getSimpleName();

	private static String DB_Path;

	private SQLiteDatabase mDataBase;

	/**
	 * Constructor takes and keeps a reference of the passed context in order to access to the application assets and
	 * resources.
	 * 
	 * @param context
	 *            of the according mapView
	 * @param pDbPath
	 *            the path to the database on the sdcard of the device
	 */
	public MbTilesDatabase(Context context, final String pDbPath) {

		super(context, pDbPath, null, 1);

		DB_Path = pDbPath;

	}

	/**
	 * opens the database from the applications path
	 * 
	 * @throws SQLException
	 */
	public void openDataBase() throws SQLException {

		// String myPath = DB_PATH + DB_NAME;
		this.mDataBase = SQLiteDatabase.openDatabase(DB_Path, null, SQLiteDatabase.OPEN_READONLY);

	}

	/**
	 * closes the database
	 */
	@Override
	public synchronized void close() {

		if (this.mDataBase != null)
			this.mDataBase.close();

		super.close();

	}

	public String getDBName() {
		return DB_Path;
	}

	/**
	 * needed to implement SQLiteOpenHelper
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		// nothing
	}

	/**
	 * needed to implement SQLiteOpenHelper
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// nothing
	}

	// //***Actual DB ACCESS comes here ****/////////

	/**
	 * queries the database for the data of an raster image
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 * @return the data, if available for these coordinates
	 */
	public byte[] getTileAsBytes(String x, String y, String z) {
		try {
			final Cursor c = this.mDataBase.rawQuery(
					"select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
							x, y, z });
			if (!c.moveToFirst()) {
				c.close();
				return null;
			}
			byte[] bb = c.getBlob(c.getColumnIndex("tile_data"));
			c.close();
			return bb;
		} catch (NullPointerException e) {
			Log.e(TAG, "NPE getTileAsBytes", e);
			return null;
		} catch (SQLiteException e) {
			Log.e(TAG, "SQLiteException getTileAsBytes", e);
			return null;
		}
	}

	/**
	 * retrieves the bounding box of the metadata table of the database renders it and return @return an BoundingBox
	 * object
	 * 
	 * @return the boundingbox of the data in this database file
	 */
	public BoundingBox getBoundingBox() {

		try {
			final Cursor c = this.mDataBase.rawQuery("select value from metadata where name=?",
					new String[] { "bounds" });
			if (!c.moveToFirst()) {
				c.close();
				return null;
			}
			final String box = c.getString(c.getColumnIndex("value"));

			String[] split = box.split(",");
			if (split.length != 4) {
				return null;
			}
			double minlon = Double.parseDouble(split[0]);
			double minlat = Double.parseDouble(split[1]);
			double maxlon = Double.parseDouble(split[2]);
			double maxlat = Double.parseDouble(split[3]);

			return new BoundingBox(minlat, minlon, maxlat, maxlon);

		} catch (NullPointerException e) {
			Log.e(TAG, "NPE retrieving boundingbox from db", e);
			return null;
		}
	}

}
