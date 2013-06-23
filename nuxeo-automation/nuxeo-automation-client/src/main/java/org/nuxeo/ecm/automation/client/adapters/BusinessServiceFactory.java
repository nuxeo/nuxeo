/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.client.adapters;

import java.lang.reflect.ParameterizedType;

import org.nuxeo.ecm.automation.client.AdapterFactory;
import org.nuxeo.ecm.automation.client.Session;

/**
 * @since 5.7
 */
public class BusinessServiceFactory<T> implements
        AdapterFactory<BusinessService<T>> {

    Class<BusinessService<T>> serviceType;

    public Class<?> getAcceptType() {
        return Session.class;
    }

    @Override
    public Class<BusinessService<T>> getAdapterType() {
        return serviceType();
    }

    @Override
    public BusinessService<T> getAdapter(Object toAdapt) {
        return new BusinessService<T>((Session) toAdapt);
    }

    protected Class<BusinessService<T>> serviceType() {
        try {
            if (serviceType == null) {
                ParameterizedType adapterType = (ParameterizedType) this.getClass().getGenericInterfaces()[0];
                ParameterizedType serviceType = (ParameterizedType) adapterType.getActualTypeArguments()[0];
                this.serviceType = (Class<BusinessService<T>>) serviceType.getRawType();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return serviceType;
    }

}
