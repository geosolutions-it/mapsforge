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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mapsforge.core.model.BoundingBox;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
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

	private static String DB_PATH;

	private static String DB_NAME;

	private SQLiteDatabase mDataBase;

	private final Context mContext;

	/**
	 * Constructor takes and keeps a reference of the passed context in order to access to the application assets and
	 * resources.
	 * 
	 * @param context
	 *            of the according mapView
	 * @param dbName
	 *            the name of the database in the /assets order of the application -> this must fit
	 */
	public MbTilesDatabase(Context context, final String dbName) {

		super(context, dbName, null, 1);

		this.mContext = context;

		DB_NAME = dbName;

		// fix for occasional crashes
		// as on newer sdk the directory is different, check and select
		// from
		// http://stackoverflow.com/questions/16636017/e-sqlitelog1893-14-cannot-open-file-at-line-30176-of-00bb9c9ce4
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			DB_PATH = this.mContext.getApplicationInfo().dataDir + "/databases/";
		} else {
			DB_PATH = "/data/data/" + this.mContext.getApplicationContext().getPackageName() + "/databases/";
		}

		try {
			createDataBase();
		} catch (IOException e) {
			Log.e(TAG, "IOE accessing the mbtiles db", e);
		}
	}

	/**
	 * Creates a empty database on the system and rewrites it with the mbtiles database if necessary
	 * 
	 * @throws IOException
	 *             in case of an IOE
	 */
	public void createDataBase() throws IOException {

		boolean dbExist = checkDataBase();

		if (dbExist) {
			// do nothing - database already exist
		} else {

			File databaseDirectory = new File(DB_PATH);
			File[] files = databaseDirectory.listFiles();
			if (files != null) {
				for (File file : files) {
					final String fileName = file.getName();
					if (fileName.substring((fileName.lastIndexOf(".") + 1), fileName.length()).equals("mbtiles")
							|| fileName.substring((fileName.lastIndexOf(".") + 1), fileName.length()).equals(
									"mbtiles-journal")) {

						Log.d(TAG, "Found existing MB Tiles db : " + fileName);
						// for now delete
						file.delete();
					}
				}
			}
			// By calling this method and empty database will be created into the default system path
			// of your application so we are gonna be able to overwrite that database with our database.
			this.getReadableDatabase();

			try {

				this.close();
				copyDataBase();
				Log.d(TAG, "database successfully copied");
				this.openDataBase(); // opening and closing is advisable to test if all works fine
				this.close();

			} catch (IOException e) {

				throw new Error("Error copying database");

			}
		}
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {

		SQLiteDatabase checkDB = null;

		try {

			checkDB = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);

		} catch (SQLiteException e) {

			// database does't exist yet.

		}

		if (checkDB != null) {

			checkDB.close();

		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies the database from local assets-folder to the just created empty database in the system folder, from where
	 * it can be accessed and handled. This is done by transferring bytestream.
	 */
	private void copyDataBase() throws IOException {

		File file = new File(Environment.getExternalStorageDirectory() + "/mapstore/" + DB_NAME);

		if (!file.exists()) {
			throw new IOException("File selected not present, please check your sd card");
		}
		// Open your local db as the input stream
		// InputStream myInput = this.mContext.getAssets().open(DB_NAME);
		InputStream myInput = new FileInputStream(file);

		// Path to the just created empty db
		String outFileName = DB_PATH + DB_NAME;

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		// transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	/**
	 * opens the database from the applications path
	 * 
	 * @throws SQLException
	 */
	public void openDataBase() throws SQLException {

		String myPath = DB_PATH + DB_NAME;
		this.mDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

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
		return DB_NAME;
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
			Log.e(TAG, "NPE retrieving tile from db", e);
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
