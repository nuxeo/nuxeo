/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */

/**
 * @since 6.0
 */
package org.nuxeo.ecm.webapp.filemanager;

import java.io.File;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;

public class NxUploadedFile implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Blob blob;

    public NxUploadedFile(Blob blob) {
        this.blob = blob;
    }

    public Blob getBlob() {
        return blob;
    }

    public String getContentType() {
        return blob.getMimeType();
    }

    public File getFile() {
        return blob.getFile();
    }

    public String getName() {
        return blob.getFilename();
    }

}
