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

package org.nuxeo.ecm.core.repository.jcr.properties;

import java.util.Map;

import javax.jcr.Node;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.runtime.api.Framework;

/**
 * NOT TESTED, DOES NOT WORK
 *
 * <p>
 * Property handling an external blob
 *
 *
 * @author Anahide Tchertchian
 *
 */
public class ExternalBlobProperty extends JCRComplexProperty {

    public static final String ENCODING = "encoding";

    public static final String MIME_TYPE = "mime-type";

    public static final String FILE_NAME = "name";

    public static final String DIGEST = "digest";

    public static final String LENGTH = "length";

    public static final String URI = "uri";

    public ExternalBlobProperty(JCRNodeProxy parent, Node node, Field field) {
        super(parent, node, field);
    }

    @Override
    protected Object doGetValue() throws DocumentException {
        Map<String, Object> map = (Map<String, Object>) super.getValue();
        if (map == null) {
            return null;
        }
        String uri = (String) map.get(URI);
        if (uri == null || "".equals(uri)) {
            return null;
        }
        String filename = (String) map.get(FILE_NAME);
        String mimeType = (String) map.get(MIME_TYPE);
        String encoding = (String) map.get(ENCODING);
        String digest = (String) map.get(DIGEST);
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
        } catch (DocumentException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    @Override
    protected void doSetValue(Object value) throws DocumentException {
        Map<String, Object> map;
        if (value == null) {
            // nothing, use null
            map = null;
        } else if (value instanceof Map) {
            map = (Map) value;
            // XXX: maybe check that all needed info is given (?)
            // map.put(MIME_TYPE, mimeType);
            // map.put(URI, binary);
            // map.put(FILE_NAME, filename);
            // map.put(ENCODING, encoding);
            // map.put(DIGEST, digest);
            // map.put(LENGTH, length);
        } else {
            // do not handle blob for now
            throw new DocumentException(
                    "Setting an invalid value for external blob (map needed): "
                            + value);
        }
        super.setValue(map);
    }

}
