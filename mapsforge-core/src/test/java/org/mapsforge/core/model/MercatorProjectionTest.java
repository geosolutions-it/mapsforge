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
package org.mapsforge.core.model;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.util.MercatorProjection;

public class MercatorProjectionTest {
	private static final String BOUNDING_BOX_TO_STRING = "minLatitude=2.0, minLongitude=1.0, maxLatitude=4.0, maxLongitude=3.0";
	private static final String DELIMITER = ",";
	private static final double MAX_LATITUDE = 4.0;
	private static final double MAX_LONGITUDE = 3.0;
	private static final double MIN_LATITUDE = 2.0;
	private static final double MIN_LONGITUDE = 1.0;

	@Test
	public void highZoomTest() {

		// 11.2386482889489 43.7721526823805
		// 11.2387096191342 43.7722895747372
		// 11.2387106299199 43.772294020615,
		// 11.2387114796373 43.7722984786521,
		// 11.2387455380977
		// 11.2388045652912

		double a = MercatorProjection.longitudeToPixelX(11.2387455380977, (byte) 24);
		double b = MercatorProjection.longitudeToPixelX(11.2388045652912, (byte) 24);
		Assert.assertNotEquals(a, b);
		Assert.assertNotEquals((long) a, (long) b);

	}
}
