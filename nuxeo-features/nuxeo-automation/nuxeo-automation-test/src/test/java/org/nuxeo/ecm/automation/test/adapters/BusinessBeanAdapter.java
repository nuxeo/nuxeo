/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test.adapters;

import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * @since 5.7.2
 */
public class BusinessBeanAdapter extends BusinessAdapter {

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

    public void setTitle(String value) throws PropertyException,
            ClientException {
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

}
