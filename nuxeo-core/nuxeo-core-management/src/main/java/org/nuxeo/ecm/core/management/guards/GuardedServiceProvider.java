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
package org.nuxeo.ecm.core.management.guards;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.AnnotatedServiceProvider;
/**
 * Return a proxy if the service is annotated by Administrated
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 *
 */
public class GuardedServiceProvider extends AnnotatedServiceProvider {

   public static final GuardedServiceProvider INSTANCE = new GuardedServiceProvider();

   public static void install() {
       INSTANCE.installSelf();
   }

    @Override
    protected Class<Guarded> annotationClass() {
        return Guarded.class;
    }

    @Override
    protected <T> T newProxy(T object, Class<T> clazz) {
         return GuardedServiceHandler.newProxy(object, clazz);
    }


    protected final Map<String, Boolean> activeStatuses = new HashMap<String, Boolean>();

    public void checkIsActive(Method method, String id) {
        if (!activeStatuses.containsKey(id)) {
            return;
        }
        Boolean isActive = activeStatuses.get(id);
        if (Boolean.TRUE.equals(isActive)) {
            return;
        }
        throw new PassivatedServiceException(method, id);
    }

 }
