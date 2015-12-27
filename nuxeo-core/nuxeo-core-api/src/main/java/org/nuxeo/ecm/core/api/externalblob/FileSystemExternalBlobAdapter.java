/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.api.externalblob;

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.PropertyException;

/**
 * File system external adapter that takes the "container" property to set the absolute path of the container folder on
 * the file system.
 *
 * @author Anahide Tchertchian
 */
public class FileSystemExternalBlobAdapter extends AbstractExternalBlobAdapter {

    private static final long serialVersionUID = 1L;

    public static final String CONTAINER_PROPERTY_NAME = "container";

    public String getFileAbsolutePath(String localPath) throws PropertyException {
        String container = getProperty(CONTAINER_PROPERTY_NAME);
        if (container == null) {
            throw new PropertyException(String.format("External blob adapter with prefix '%s' "
                    + "and class '%s' is missing the '%s' property", getPrefix(), getClass().getName(),
                    CONTAINER_PROPERTY_NAME));
        }
        container = container.trim();
        if (!container.endsWith(File.separator)) {
            return String.format("%s%s%s", container, File.separator, localPath);
        } else {
            return String.format("%s%s", container, localPath);
        }
    }

    @Override
    public Blob getBlob(String uri) throws PropertyException, IOException {
        String localPath = getLocalName(uri);
        String path = getFileAbsolutePath(localPath);
        File file = new File(path);
        if (!file.exists()) {
            throw new PropertyException(String.format("Cannot find file at '%s'", path));
        }
        Blob blob = Blobs.createBlob(file);
        blob.setFilename(file.getName());
        return blob;
    }
}
