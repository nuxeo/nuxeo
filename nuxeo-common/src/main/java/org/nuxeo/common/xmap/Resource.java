/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
