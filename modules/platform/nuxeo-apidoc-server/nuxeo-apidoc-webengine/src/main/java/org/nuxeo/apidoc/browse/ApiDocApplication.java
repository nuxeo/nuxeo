/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.apidoc.export.ArchiveFileWriter;
import org.nuxeo.ecm.webengine.app.WebEngineModule;

// before 5.4.1 you extended javax.ws.rs.core.Application
// if the MANIFEST.MF contained
// Nuxeo-WebModule: org.nuxeo.apidoc.browse.ApiDocApplication; explode:=true; compat:=true
// and the module.xml had a proper path="/distribution"

public class ApiDocApplication extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = super.getClasses();
        classes = classes == null ? new HashSet<>() : new HashSet<>(classes);
        classes.add(ArchiveFileWriter.class);
        return classes;
    }

}
