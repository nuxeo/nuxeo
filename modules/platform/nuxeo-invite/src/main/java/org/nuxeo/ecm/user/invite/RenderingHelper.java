/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.invite;

import java.io.File;
import java.net.URL;

import freemarker.template.Configuration;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;


public class RenderingHelper {

    protected volatile FreemarkerEngine engine;

    protected class CLResourceLocator implements ResourceLocator {
        @Override
        public File getResourceFile(String key) {
            return null;
        }

        @Override
        public URL getResourceURL(String key) {
            return this.getClass().getClassLoader().getResource(key);
        }
    }

    public FreemarkerEngine getRenderingEngine() {
        if (engine == null) {
            synchronized (this) {
                if (engine == null) {
                    FreemarkerEngine fme = new FreemarkerEngine();
                    fme.setResourceLocator(new CLResourceLocator());
                    engine = fme;
                }
            }
        }
        return engine;
    }

    public Configuration getEngineConfiguration() {
        return getRenderingEngine().getConfiguration();
    }

}
