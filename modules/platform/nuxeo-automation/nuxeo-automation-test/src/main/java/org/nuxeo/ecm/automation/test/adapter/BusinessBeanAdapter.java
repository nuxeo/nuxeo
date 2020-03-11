/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
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
        return (String) getDocument().getPropertyValue("dc:title");
    }

    public void setTitle(String value) {
        getDocument().setPropertyValue("dc:title", value);
    }

    public String getDescription() {
        return (String) getDocument().getPropertyValue("dc:description");
    }

    public void setDescription(String value) {
        getDocument().setPropertyValue("dc:description", value);
    }

    public String getNote() {
        return (String) getDocument().getPropertyValue("note:note");
    }

    public void setNote(String value) {
        getDocument().setPropertyValue("note:note", value);
    }

    public Object getObject() {
        return new String("object");
    }

    public void setObject(Object object) {

    }

}
