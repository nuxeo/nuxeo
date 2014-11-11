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

package org.nuxeo.runtime.model;

/**
 * Defines an extensible object.
 * <p>
 * Extensible objects are accepting extensions through extension points.
 * They provide methods for registering and unregistering extensions.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Extensible {

    /**
     * Registers the given extension.
     *
     * @param extension the extension to register
     * @throws Exception if any error occurs
     */
    void registerExtension(Extension extension) throws Exception;

    /**
     * Unregisters the given extension.
     *
     * @param extension the extension to unregister
     * @throws Exception if any error occurs
     */
    void unregisterExtension(Extension extension) throws Exception;

}
