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

package org.nuxeo.common.xmap;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A resource that can be retrieved using the class loader.
 * <p>
 * This is wrapping an URL as returned by the class loader.
 * <p>
 * The URL class cannot be used directly because it already has
 * a factory associated to it that constructs the URL using its constructor.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Resource {

    private final URL url;


    public Resource(URL url) {
        this.url = url;
    }

    public Resource(Context ctx, String path) {
        url = ctx.getResource(path);
    }

    public URL toURL() {
        return url;
    }

    public URI toURI() throws URISyntaxException {
        return url != null ? url.toURI() : null;
    }

    public File toFile() throws URISyntaxException {
        return url != null ? new File(url.toURI()) : null;
    }

}
