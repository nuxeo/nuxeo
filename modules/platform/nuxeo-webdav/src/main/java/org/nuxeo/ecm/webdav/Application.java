/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav;

import org.nuxeo.ecm.webdav.resource.RootResource;
import org.nuxeo.ecm.webengine.model.io.BlobWriter;
import org.nuxeo.ecm.webengine.model.io.DocumentBlobHolderWriter;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers the application (root resource classes and providers) in a standard / container-neutral way.
 */
public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(RootResource.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.add(new ExceptionHandler());
        singletons.add(new WebDavContextResolver());
        singletons.add(new DocumentBlobHolderWriter());
        singletons.add(new BlobWriter());
        return singletons;
    }

}
