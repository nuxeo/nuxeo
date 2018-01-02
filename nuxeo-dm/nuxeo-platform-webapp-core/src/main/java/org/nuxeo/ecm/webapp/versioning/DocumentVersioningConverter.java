/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.versioning;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * Converter for document versioning, setting the accurate {@link VersioningOption} on the document model context data
 * instead of String selected in the interface.
 *
 * @since 5.7.3
 */
@Name("documentVersioningConverter")
@org.jboss.seam.annotations.faces.Converter
@BypassInterceptors
public class DocumentVersioningConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
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
    public String getAsString(FacesContext context, UIComponent component, Object value) {
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
