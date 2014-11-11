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
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.ui.web.converter;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * Formats textarea to take into account new lines.
 *
 * @author Thierry Martins
 *
 */
public class TextareaConverter implements Converter, StateHolder {

    public static final String CONVERTER_ID = "TextareaConverter";
    
    public boolean isTransient() {
        return false;
    }

    public void restoreState(FacesContext arg0, Object arg1) {
    }

    public Object saveState(FacesContext arg0) {
        return null;
    }

    public void setTransient(boolean arg0) {
    }
    
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        // Should never be used
        return value.replaceAll("<br />", "\n");
    }

    /**
     * Replaces new line by BR tag
     */
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        String v = (String) value;
        return v.replaceAll("\n", "<br />");
    }

}
