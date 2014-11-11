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
package org.nuxeo.runtime.transaction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceProvider;

/**
 * Allocate transacted invocation handlers and return proxies if
 * service is annotated with Transacted annotations. The provider is to be
 * lazy installed by the client module using the install method.
 *
 * @author matic
 *
 */
public class TransactedServiceProvider implements ServiceProvider {

    protected static final Log log = LogFactory.getLog(TransactedServiceProvider.class);

    private final static TransactedServiceProvider INSTANCE = new TransactedServiceProvider();

    protected  ServiceProvider nextProvider;

    public static void install() {
        if (INSTANCE.nextProvider != null) {
            INSTANCE.nextProvider = DefaultServiceProvider.getProvider();
            DefaultServiceProvider.setProvider(INSTANCE);
        }
    }

   protected class Entry<T> {

       final Class<T> srvClass;
       final boolean isTransacted;

       protected Entry(Class<T> srvClass) {
           this.srvClass = srvClass;
           this.isTransacted = srvClass.isInterface() && hasAnnotations(srvClass);
           if (isTransacted) {
               log.info("transacted  " + srvClass.getSimpleName());
           }
       }


       protected boolean hasAnnotations(Class<?> srvClass) {
           if (srvClass.isAnnotationPresent(Transacted.class)) {
               return true;
           }
           for (Method m: srvClass.getMethods()) {
               if (m.getAnnotation(Transacted.class) != null) {
                   return true;
               }
           }
           return false;
       }

       protected T getService() {
           // do not cache srv objects because we don't know if service is adapted or not (CoreSession for instance)
            T srvObject = nextProvider != null ?
                    nextProvider.getService(srvClass) :
                    Framework.getRuntime().getService(srvClass);
            if (!isTransacted) {
                return srvObject;
            }
            return  TransactedInstanceHandler.newProxy(srvObject, srvClass);
       }
   }


   protected Map<Class<?>, Entry<?>> registry = new HashMap<Class<?>, Entry<?>>();

    @Override
    public <T> T getService(Class<T> srvClass) {
        if (!registry.containsKey(srvClass)) {
            registry.put(srvClass,  new Entry<T>(srvClass));
        }
        return srvClass.cast(registry.get(srvClass).getService());
    }


}
