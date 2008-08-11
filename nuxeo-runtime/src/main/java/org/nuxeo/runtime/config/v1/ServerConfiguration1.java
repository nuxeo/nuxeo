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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.config.v1;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceDescriptor;
import org.nuxeo.runtime.api.ServiceHost;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.api.login.SecurityDomain;
import org.nuxeo.runtime.config.AbstractServerConfiguration;
import org.nuxeo.runtime.services.streaming.StreamingService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServerConfiguration1 extends AbstractServerConfiguration {

    private static final long serialVersionUID = 1970555604877434479L;

    // for compatibility
    private ServiceDescriptor[] serviceDescriptors;
    private ServiceHost[] hosts;

    public ServerConfiguration1(InvokerLocator locator, String name, Version version) {
        super(locator, name, version);
    }

    // compatibility code - retrieve service bindings and groups

    public ServiceDescriptor[] getServiceBindingsCompat() {
        return serviceDescriptors;
    }

    public void setServiceBindingsCompat(ServiceDescriptor[] bindings) {
        serviceDescriptors = bindings;
    }

    public ServiceHost[] getServiceHostsCompat() {
        return hosts;
    }

    public void setServiceHostsCompat(ServiceHost[] hosts) {
        this.hosts = hosts;
    }

    @Override
    public void install() throws Exception {
        ServiceManager serviceMgr = Framework.getLocalService(ServiceManager.class);
        LoginService loginMgr = Framework.getLocalService(LoginService.class);

        if (streamingLocator != null) {
            loadStreamingConfig(streamingLocator);
        }

        // TODO: service/groups management is buggy - when ServiceHost objects are deserialized
        // the Groups are registered in the current manager!!
        // service management must be rewritten
        for (ServiceHost sh : hosts) {
            serviceMgr.registerServer(sh);
        }

        for (ServiceDescriptor sd : serviceDescriptors) {
            serviceMgr.registerService(sd);
        }

        // get login info
        for (SecurityDomain sd : securityDomains) {
            loginMgr.addSecurityDomain(sd);
        }
    }

    protected void loadStreamingConfig(String serverLocator) throws Exception {
        StreamingService streamingService = (StreamingService) Framework.getRuntime().getComponent(
                StreamingService.NAME);
        if (!streamingService.isServer()) { // not a server
            String oldLocator = streamingService.getServerLocator();
            if (!serverLocator.equals(oldLocator)) {
                streamingService.stopManager();
                streamingService.setServerLocator(serverLocator);
                streamingService.setServer(false);
                streamingService.startManager();
            }
        }
    }

}
