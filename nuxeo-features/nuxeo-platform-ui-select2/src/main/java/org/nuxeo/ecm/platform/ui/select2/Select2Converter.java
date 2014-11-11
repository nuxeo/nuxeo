/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.ui.select2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * Select2 converter.
 *
 * @since 5.7.3
 */
@Name("select2Converter")
@BypassInterceptors
@org.jboss.seam.annotations.faces.Converter
public class Select2Converter implements Serializable, Converter {

    private static final long serialVersionUID = 1L;

    protected static final String SEP = ",";

    public static String getSeparator() {
        return SEP;
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        if (value == null || value.isEmpty()) {
            return null;
        } else {
            String[] values = value.split(getSeparator());
            // Be careful here, if we just return Arrays.asList(values), the
            // resulting list will be unmodifiable and this might cause an error
            // if something try to add elements. Let's make sure it'll be modifiable
            return new ArrayList<String>(Arrays.asList(values));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        if (value == null) {
            return null;
        } else {
            String stringValue = "";
            if (value instanceof List) {
                for (Object v : (List) value) {
                    stringValue += v.toString() + getSeparator();
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    stringValue += v.toString() + getSeparator();
                }
            }
            if (stringValue.endsWith(getSeparator())) {
                stringValue = stringValue.substring(0, stringValue.length()
                        - getSeparator().length());
            }
            return stringValue;
        }
    }

}
