/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
