/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public static final List<String> TEST_IMAGE_FILENAMES = Arrays.asList("cat.gif", "andy.bmp");

    public static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assert file.length() > 0;
        return file;
    }

}
