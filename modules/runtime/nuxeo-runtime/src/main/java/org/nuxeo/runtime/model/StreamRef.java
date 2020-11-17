/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A named stream used to be able to deploy new components without referring to them via URLs.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface StreamRef {

    /**
     * Get an unique identifier for this stream.
     */
    String getId();

    /**
     * Get the stream content.
     */
    InputStream getStream() throws IOException;

    /**
     * Get an URL to that stream. May return null if no URL is available.
     */
    URL asURL();
}
