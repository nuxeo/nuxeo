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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.ui.select2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.commons.lang3.StringUtils;
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

    protected static final String SEP_ATTRIBUTE_NAME = "separator";

    public static String getSeparator() {
        return SEP;
    }

    public static String getSeparator(UIComponent component) {
        String widget_separator = (String) component.getAttributes().get(SEP_ATTRIBUTE_NAME);
        return StringUtils.isNotBlank(widget_separator) ? widget_separator : getSeparator();
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        } else {
            String[] values = value.split(Pattern.quote(getSeparator(component)));
            // Be careful here, if we just return Arrays.asList(values), the
            // resulting list will be unmodifiable and this might cause an error
            // if something try to add elements. Let's make sure it'll be
            // modifiable
            return new ArrayList<>(Arrays.asList(values));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return null;
        } else {
            String stringValue = "";
            final String separator = getSeparator(component);
            if (value instanceof List) {
                for (Object v : (List) value) {
                    stringValue += v.toString() + separator;
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    stringValue += v.toString() + separator;
                }
            }
            if (stringValue.endsWith(separator)) {
                stringValue = stringValue.substring(0, stringValue.length() - separator.length());
            }
            return stringValue;
        }
    }

}
