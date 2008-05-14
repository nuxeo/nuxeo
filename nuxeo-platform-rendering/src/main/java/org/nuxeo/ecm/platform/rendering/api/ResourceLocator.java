/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
     * @param key
     * @return
     */
    File getResourceFile(String key);

}
