/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.impl.blob.LazyBlob;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLContentProperty} gives access to a blob, which consists of a
 * {@link Node} of a specialized type: {@code content}. One of the columns of
 * the row stores (indirectly) actual blob data.
 *
 * @author Florent Guillaume
 */
public class SQLContentProperty extends SQLComplexProperty {

    public SQLContentProperty(Node node, ComplexType type, SQLSession session) {
        super(node, type, session);
    }

    @Override
    public Object getValue() throws DocumentException {
        Map<String, Object> map = (Map<String, Object>) super.getValue();
        try {
            String sid = getSession().getUserSessionId();
            SQLBlob blob = new SQLBlob(node);
            String repositoryName = getRepository().getName();
            return new LazyBlob(blob.getStream(), blob.getEncoding(),
                    blob.getMimeType(), sid, node.getPath() + '/' + "DATA",
                    repositoryName, blob.getFilename(), blob.getDigest(),
                    blob.getLength());
        } catch (Exception e) {
            throw new DocumentException("Failed to create SQL lazy blob", e);
        }
    }

    @Override
    public void setValue(Object value) throws DocumentException {
        // XXX AT: set null value insted of HashMap, waiting for NXP-912
        if (value instanceof Blob) {
            Blob blob = (Blob) value;
            if (blob.getMimeType() == null) {
                blob.setMimeType("application/octet-stream");
            }
            SQLBlob.setContent(node, (Blob) value);
        } else {
            SQLBlob.setContent(node, null);
        }
    }

}
