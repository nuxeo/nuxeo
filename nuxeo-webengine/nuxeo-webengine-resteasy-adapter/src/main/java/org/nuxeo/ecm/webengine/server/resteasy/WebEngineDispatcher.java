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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.server.resteasy;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.interception.InterceptorRegistry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.nuxeo.ecm.webengine.ResourceRegistry;

/**
 * We need this wrapper to be able to know when resteasy is sending a 404...
 * This way we can optimize lazy module loading - because we can check for a lazy module
 * after dispatching the jax-rs request
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineDispatcher extends SynchronousDispatcher {

    protected final ResourceRegistryImpl resourceReg;

    public WebEngineDispatcher(ResteasyProviderFactory providerFactory) {
        super(providerFactory);
        resourceReg = new ResourceRegistryImpl(this);
        addInterceptors();
    }

    public ResourceRegistry getResourceRegistry() {
        return resourceReg;
    }

    public void addInterceptors() {
        InterceptorRegistry reg = getProviderFactory().getInterceptorRegistry();
        reg.registerResourceMethodInterceptor(new SecurityInterceptor());
        reg.registerResourceMethodInterceptor(new TransactionInterceptor());
    }

}
