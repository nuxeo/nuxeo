/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;

/**
 * Extended implementation of {@link BlobHolder} - stores properties extracted from XML - stores documentType extracted
 * from XML
 *
 * @author Thierry Delprat
 */
public class ScanFileBlobHolder extends SimpleBlobHolder {

    protected final Map<String, Serializable> properties;

    protected String targetType = null;

    public ScanFileBlobHolder(List<Blob> blobs, Map<String, Serializable> properties, String targetType) {
        super(blobs);
        this.properties = properties;
        this.targetType = targetType;
    }

    public ScanFileBlobHolder(Blob blob, String targetType) {
        super(blob);
        this.properties = new HashMap<>();
        this.targetType = targetType;
    }

    @Override
    public Serializable getProperty(String name) {
        if (properties == null) {
            return null;
        }
        return properties.get(name);
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public String getTargetType() {
        if (targetType == null) {
            return ScanFileMappingDescriptor.DEFAULT_LEAF_TYPE;
        }
        return targetType;
    }

}
