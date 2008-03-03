/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.jboss.adapter.deployment;

import java.net.URL;

import org.jboss.deployment.DeploymentInfo;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JBossRuntimeContext extends DefaultRuntimeContext {

    private final DeploymentInfo di;

    public JBossRuntimeContext(DeploymentInfo di) {
        this.di = di;
    }

    public JBossRuntimeContext(RuntimeService runtime, DeploymentInfo di) {
        super(runtime);
        this.di = di;
    }

    @Override
    public URL getResource(String name) {
        URL url;
        DeploymentInfo parent = di.parent;
        // :XXX: check if it covers all the cases.
        if (parent != null && di.isXML) {
            // Here, load from the parent because we are certainly within a
            // jar file with a bundle descriptor
            url = di.parent.localCl.findResource(name);
        } else {
            url = di.localCl.findResource(name);
        }
        return url;
    }

    @Override
    public URL getLocalResource(String name) {
        return di.localCl.findResource(name);
    }

    @Override
    public Class loadClass(String className) throws ClassNotFoundException {
        return di.ucl.loadClass(className);
    }

}
