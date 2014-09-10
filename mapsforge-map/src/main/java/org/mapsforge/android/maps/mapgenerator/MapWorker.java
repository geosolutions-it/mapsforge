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
package org.mapsforge.android.maps.mapgenerator;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.PausableThread;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.DatabaseRenderer;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;

/**
 * A MapWorker uses a {@link DatabaseRenderer} to generate map tiles. It runs in a separate thread to avoid blocking the
 * UI thread.
 */
public class MapWorker extends PausableThread {
	private static final String THREAD_NAME = "MapWorker";

	private MapRenderer mapRenderer;
	private final TileCache fileSystemTileCache;
	private final TileCache inMemoryTileCache;
	private final JobQueue jobQueue;
	private final MapView mapView;
	private final Bitmap tileBitmap;

	/**
	 * @param mapView
	 *            the MapView for which this MapWorker generates map tiles.
	 */
	public MapWorker(MapView mapView) {
		super();
		this.mapView = mapView;
		this.jobQueue = mapView.getJobQueue();
		this.inMemoryTileCache = mapView.getInMemoryTileCache();
		this.fileSystemTileCache = mapView.getFileSystemTileCache();
		this.tileBitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Bitmap.Config.RGB_565);
	}

	/**
	 * @param pMapRenderer
	 *            the DatabaseRenderer which this MapWorker should use.
	 */
	public void setDatabaseRenderer(MapRenderer pMapRenderer) {
		this.mapRenderer = pMapRenderer;
	}

	@Override
	protected void afterRun() {
		this.tileBitmap.recycle();
	}

	@Override
	protected void doWork() {

		// open the MBTiles DB if necessary
		if (!this.mapRenderer.isWorking()) {
			this.mapRenderer.start();
		}

		MapGeneratorJob mapGeneratorJob = this.jobQueue.poll();

		if (this.inMemoryTileCache.containsKey(mapGeneratorJob) && this.mapView.usesMapsforgeBackground()) {
			return;
		} else if (this.fileSystemTileCache.containsKey(mapGeneratorJob) && this.mapView.usesMapsforgeBackground()) {
			return;
		}

		boolean success = this.mapRenderer.executeJob(mapGeneratorJob, this.tileBitmap);

		if (!isInterrupted() && success) {
			if (this.mapView.getFrameBuffer().drawBitmap(mapGeneratorJob.tile, this.tileBitmap)) {

				if (this.mapView.usesMapsforgeBackground()) {
					this.inMemoryTileCache.put(mapGeneratorJob, this.tileBitmap);
			}
			}
			this.mapView.postInvalidate();

			if (this.mapView.usesMapsforgeBackground()) {
				this.fileSystemTileCache.put(mapGeneratorJob, this.tileBitmap);
			}
		}
		// close the MB Tiles DB if queue is empty
		if (this.jobQueue.isEmpty()) {
			this.mapRenderer.stop();
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return !this.jobQueue.isEmpty();
	}
}
