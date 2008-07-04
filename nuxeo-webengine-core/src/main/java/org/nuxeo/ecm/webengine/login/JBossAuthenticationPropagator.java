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

import javax.security.auth.Subject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JBossAuthenticationPropagator implements AuthenticationPropagator {

    private Method method = null;

    public void propagate(Subject subject, Principal principal, Object credentials) {
        // the following call is made through reflection API:
        // SecurityAssociation.pushSubjectContext(subject, principal, credentials);
        try {
            if (method == null) {
                Class<?> klass = Class.forName("org.jboss.security.SecurityAssociation");
                method = klass.getDeclaredMethod("pushSubjectContext",
                        new Class<?>[] {Subject.class, Principal.class, Object.class});
            }
            //method.invoke(null, new Object[] {subject, principal, credentials});
            doPropagate(method, subject, principal, credentials);
        } catch (Exception e) {
            e.printStackTrace();
            return; // do nothing
        }
    }

    private final void doPropagate(final Method method, final Subject subject, final Principal principal, final Object credentials) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    method.invoke(null, new Object[] {subject, principal, credentials});
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

}
