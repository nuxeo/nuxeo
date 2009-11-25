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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.MultiTiler;

public class TestMagickExecutors extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployContrib("org.nuxeo.ecm.platform.picture.core",
                "OSGI-INF/commandline-imagemagick-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/commandline-imagemagick-contrib.xml");
    }

    public void testTiler() throws Exception {

        String outputPath = System.getProperty("java.io.tmpdir")
                + "/test_tiles/";
        new File(outputPath).mkdir();
        File file = FileUtils.getResourceFileFromContext("test.jpg");

        MultiTiler.tile(file.getAbsolutePath(), outputPath, 255, 255);

        File outDir = new File(outputPath);
        String[] tiles = outDir.list();
        assertTrue(tiles.length > 0);
    }

}
