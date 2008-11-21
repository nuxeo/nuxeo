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

package org.nuxeo.webengine.gwt.client;

/**
 * Marker interface to define application bundles (or layouts)
 * This is used the deferred binding to generate bundled application   
 * 
 * To create new application bundles we should extends this interface and add 
 * describe which extension and extension points must be included in the bundle.  
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ApplicationBundle {

    /**
     * Start this bundle. This will deploy all bundled extension points and extensions
     * and then it is starting the application. 
     */
    public void start();
    
    /**
     * Deploy all bundled extension and extension points without starting the application 
     */
    public void deploy();
    
}
