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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.properties;

import javax.jcr.Node;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.impl.blob.LazyBlob;
import org.nuxeo.ecm.core.repository.jcr.JCRBlob;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlobProperty extends JCRComplexProperty {

    public BlobProperty(JCRNodeProxy parent, Node node, Field field) {
        super(parent, node, field);
    }

    @Override
    protected Object doGetValue() throws DocumentException {
        try {
            String sid = getDocument().getSession().getUserSessionId();
            JCRBlob blob = new JCRBlob(node);
            String repositoryName = getDocument().getRepository().getName();
            return new LazyBlob(blob.getStream(), blob.getEncoding(),
                    blob.getMimeType(), sid, node.getPath() + '/'
                            + JCRBlob.DATA, repositoryName,
                     blob.getFilename(), blob.getDigest(), blob.getLength());
        } catch (Exception e) {
            throw new DocumentException("Failed to create JCR lazy blob", e);
        }
    }

    @Override
    protected void doSetValue(Object value) throws DocumentException {
        // XXX AT: set null value insted of HashMap, waiting for NXP-912
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
