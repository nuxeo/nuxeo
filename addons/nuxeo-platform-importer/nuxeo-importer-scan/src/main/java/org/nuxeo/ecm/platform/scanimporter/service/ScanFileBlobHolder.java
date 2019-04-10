/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;

/**
 *
 * Extended implementation of {@link BlobHolder} - stores properties extracted
 * from XML - stores documentType extracted from XML
 *
 * @author Thierry Delprat
 *
 */
public class ScanFileBlobHolder extends SimpleBlobHolder {

    protected final Map<String, Serializable> properties;

    protected String targetType = null;

    public ScanFileBlobHolder(List<Blob> blobs,
            Map<String, Serializable> properties, String targetType) {
        super(blobs);
        this.properties = properties;
        this.targetType = targetType;
    }

    public ScanFileBlobHolder(Blob blob, String targetType) {
        super(blob);
        this.properties = new HashMap<String, Serializable>();
        this.targetType = targetType;
    }

    @Override
    public Serializable getProperty(String name) throws ClientException {
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
