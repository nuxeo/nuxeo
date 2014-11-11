/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.service.test;

import java.io.File;
import java.util.GregorianCalendar;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.adapter.PictureTilesAdapter;

public class TestAdapters extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");        
        deployContrib("org.nuxeo.ecm.platform.picture.core",
        "OSGI-INF/commandline-imagemagick-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-adapter-contrib.xml");
        openRepository();
    }

    public void testAdapter() throws Exception {

        DocumentModel root = coreSession.getRootDocument();

        DocumentModel doc = coreSession.createDocumentModel(
                root.getPathAsString(), "file", "File");
        doc.setProperty("dublincore", "title", "MyDoc");
        doc.setProperty("dublincore", "coverage", "MyDocCoverage");
        doc.setProperty("dublincore", "modified", new GregorianCalendar());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);
        doc.setProperty("file", "content", image);
        doc.setProperty("file", "filename", "test.jpg");

        doc = coreSession.createDocument(doc);
        coreSession.save();

        PictureTilesAdapter tilesAdapter = doc.getAdapter(PictureTilesAdapter.class);
        assertNotNull(tilesAdapter);

        PictureTiles tiles = tilesAdapter.getTiles(255, 255, 15);
        assertNotNull(tiles);

        PictureTiles tiles2 = tilesAdapter.getTiles(255, 255, 20);
        assertNotNull(tiles2);

    }

}
