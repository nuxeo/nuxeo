/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * Temporary implementation for the application manager.
 * For now this implementation does nothing 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleApplicationManager implements ApplicationManager {

    protected WebEngine engine;
    
    public SimpleApplicationManager(WebEngine engine) {
        this.engine = engine;
    }
    
    public void addApplication(Bundle bundle, Application app) {
        
    }
    
    public void removeApplication(Bundle bundle) {
        
    }

    public void reload() {
        
    }
    
    public boolean deployApplication(Bundle bundle) throws Exception {
        return false;
    }
    
}
