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
package org.nuxeo.ecm.automation.server.test.business.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
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

    public String getTitle() {
        try {
            return (String) getDocument().getPropertyValue("dc:title");
        } catch (ClientException e) {
            log.error("cannot get property title", e);
        }
        return null;
    }

    public void setTitle(String value) {
        try {
            getDocument().setPropertyValue("dc:title", value);
        } catch (ClientException e) {
            log.error("cannot set property title", e);
        }
    }

    public String getDescription() {
        try {
            return (String) getDocument().getPropertyValue("dc:description");
        } catch (ClientException e) {
            log.error("cannot get description property", e);
        }
        return null;
    }

    public void setDescription(String value) {
        try {
            getDocument().setPropertyValue("dc:description", value);
        } catch (ClientException e) {
            log.error("cannot set description property", e);
        }
    }

    public String getNote() {
        try {
            return (String) getDocument().getPropertyValue("note:note");
        } catch (ClientException e) {
            log.error("cannot get note property", e);
        }
        return null;
    }

    public void setNote(String value) {
        try {
            getDocument().setPropertyValue("note:note", value);
        } catch (ClientException e) {
            log.error("cannot get note property", e);
        }
    }

}
