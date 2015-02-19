/*
 * (C) Copyright 2007-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

/**
 * Available choice when uploading a file.
 */
public class InputFileChoice {

    /**
     * No file exists currently and no new one is added.
     */
    public static final String NONE = "none";

    /**
     * A file exists and is kept.
     */
    public static final String KEEP = "keep";

    /**
     * A file has been uploaded but there was a validation error in another field.
     */
    public static final String KEEP_TEMP = "tempKeep";

    /**
     * A file has been uploaded.
     * <p>
     * Also used as a prefix for all custom upload modes.
     */
    public static final String UPLOAD = "upload";

    /**
     * A file exists and should be deleted.
     */
    public static final String DELETE = "delete";

    private InputFileChoice() {
        // utility class
    }

    public static boolean isKeepOrKeepTemp(String choice) {
        return KEEP_TEMP.equals(choice) || KEEP.equals(choice);
    }

    public static boolean isUploadOrKeepTemp(String choice) {
        return KEEP_TEMP.equals(choice) || isUpload(choice);
    }

    public static boolean isUpload(String choice) {
        return UPLOAD.equals(choice) || choice.startsWith(UPLOAD);
    }

}
