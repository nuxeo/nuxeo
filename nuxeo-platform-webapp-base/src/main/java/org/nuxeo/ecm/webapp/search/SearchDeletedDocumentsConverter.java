/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.search;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.nuxeo.ecm.core.api.LifeCycleConstants;

/**
 * JSF converter that translates between a boolean and a list of document
 * states to filter on
 *
 * @author Anahide Tchertchian
 * @deprecated this converter is not needed anymore, see NXP-6249
 */
@Deprecated
public class SearchDeletedDocumentsConverter implements Converter {

    // TODO: make states configurable through converter arguments

    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        if (context == null || component == null) {
            throw new NullPointerException();
        }

        // If the specified value is null or zero-length, return null
        if (value == null) {
            return (null);
        }
        value = value.trim();
        if (value.length() < 1) {
            return (null);
        }

        // Let them know that the value being converted is not specifically
        // "true" or "false".
        try {
            Boolean result = (Boolean.valueOf(value));
            if (Boolean.TRUE.equals(result)) {
                return new String[] { "project", "approved", "obsolete",
                        LifeCycleConstants.DELETED_STATE };
            } else {
                return new String[] { "project", "approved", "obsolete" };
            }
        } catch (Exception e) {
            throw new ConverterException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        if (context == null || component == null) {
            throw new NullPointerException();
        }

        // If the specified value is null, return a zero-length String
        if (value == null) {
            return "";
        }

        try {
            List<String> states = (List<String>) value;
            if (states != null) {
                for (String state : states) {
                    if (LifeCycleConstants.DELETED_STATE.equals(state)) {
                        return Boolean.TRUE.toString();
                    }
                }
            }
            return Boolean.FALSE.toString();

        } catch (Exception e) {
            throw new ConverterException(e);
        }

    }

}
