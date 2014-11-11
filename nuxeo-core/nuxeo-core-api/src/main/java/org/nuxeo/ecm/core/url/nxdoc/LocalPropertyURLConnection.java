/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.url.nxobj.ObjectURLConnection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
        } catch (ClientException e) {
            IOException ee = new IOException("Failed to get property: "
                    + url.getPath());
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    protected long lastModified() throws IOException {
        try {
            try {
                DocumentModel doc = (DocumentModel) obj;
                Calendar cal = (Calendar) doc.getPropertyValue("dc:modified");
                if (cal != null) {
                    return cal.getTimeInMillis();
                }
            } catch (PropertyNotFoundException e) {
                // ignore
            }
        } catch (ClientException e) {
            IOException ee = new IOException(
                    "Failed to get last modified property");
            ee.initCause(e);
            throw ee;
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
                return ((Blob) value).getStream();
            }
            if (value instanceof InputStream) {
                return (InputStream) value;
            }
            return new ByteArrayInputStream(value.toString().getBytes());
        } catch (PropertyException e) {
            throw new IOException("Failed to get property value: "
                    + p.getName());
        }
    }

}
