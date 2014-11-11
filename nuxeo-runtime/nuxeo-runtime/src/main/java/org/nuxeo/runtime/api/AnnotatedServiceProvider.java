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
 *     "Stephane Lacoin at Nuxeo (aka matic)"
 */
package org.nuxeo.runtime.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.transaction.Transacted;
import org.nuxeo.runtime.transaction.TransactedServiceProvider;


/**
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 *
 */
public abstract class AnnotatedServiceProvider implements ServiceProvider {

    protected static final Log log = LogFactory.getLog(TransactedServiceProvider.class);

    protected ServiceProvider nextProvider;

    boolean installed = false;

    protected final Map<Class<?>, Entry<?>> registry = new HashMap<Class<?>, Entry<?>>();

    protected abstract Class<? extends Annotation> annotationClass();
    protected abstract <T> T newProxy(T object, Class<T> clazz);


    public void installSelf() {
        // next provider can be null, we need a flag
        if (installed) {
            return;
        }
        installed = true;
        nextProvider = DefaultServiceProvider.getProvider();
        DefaultServiceProvider.setProvider(this);
    }

    protected class Entry<T> {
        final Class<T> srvClass;

        final boolean isAnnotated;

        protected Entry(Class<T> srvClass) {
            this.srvClass = srvClass;
            this.isAnnotated = srvClass.isInterface() && hasAnnotations(srvClass);
            if (isAnnotated) {
                log.info("handling  " + srvClass.getSimpleName() + " for " + annotationClass().getSimpleName());
            }
        }

        protected boolean hasAnnotations(Class<?> srvClass) {
            Class<? extends Annotation> annotationClass = annotationClass();
            if (srvClass.isAnnotationPresent(annotationClass)) {
                return true;
            }
            for (Method m : srvClass.getMethods()) {
                if (m.getAnnotation(Transacted.class) != null) {
                    return true;
                }
            }
            return false;
        }

        public T getService() {
            // do not cache srv objects because we don't know if service is
            // adapted or not (CoreSession for instance)
            T srvObject = nextProvider != null ? nextProvider.getService(srvClass) : Framework.getRuntime().getService(srvClass);
            if (!isAnnotated) {
                return srvObject;
            }
            return newProxy(srvObject, srvClass);
        }
    }

   @Override
    public <T> T getService(Class<T> srvClass) {
        if (!registry.containsKey(srvClass)) {
            registry.put(srvClass,  new Entry<T>(srvClass));
        }
        return srvClass.cast(registry.get(srvClass).getService());
    }

}
