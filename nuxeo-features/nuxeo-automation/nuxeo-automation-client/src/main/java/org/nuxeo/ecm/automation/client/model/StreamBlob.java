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
package org.nuxeo.ecm.automation.client.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings("serial")
public class StreamBlob extends Blob {

    protected final InputStream in;

    public StreamBlob(InputStream in, String name, String ctype) {
        super(name, ctype);
        this.in = in;
    }

    @Override
    public InputStream getStream() throws IOException {
        return in;
    }

}
