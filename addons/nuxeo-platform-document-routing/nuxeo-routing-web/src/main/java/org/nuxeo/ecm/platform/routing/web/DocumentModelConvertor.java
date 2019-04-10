/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.routing.web;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * JSF Converter used for rendering, transforming a docId into the document title
 *
 * @author Mariana Cedica
 */
public class DocumentModelConvertor implements Converter {

    CoreSession session;

    public DocumentModelConvertor(CoreSession session) {
        this.session = session;
    }

    /**
     * Returns given value (does not do any reverse conversion)
     */
    @Override
    public Object getAsObject(FacesContext arg0, UIComponent component, String value) {
        return value;
    }

    /**
     * Returns the document title using the docId passed as value
     */
    @Override
    public String getAsString(FacesContext arg0, UIComponent component, Object value) {
        if (value instanceof String && !StringUtils.isEmpty((String) value)) {
            DocumentModel doc = session.getDocument((new IdRef((String) value)));
            return doc.getTitle();
        }
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }
}
