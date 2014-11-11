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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.versioning;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * Converter for document versioning, setting the accurate
 * {@link VersioningOption} on the document model context data instead of
 * String selected in the interface.
 *
 * @since 5.7.3
 */
@Name("documentVersioningConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class DocumentVersioningConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        VersioningActions option = VersioningActions.valueOf(value);
        VersioningOption vo;
        if (option == VersioningActions.ACTION_INCREMENT_MAJOR) {
            vo = VersioningOption.MAJOR;
        } else if (option == VersioningActions.ACTION_INCREMENT_MINOR) {
            vo = VersioningOption.MINOR;
        } else if (option == VersioningActions.ACTION_NO_INCREMENT) {
            vo = VersioningOption.NONE;
        } else {
            vo = null;
        }
        return vo;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value == null || (!(value instanceof VersioningOption))) {
            return null;
        }
        VersioningOption option = (VersioningOption) value;
        VersioningActions action = VersioningActions.getByVersioningOption(option);
        if (action != null) {
            return action.name();
        }
        return null;
    }

}
