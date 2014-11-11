/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.io.Serializable;

import javax.jcr.Node;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.LazyBlob;
import org.nuxeo.ecm.core.api.model.Property;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlobPropertyAccessor implements PropertyAccessor {

    public static final String TYPE = "content";


    public void read(Node node, Property property) throws Exception {
        String key = (String)property.getRoot().getData();
        int p = key.indexOf(':');
        if (p == -1) {
            throw new IllegalStateException("No session URI passed through context");
        }
        String repo = key.substring(0, p);
        String sid = key.substring(p+1);
        JCRBlob jcrBlob = new JCRBlob(node);
        LazyBlob blob = new LazyBlob(jcrBlob.getStream(), jcrBlob.getEncoding(),
                jcrBlob.getMimeType(), sid, node.getPath() + '/'
                + JCRBlob.DATA, repo,
                jcrBlob.getFilename(), jcrBlob.getDigest(), jcrBlob.getLength());
        property.init(blob);
    }

    public void write(Node node, Property property) throws Exception {
        Serializable value = property.getValue();
        if (value instanceof Blob) {
            Blob blob = (Blob) value;
            if (blob.getMimeType() == null) {
                blob.setMimeType("application/octet-stream");
            }
            JCRBlob.setContent(node, (Blob) value);
        } else {
            JCRBlob.setContent(node, null);
        }
    }

}
