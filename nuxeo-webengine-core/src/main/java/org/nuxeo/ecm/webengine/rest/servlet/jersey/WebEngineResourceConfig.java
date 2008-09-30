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

package org.nuxeo.ecm.webengine.rest.servlet.jersey;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.webengine.rest.ResourceBinding;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.api.core.DefaultResourceConfig;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineResourceConfig extends DefaultResourceConfig {


    /**
     *
     */
    public WebEngineResourceConfig() {
        super ();
    }
    
    @Override
    public Set<Class<?>> getResourceClasses() {
        WebEngine2 engine = Framework.getLocalService(WebEngine2.class);
        HashSet<Class<?>> result = new HashSet<Class<?>>();
        for (ResourceBinding binding : engine.getBindings()) {
            try {
                result.add(engine.getScripting().loadClass(binding.className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    

}
