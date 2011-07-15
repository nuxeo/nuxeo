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
 */
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.nuxeo.ecm.automation.client.model.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BlobDataSource implements DataSource {

    protected final Blob blob;

    public BlobDataSource(Blob blob) {
        this.blob = blob;
    }

    public String getContentType() {
        return blob.getMimeType();
    }

    public InputStream getInputStream() throws IOException {
        return blob.getStream();
    }

    public String getName() {
        return blob.getFileName();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("blob data source is read only");
    }

}
