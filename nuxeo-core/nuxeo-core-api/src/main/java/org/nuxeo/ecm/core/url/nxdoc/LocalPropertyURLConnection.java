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

package org.nuxeo.ecm.core.url.nxdoc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.url.nxobj.ObjectURLConnection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LocalPropertyURLConnection extends ObjectURLConnection {

    protected Property property;

    LocalPropertyURLConnection(URL url) {
        super(url);
    }

    public Property getProperty() throws IOException {
        try {
            if (property == null) {
                String xpath = url.getPath();
                property = ((DocumentModel) obj).getProperty(xpath);
            }
            return property;
        } catch (PropertyException e) {
            IOException ee = new IOException("Failed to get property: " + url.getPath());
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    protected long lastModified() throws IOException {
        DocumentPart part = ((DocumentModel)obj).getPart("dublincore");

        if (part != null) {
            try {
                Calendar cal = (Calendar)part.get("modified");
                if (cal != null) {
                    return cal.getTimeInMillis();
                }
            } catch (PropertyException e) {
                IOException ee = new IOException("Failed to get last modified property");
                ee.initCause(e);
                throw ee;
            }
        }

        return getProperty().isDirty() ? -1L : 0L;
    }

    @Override
    protected InputStream openStream() throws IOException {
        Property p = getProperty();
        try {
            Object value = p.getValue();
            if (value == null) {
                return new ByteArrayInputStream(new byte[0]);
            }
            if (value instanceof Blob) {
                return ((Blob)value).getStream();
            }
            if (value instanceof InputStream) {
                return (InputStream)value;
            }
            return new ByteArrayInputStream(value.toString().getBytes());
        } catch (PropertyException e) {
            throw new IOException("Failed to get property value: " + p.getName());
        }
    }

}
