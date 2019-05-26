/*
 * (C) Copyright 2007-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
