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
 *     matic
 */
package org.nuxeo.ecm.platform.management.adapters;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.DefaultSessionManager;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.model.ComponentName;

/**
 * @author matic
 * 
 */
public class HttpSessionMetricFactory extends AbstractResourceFactory {
 
    protected static HttpSessionMetricAdapter mbeanAdapter = new HttpSessionMetricAdapter();

    protected static void addSessionListener(HttpServletRequest request) {
        mbeanAdapter.addSessionListener(request);
    }

    public static class SessionManager extends DefaultSessionManager {
        @Override
        public void onAuthenticatedSessionCreated(ServletRequest request,
                HttpSession session,
                CachableUserIdentificationInfo cachebleUserInfo) {
            addSessionListener((HttpServletRequest) request);
        }
    }

    public void registerResources() {
        String qualifiedName = 
            ObjectNameFactory.formatMetricQualifiedName(new ComponentName(PluggableAuthenticationService.NAME), "session");
        service.registerResource("http-session-metric", qualifiedName,
                HttpSessionMetricMBean.class, mbeanAdapter);
    }
}
