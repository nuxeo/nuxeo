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
 */
package org.nuxeo.runtime.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.core.ContainerBase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoLauncher implements LifecycleListener {

    
    public void lifecycleEvent(LifecycleEvent event) {
        Lifecycle lf = event.getLifecycle();
        if (lf instanceof ContainerBase) {
            Loader loader = ((ContainerBase)lf).getLoader();
            if (loader instanceof NuxeoWebappLoader) {
                handleEvent((NuxeoWebappLoader)loader, event);
            }
        }
    }
    
    protected void handleEvent(NuxeoWebappLoader loader, LifecycleEvent event) {
        String type = event.getType();
        if (type == Lifecycle.AFTER_START_EVENT) {
            startNuxeo(loader);
        } else if (type == Lifecycle.STOP_EVENT) {
            stopNuxeo(loader);
        }
    }

    protected void startNuxeo(NuxeoWebappLoader loader) {
        loader.startFramework();
    }

    protected void stopNuxeo(NuxeoWebappLoader loader) {
        loader.stopFramework();
    }

}
