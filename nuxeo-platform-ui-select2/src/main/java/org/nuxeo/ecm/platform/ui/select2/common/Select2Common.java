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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.select2.common;

import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * Group fields and methods used at initialization and runtime for select2
 * feature.
 *
 * @since 5.7.3
 */
public class Select2Common {

    public static final String LANG_TOKEN = "{lang}";

    public static final String LABEL = "label";

    public static final String PARENT_FIELD_ID = "parent";

    public static final String ID = "id";

    public static final String PLACEHOLDER = "placeholder";

    public static final String COMPUTED_ID = "computedId";

    public static final String DEFAULT_KEY_SEPARATOR = "/";

    public static final String OBSOLETE_FIELD_ID = "obsolete";

    public static final String USER_SUGGESTION_SELECT2 = "select2User";

    public static final String SUGGESTION_SELECT2 = "select2Widget";

    public static final String USER_TYPE = "user";

    public static final String GROUP_TYPE = "group";

    public static final String TYPE_KEY_NAME = "type";

    public static final String PREFIXED_ID_KEY_NAME = "prefixed_id";

    public static final String SUGGESTION_FORMATTER = "suggestionFormatter";

    public static final String SELECTION_FORMATTER = "selectionFormatter";

    public static final String USER_DEFAULT_SUGGESTION_FORMATTER = "userEntryDefaultFormatter";

    public static final String DOC_DEFAULT_SUGGESTION_FORMATTER = "docEntryDefaultFormatter";

    public static final String WARN_MESSAGE_LABEL = "warn_message";

    public static final String DIR_SUGGESTION_SELECT2 = "select2Directory";

    public static final String DIR_DEFAULT_SUGGESTION_FORMATTER = "dirEntryDefaultFormatter";

    public static final String READ_ONLY_PARAM = "readonly";

    public static final String RERENDER_JS_FUNCTION_NAME = "reRenderFunctionName";

    public static final String AJAX_RERENDER = "ajaxReRender";

    public static final String USER_DEFAULT_SELECTION_FORMATTER = USER_DEFAULT_SUGGESTION_FORMATTER;

    public static final String DOC_DEFAULT_SELECTION_FORMATTER = DOC_DEFAULT_SUGGESTION_FORMATTER;

    public static final String DIR_DEFAULT_SELECTION_FORMATTER = DIR_DEFAULT_SUGGESTION_FORMATTER;

    /**
     * Compute the filed name of the directory that holds the value that we want
     * to display.
     *
     * @param schema the directory schema
     * @param dbl10n are translations carried by directory fields
     * @param labelFieldName the name or pattern of the fields that held values
     * @param lang the current language
     * @return the final field name where we pick up the value
     *
     * @since 5.7.3
     */
    public static String getLabelFieldName(final Schema schema, boolean dbl10n,
            String labelFieldName, final String lang) {
        if (labelFieldName == null || labelFieldName.isEmpty()) {
            // No labelFieldName provided, we assume it is 'label'
            labelFieldName = LABEL;
        }
        if (dbl10n) {
            int i = labelFieldName.indexOf(LANG_TOKEN);
            if (i >= 0) {
                // a pattern is provided, let's compute the field name according
                // to the current lang
                StringBuffer buf = new StringBuffer();
                buf.append(labelFieldName.substring(0, i));
                buf.append(lang);
                buf.append(labelFieldName.substring(i + LANG_TOKEN.length()));
                String result = buf.toString();
                if (schema.getField(result) != null) {
                    return result;
                } else {
                    // there is no field for the current lang, let's pick
                    // english by default
                    buf = new StringBuffer();
                    buf.append(labelFieldName.substring(0, i));
                    buf.append("en");
                    buf.append(labelFieldName.substring(i + LANG_TOKEN.length()));
                    return buf.toString();
                }
            } else {
                // No pattern, we assume that fields are named like 'xxx_en',
                // 'xxx_fr', etc.
                return labelFieldName + "_" + lang;
            }
        }
        return labelFieldName;
    }

}
