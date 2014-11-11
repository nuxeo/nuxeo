/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.naming.ContextAccessController;

/**
 * 
 * Grab security token and source context for setting write access onto naming
 * context during container startup.
 * 
 * @author matic
 * 
 * @since 5.5
 */
public class ContextSecurityGrabber implements LifecycleListener {

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        final String type = event.getType();
        if (!Lifecycle.AFTER_START_EVENT.equals(type)) {
            return;
        }
        final Object token = event.getLifecycle();
        final StandardContext source = (StandardContext) event.getSource();
        String name = "/" + source.getDomain() + "/" + source.getHostname()
                + source.getPath();
        ContextAccessController.setWritable(name, token);
    }

}
