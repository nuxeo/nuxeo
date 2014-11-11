/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.compat.tomahawk;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Converter issuing a conversion error with a disabled message.
 *
 * @author Anahide Tchertchian
 */
public class DisabledTomahawkConverter implements Converter {

    private static final Log log = LogFactory.getLog(DisabledTomahawkConverter.class);

    public static final String DISABLED_CONVERTER_MESSAGE = "This tomahawk converter is disabled. Please use another jsf library";

    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        log.error(DISABLED_CONVERTER_MESSAGE);
        return value;
    }

    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        log.error(DISABLED_CONVERTER_MESSAGE);
        if (value == null || value instanceof String) {
            return (String) value;
        }
        return null;
    }

}
