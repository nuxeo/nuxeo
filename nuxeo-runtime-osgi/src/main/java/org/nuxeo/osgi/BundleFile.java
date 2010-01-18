/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface BundleFile {

    /**
     * Checks if this bundle is a JAR.
     *
     * @return true if the bundle is a JAR, false otherwise
     */
    boolean isJar();

    /**
     * Checks if this bundle is a directory (an exploded jar).
     *
     * @return true if the bundle is a directory, false otherwise
     */
    boolean isDirectory();

    /**
     * Gets this bundle symbolic name. If this bundle is an OSGi bundle,
     * then the Bundle-SymbolicName manifest header is returned, otherwise
     * null is returned.
     *
     * @return null if not an OSGi bundle, the OSGI bundle symbolic name otherwise
     */
    String getSymbolicName();

    /**
     * Gets the original file name of this bundle.
     *
     * @return the bundle file name
     */
    String getFileName();

    /**
     * Gets the original location of this bundle.
     * <p>
     * This is an URI string pointing to the original locatioon of the bundle.
     *
     * @return the location
     */
    String getLocation();

    /**
     * Gets the current location of the bundle as an URL (it may be different
     * from the original location).
     *
     * @return the bundle url
     */
    URL getURL();

    /**
     * Gets the current location of the bundle as a file.
     *
     * @return the bundle file or null if the bundle is not a file
     */
    File getFile();

    /**
     * Gets the bundle manifest.
     *
     * @return the bundle manifest
     */
    Manifest getManifest();

    /**
     * Gets the entry at the given path in this bundle.
     *
     * @return the entry URL if any null otherwise
     *
     * @see org.osgi.framework.Bundle#getEntry(String)
     */
    URL getEntry(String name);

    /**
     * Returns an Enumeration of all the paths (<code>String</code> objects)
     * to entries within the bundle whose longest sub-path matches the supplied
     * path argument.
     *
     * @see org.osgi.framework.Bundle#getEntryPaths(String)
     */
    Enumeration<String> getEntryPaths(String path);

    /**
     * Finds entries in that bundle.
     *
     * @see org.osgi.framework.Bundle#findEntries(String, String, boolean)
     */
    Enumeration<URL> findEntries(String name, String pattern, boolean recurse);

    /**
     * Gets a list with nested bundles or null if none. The bundle Manifest
     * headers Bundle-ClassPath and Class-Path will be used to retrieve nested
     * jars.
     *
     * @param tmpDir optional temporary dir if the nested bundle should be
     *            extracted from an archive
     */
    Collection<BundleFile> getNestedBundles(File tmpDir) throws IOException;

    /**
     * Get a list with nested bundles or null if none. The bundle file will be
     * scanned for nested JARs.
     *
     * @param tmpDir optional temporary dir if the nested bundle should be
     *            extracted from an archive
     */
    Collection<BundleFile> findNestedBundles(File tmpDir) throws IOException;

}
