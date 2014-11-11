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

package org.nuxeo.ecm.core.jca;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JCAResourceAdapter implements ResourceAdapter {

    public void start(BootstrapContext arg0)
            throws ResourceAdapterInternalException {
        //System.out.println(">>>>>>>>>>>> starting resource adapter");
    }

    public void stop() {
        //System.out.println(">>>>>>>>>>>> stopping resource adapter");
    }

    public void endpointActivation(MessageEndpointFactory arg0,
            ActivationSpec arg1) throws ResourceException {
        throw new UnsupportedOperationException("NOT SUPPORTED FOR 1.0 ADAPTERS");
    }

    public void endpointDeactivation(MessageEndpointFactory arg0,
            ActivationSpec arg1) {
        throw new UnsupportedOperationException("NOT SUPPORTED FOR 1.0 ADAPTERS");
    }

    public XAResource[] getXAResources(ActivationSpec[] arg0)
            throws ResourceException {
        return new XAResource[0];
    }

}
