/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.services.streaming;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StringSource extends AbstractStreamSource {

    protected final String string;


    public StringSource(String string) {
        this.string = string;
    }

    @Override
    public long getLength() throws IOException {
        return string.length();
    }

    @Override
    public InputStream getStream() throws IOException {
        return new ByteArrayInputStream(string.getBytes());
    }

}
