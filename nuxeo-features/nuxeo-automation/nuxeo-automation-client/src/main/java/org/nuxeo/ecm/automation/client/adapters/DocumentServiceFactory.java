/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.adapters;

import org.nuxeo.ecm.automation.client.AdapterFactory;
import org.nuxeo.ecm.automation.client.Session;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentServiceFactory implements AdapterFactory<DocumentService> {

    public Class<?> getAcceptType() {
        return Session.class;
    }

    public Class<DocumentService> getAdapterType() {
        return DocumentService.class;
    }

    public DocumentService getAdapter(Object toAdapt) {
        return new DocumentService((Session) toAdapt);
    }

}
