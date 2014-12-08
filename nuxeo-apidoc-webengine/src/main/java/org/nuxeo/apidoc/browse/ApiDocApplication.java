/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.apidoc.doc.DocumentationItemReader;
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
        classes = classes == null ? new HashSet<Class<?>>() : new HashSet<Class<?>>(classes);
        classes.add(DocumentationItemReader.class);
        classes.add(ArchiveFileWriter.class);
        return classes;
    }

}
