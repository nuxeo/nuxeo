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
package org.nuxeo.ecm.automation.test.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class BusinessBeanAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if ("Note".equals(doc.getType())) {
            return new BusinessBeanAdapter(doc);
        } else {
            return null;
        }
    }
}
