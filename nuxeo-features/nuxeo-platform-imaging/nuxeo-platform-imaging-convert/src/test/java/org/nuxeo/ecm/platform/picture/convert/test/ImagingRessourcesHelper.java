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

package org.nuxeo.ecm.platform.picture.convert.test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;

public class ImagingRessourcesHelper {

    public static final String TEST_DATA_FOLDER = "test-data/";

    public static final List<String> TEST_IMAGE_FILENAMES = Arrays.asList(
            "big_nuxeo_logo.jpg", "big_nuxeo_logo.gif", "big_nuxeo_logo.png");

    public static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assert file.length() > 0;
        return file;
    }

}
