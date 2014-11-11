/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tmartins
 *
 */
package org.nuxeo.ecm.directory.ldap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.naming.ServiceUnavailableException;
import javax.naming.directory.DirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper to encapsulate the calls to LDAP and retry the requests in case of
 * ServiceUnavailableException errors
 *
 * @since 5.7
 * @author Thierry Martins <tm@nuxeo.com>
 *
 */
public class LdapRetryHandler implements InvocationHandler {

    private static final Log log = LogFactory.getLog(LdapRetryHandler.class);

    protected DirContext dirContext;

    protected int attemptsNumber;

    protected LdapRetryHandler(DirContext object, int attempts) {
        dirContext = object;
        attemptsNumber = attempts;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        int attempts = attemptsNumber;
        Throwable e = null;
        while (attempts-- > 0) {
            try {
                return method.invoke(dirContext, args);
            } catch (InvocationTargetException sue) {
                e = sue.getTargetException();
                if (!(e instanceof ServiceUnavailableException)) {
                    throw sue.getTargetException();
                } else {
                    log.debug("Retrying ...", e);
                }
            }
        }
        throw e;
    }

    public static DirContext wrap(DirContext dirContext, int retries) {
        LdapRetryHandler handler = new LdapRetryHandler(dirContext, retries);
        return (DirContext) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { DirContext.class }, handler);
    }
}
