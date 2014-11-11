/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client;

import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingInfo;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * @author Alexandre Russel
 *
 */
public class GwtTestTilingInfo extends GWTTestCase {
    private TilingInfo info = new TilingInfo("docId", "repoId", "/nuxeo");

    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.pictures.tiles.gwt.TilingPreview";
    }

    public void testParseResponse() {
        info.parseResponse(jsonString);
        assertTrue(info.getZoom() > 0.2);
        assertTrue(info.getZoom() < 0.3);
    }

    private static String jsonString = "{"
            + "        \"originalImage\": {"
            + "         \"width\": 2386,"
            + "         \"height\": 3567,"
            + "         \"format\": \"JPEG\""
            + "        },"
            + "        \"additionalInfo\": {"
            + "         \"XTiles\": \"3\","
            + "         \"outputDirPath\": \"/tmp/default_8c3fb786-a397-44f1-9ef6-c425195b9f43_file_content/tiles-200-200-4\","
            + "         \"YTiles\": \"4\","
            + "         \"TilesWidth\": \"200\","
            + "         \"TilesHeight\": \"200\","
            + "         \"MaxTiles\": \"4\"" + "        },"
            + "        \"srcImage\": {" + "         \"width\": 1193,"
            + "         \"height\": 1784," + "         \"format\": \"JPEG\""
            + "        }," + "        \"tileInfo\": {"
            + "         \"zoom\": 0.25146690011024475,"
            + "         \"xtiles\": 3," + "         \"maxtiles\": 4,"
            + "         \"ytiles\": 4," + "         \"tileHeight\": 200,"
            + "         \"tileWidth\": 200" + "        }" + "       }";
}
