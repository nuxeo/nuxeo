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

package org.nuxeo.ecm.platform.rendering.api;

import java.io.File;
import java.net.URL;

/**
 * Resource locators are used to resolve resource names to resource URLs
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ResourceLocator {

    /**
     * Gets an URL resource given a key
     *
     * @param key the resource key
     * @return the resource URL or null if no such result was found
     */
    URL getResourceURL(String key);

    /**
     * Get a file resource given a key
     *
     * @param key
     * @return
     */
    File getResourceFile(String key);

}
