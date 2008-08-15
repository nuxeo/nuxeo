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

package org.nuxeo.ecm.webengine.login;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.HashMap;

import javax.security.auth.Subject;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.webengine.session.UserSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JBossAuthenticationPropagator implements AuthenticationPropagator {

    protected static HashMap<String, String> NULL_PARAMS = new HashMap<String, String>();

    private Method method = null;

    public void propagate(UserSession userSession) {
        // the following call is made through reflection API:
        // SecurityAssociation.pushSubjectContext(subject, principal, credentials);
        //TODO: we may use in future the JBoss AltClientLoginModule to perform the
        // propagation - this way we avoid using explicitly reflection or jboss code
        try {
            if (method == null) {
                Class<?> klass = Class.forName("org.jboss.security.SecurityAssociation");
                method = klass.getDeclaredMethod("pushSubjectContext",
                        new Class<?>[] {Subject.class, Principal.class, Object.class});
            }
            Subject subject = userSession.getSubject();
            Principal principal = userSession.getPrincipal();
            // for anonymous user
            Object credentials = userSession.isAnonymous() ?
                    createAnonymousCredentials(userSession)
                    : userSession.getCredentials();
            doPropagate(method, subject, principal, credentials);
        } catch (Exception e) {
            e.printStackTrace();
            return; // do nothing
        }
    }

    protected Object createAnonymousCredentials(UserSession userSession) {
        String name = userSession.getPrincipal().getName();
        UserIdentificationInfo uid = new UserIdentificationInfo(name, name);
        uid.setAuthPluginName("ANONYMOUS_AUTH");
        uid.setLoginPluginName("Trusting_LM");
        uid.setLoginParameters(NULL_PARAMS);
        return uid;
    }

    private void doPropagate(final Method method, final Subject subject,
            final Principal principal, final Object credentials) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    method.invoke(null, subject, principal, credentials);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

}
