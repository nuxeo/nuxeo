/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.osm.ObjectAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter handling edit from a map, and handling read returning a blob.
 * <p>
 * Create/edit from a blob is not handled, and the blob uri cannot be retrieved
 * from the blob (no api for now).
 *
 * @author Anahide Tchertchian
 */
public class ExternalBlobObjectAdapter implements ObjectAdapter {

    private static final long serialVersionUID = 1L;

    // constants based on core-types.xsd fields. XXX Should be in model
    public static final String ENCODING = "encoding";

    public static final String MIME_TYPE = "mime-type";

    public static final String FILE_NAME = "name";

    public static final String DIGEST = "digest";

    public static final String LENGTH = "length";

    public static final String URI = "uri";

    public Object create(Map<String, Object> value) {
        if (value == null) {
            // return default value
            return getMapFromBlob(null);
        }
        String uri = (String) value.get(URI);
        if (uri == null || "".equals(uri)) {
            return null;
        }
        String filename = (String) value.get(FILE_NAME);
        String mimeType = (String) value.get(MIME_TYPE);
        String encoding = (String) value.get(ENCODING);
        String digest = (String) value.get(DIGEST);
        try {
            BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
            if (service == null) {
                throw new DocumentException(
                        "BlobHolderAdapterService not found");
            }
            Blob blob = service.getExternalBlobForUri(uri);
            if (filename != null) {
                blob.setFilename(filename);
            }
            blob.setMimeType(mimeType);
            blob.setEncoding(encoding);
            // TODO maybe check if digest is still a match to the retrieved blob
            blob.setDigest(digest);
            return blob;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectAdapter getAdapter(String name)
            throws PropertyNotFoundException {
        // not sure what should be returned here
        return null;
    }

    protected HashMap<String, Object> getMapFromBlob(Blob blob) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (blob == null) {
            map.put(URI, null);
            map.put(FILE_NAME, null);
            map.put(MIME_TYPE, null);
            map.put(ENCODING, null);
            map.put(LENGTH, null);
            map.put(DIGEST, null);
        } else {
            // cannot return uri for blob for now: no edit implemented
            map.put(URI, null);
            map.put(FILE_NAME, blob.getFilename());
            map.put(MIME_TYPE, blob.getMimeType());
            map.put(ENCODING, blob.getEncoding());
            map.put(LENGTH, blob.getLength());
            map.put(DIGEST, blob.getDigest());
        }
        return map;
    }

    public Serializable getDefaultValue() {
        return getMapFromBlob(null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(Object object) throws PropertyException {
        if (object instanceof Map) {
            return (Map) object;
        } else if (object instanceof Blob) {
            return getMapFromBlob((Blob) object);
        } else if (object != null) {
            throw new PropertyException(String.format(
                    "Invalid value for an external blob (map or blob needed): "
                            + "object '%s', name '%s'", object));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object getValue(Object object, String name) throws PropertyException {
        if (object instanceof Map) {
            return ((Map) object).get(name);
        } else if (object instanceof Blob) {
            Map<String, Object> map = getMapFromBlob((Blob) object);
            return map.get(name);
        } else if (object != null) {
            throw new PropertyException(String.format(
                    "Invalid value for an external blob (map or blob needed): "
                            + "object '%s', name '%s'", object, name));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void setMap(Object object, Map<String, Object> value)
            throws PropertyException {
        if (object instanceof Map) {
            ((Map) object).clear();
            ((Map) object).putAll(value);
        } else {
            throw new PropertyException(String.format(
                    "Invalid value for an external blob (map needed): "
                            + "object '%s', value '%s'", object, value));

        }
    }

    /**
     * Sets property value in map
     */
    @SuppressWarnings("unchecked")
    public void setValue(Object object, String name, Object value)
            throws PropertyException {
        if (object instanceof Map) {
            ((Map) object).put(name, value);
        } else if (object != null) {
            throw new PropertyException(String.format(
                    "Invalid value for an external blob (map needed): "
                            + "object '%s', property name '%s', value '%s'",
                    object, name, value));
        }
    }
}
