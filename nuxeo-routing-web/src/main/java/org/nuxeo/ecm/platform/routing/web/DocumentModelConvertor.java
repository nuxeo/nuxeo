/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.routing.web;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;


/**
 * JSF Converter used for rendering, transforming a docId into the document title
 *
 * @author Mariana Cedica
 *
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
    public Object getAsObject(FacesContext arg0, UIComponent component,
            String value) {
        return value;
    }


    /**
     * Returns the document title using the docId passed as value
     */
    @Override
    public String getAsString(FacesContext arg0, UIComponent component,
            Object value) {
        if (value instanceof String && !StringUtils.isEmpty((String) value)) {
            try {
                DocumentModel doc = session.getDocument((new IdRef(
                        (String) value)));
                return doc.getTitle();
            } catch (ClientException e) {
                return null;
            }
        }
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }
}
