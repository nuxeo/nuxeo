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
