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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.operations.business.adapter
        .BusinessAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Document Model Adapter example server side
 */
public class BusinessBeanAdapter extends BusinessAdapter {

    private static final Log log = LogFactory.getLog(BusinessBeanAdapter.class);

    /**
     * Default constructor is needed for jackson mapping
     */
    public BusinessBeanAdapter() {
        super();
    }

    public BusinessBeanAdapter(DocumentModel documentModel) {
        super(documentModel);
    }

    public String getTitle() throws ClientException {
        return (String) getDocument().getPropertyValue("dc:title");
    }

    public void setTitle(String value) throws ClientException {
        getDocument().setPropertyValue("dc:title", value);
    }

    public String getDescription() throws ClientException {
        return (String) getDocument().getPropertyValue("dc:description");
    }

    public void setDescription(String value) throws ClientException {
        getDocument().setPropertyValue("dc:description", value);
    }

    public String getNote() throws ClientException {
        return (String) getDocument().getPropertyValue("note:note");
    }

    public void setNote(String value) throws ClientException {
        getDocument().setPropertyValue("note:note", value);
    }

    public Object getObject() {
        return new String("object");
    }

    public void setObject(Object object) {

    }

}
