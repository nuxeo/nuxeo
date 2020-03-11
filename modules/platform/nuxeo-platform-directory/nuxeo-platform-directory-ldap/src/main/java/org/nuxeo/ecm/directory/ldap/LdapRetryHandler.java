/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Wrapper to encapsulate the calls to LDAP and retry the requests in case of ServiceUnavailableException errors
 *
 * @since 5.7
 * @author Thierry Martins <tm@nuxeo.com>
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
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        throw e; // NOSONAR
    }

    public static DirContext wrap(DirContext dirContext, int retries) {
        LdapRetryHandler handler = new LdapRetryHandler(dirContext, retries);
        return (DirContext) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { DirContext.class }, handler);
    }
}
