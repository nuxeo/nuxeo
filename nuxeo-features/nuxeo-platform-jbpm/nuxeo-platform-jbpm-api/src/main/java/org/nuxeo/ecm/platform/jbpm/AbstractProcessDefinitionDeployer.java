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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public abstract class AbstractProcessDefinitionDeployer implements
        ProcessDefinitionDeployer {

    public void deploy(final URL url) throws Exception {
        JbpmService service = Framework.getService(JbpmService.class);
        service.executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                InputStream is;
                try {
                    is = url.openStream();
                } catch (IOException e) {
                    throw new NuxeoJbpmException(
                            "Error opening process definition url.", e);
                }
                ProcessDefinition pd = ProcessDefinition.parseXmlInputStream(is);
                context.deployProcessDefinition(pd);
                try {
                    is.close();
                } catch (IOException e) {
                    throw new NuxeoJbpmException(
                            "Error closing process definition url.", e);
                }
                return null;
            }
        });
    }

    public boolean isDeployable(URL url) throws Exception {
        return false;
    }
}
