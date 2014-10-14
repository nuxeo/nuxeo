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

package org.nuxeo.ecm.platform.picture.core.test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;

class ImagingResourcesHelper {

    public static final String TEST_DATA_FOLDER = "images/";

    public static final List<String> TEST_IMAGE_FILENAMES = Arrays.asList(
            "cat.gif", "andy.bmp");

    public static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assert file.length() > 0;
        return file;
    }

}
