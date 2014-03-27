/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.invite;

import java.io.File;
import java.net.URL;

import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;

public class RenderingHelper {

    protected static RenderingEngine engine;

    protected class CLResourceLocator implements ResourceLocator {
        public File getResourceFile(String key) {
            return null;
        }

        public URL getResourceURL(String key) {
            return this.getClass().getClassLoader().getResource(key);
        }
    }

    public RenderingEngine getRenderingEngine() {
        if (engine == null) {
            engine = new FreemarkerEngine();
            engine.setResourceLocator(new CLResourceLocator());
        }
        return engine;
    }

}
