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
package org.nuxeo.runtime.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A named stream used to be able to deploy new components without referring to
 * them via URLs.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface StreamRef {

    /**
     * Get an unique identifier for this stream.
     */
    String getId();

    /**
     * Get the stream content.
     *
     * @return
     */
    InputStream getStream() throws IOException;

    /**
     * Get an URL to that stream. May return null if no URL is available.
     *
     * @return
     */
    URL asURL();
}
