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
 */

package org.nuxeo.ecm.platform.ui.web.seam;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.providers.ServiceProvider;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Provide simple extension to Seam injection system to be able to inject Nuxeo
 * Services and Nuxe Components inside Seam Beans
 *
 * @since 5.7.3
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@Scope(ScopeType.STATELESS)
@Name(ServiceProvider.NAME)
@Install(precedence = FRAMEWORK)
@BypassInterceptors
public class NuxeoRuntimeServiceProvider implements ServiceProvider {

    private static final Log log = LogFactory.getLog(NuxeoRuntimeServiceProvider.class);

    @Override
    public Object lookup(String name, Class type, boolean create) {

        if (Framework.getRuntime() == null) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Nuxeo Lookup for " + name + " class " + type);
        }

        if (type != null && type.isAssignableFrom(CoreSession.class)) {
            // XXX return a CoreSession on the default repository ?
            return null;
        }
        // service loopkup
        Object result = Framework.getLocalService(type);
        if (result == null) {
            // fallback on component lookup
            result = Framework.getRuntime().getComponent(name);
        }

        if (log.isDebugEnabled()) {
            log.debug("Nuxeo Lookup => return " + result);
        }
        return result;

    }

}
