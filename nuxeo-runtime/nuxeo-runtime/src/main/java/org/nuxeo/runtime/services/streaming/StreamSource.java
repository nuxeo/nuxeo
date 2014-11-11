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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface StreamSource {

    InputStream getStream() throws IOException;

    byte[] getBytes() throws IOException;

    String getString() throws IOException;

    long getLength() throws IOException;

    void copyTo(File file) throws IOException;

    void copyTo(OutputStream out) throws IOException;

    boolean canReopen();

    void destroy();
}
