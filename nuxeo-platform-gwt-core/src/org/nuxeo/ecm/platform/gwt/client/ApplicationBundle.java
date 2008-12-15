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

package org.nuxeo.ecm.platform.gwt.client;

/**
 * Marker interface to define application bundles (or layouts)
 * This is used the deferred binding to generate bundled application   
 * 
 * To create new application bundles we should extends this interface and add 
 * describe which extension and extension points must be included in the bundle.  
 * 
 * When deploying extensions through bundles it will automatically handle ordering hints 
 * so the target extension point may ignore hints..  (only collection oriented hints are handled. 
 * (REPLACE and AS_DEFAULT are not)
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
     * Start this bundle using a prefix for server URL. This is especially needed when debuging in hosted mode 
     * to redirect remote calls to another server than the one in embedded Tomcat
     */
    public void start(String name);
    
    /**
     * Deploy all bundled extension and extension points without starting the application 
     */
    public void deploy();
    
}
