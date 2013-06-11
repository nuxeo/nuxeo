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

import org.nuxeo.ecm.automation.client.AdapterFactory;
import org.nuxeo.ecm.automation.client.Session;

/**
 * @since 5.7
 */
public class BusinessServiceFactory implements AdapterFactory<BusinessService> {

    public Class<?> getAcceptType() {
        return Session.class;
    }

    public Class<BusinessService> getAdapterType() {
        return BusinessService.class;
    }

    public BusinessService getAdapter(Object toAdapt) {
        return new BusinessService((Session) toAdapt);
    }

}
