/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.features;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.platform.usermanager.UserConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Constants used to generate JSON for suggestion in operations.
 */
public class SuggestConstants {

    private static final Log log = LogFactory.getLog(SuggestConstants.class);

    public static final String LANG_TOKEN = "{lang}";

    private static final String FORCE_DISPLAY_EMAIL_IN_SUGGESTION = "nuxeo.ui.displayEmailInUserSuggestion";

    public static final String ID = "id";

    public static final String LABEL = "displayLabel";

    public static final String ICON = "icon";

    public static final String DEFAULT_LANG = "en";

    public static final String USER_TYPE = "USER_TYPE";

    public static final String GROUP_TYPE = "GROUP_TYPE";

    public static final String TYPE_KEY_NAME = "type";

    public static final String PREFIXED_ID_KEY_NAME = "prefixed_id";

    protected static Boolean forceDisplayEmailInSuggestion = null;

    public static final String DISPLAY_ICON = "displayIcon";

    public static final String PARENT_FIELD_ID = "parent";

    public static final String OBSOLETE_FIELD_ID = "obsolete";

    public static final String ABSOLUTE_LABEL = "absoluteLabel";

    public static final String COMPUTED_ID = "computedId";

    public static final String WARN_MESSAGE_LABEL = "warn_message";

    public static final String DIRECTORY_ORDER_FIELD_NAME = "ordering";

    public static final String DIRECTORY_DEFAULT_LABEL_COL_NAME = "label";

    public static final String DEFAULT_KEY_SEPARATOR = "/";

    public static void computeGroupLabel(final Map<String, Object> obj, final String groupId, final String groupLabelField,
            final boolean hideFirstLabel) {
        String label = null;
        if (hideFirstLabel) {
            label = groupId;
        } else {
            String groupLabelValue = (String) obj.get(groupLabelField);
            if (StringUtils.isNotBlank(groupLabelValue)) {
                label = groupLabelValue;
            } else {
                label = groupId;
            }
        }
        obj.put(LABEL, label);
    }

    public static void computeUserGroupIcon(final Map<String, Object> obj, final boolean hideIcon) {
        if (obj != null) {
            if (!hideIcon) {
                String userGroupType = (String) obj.get(TYPE_KEY_NAME);
                obj.put(DISPLAY_ICON, StringUtils.isNotBlank(userGroupType)
                        && (userGroupType.equals(USER_TYPE) || userGroupType.equals(GROUP_TYPE)));
            }
        }
    }

    public static void computeUserLabel(final Map<String, Object> obj, final String firstLabelField,
            final String secondLabelField, final String thirdLabelField, final boolean hideFirstLabel,
            final boolean hideSecondLabel, final boolean hideThirdLabel, boolean displayEmailInSuggestion,
            final String userId) {
        String result = "";
        if (obj != null) {

            if (StringUtils.isNotBlank(firstLabelField) && !hideFirstLabel) {
                // If firtLabelField given and first label not hidden
                final String firstLabel = (String) obj.get(firstLabelField);
                result += StringUtils.isNotBlank(firstLabel) ? firstLabel : "";
            } else if (!hideFirstLabel) {
                // Else we use firstname
                final String firstname = (String) obj.get(UserConfig.FIRSTNAME_COLUMN);
                result += StringUtils.isNotBlank(firstname) ? firstname : "";
            }

            if (StringUtils.isNotBlank(secondLabelField) && !hideSecondLabel) {
                // If secondLabelField given and second label not hidden
                final String secondLabel = (String) obj.get(firstLabelField);
                if (StringUtils.isNotBlank(secondLabel)) {
                    if (StringUtils.isNotBlank(result)) {
                        result += " ";
                    }
                    result += secondLabel;
                }
            } else if (!hideSecondLabel) {
                // Else we use lastname
                final String lastname = (String) obj.get(UserConfig.LASTNAME_COLUMN);
                if (StringUtils.isNotBlank(lastname)) {
                    if (StringUtils.isNotBlank(result)) {
                        result += " ";
                    }
                    result += lastname;
                }
            }
            if (StringUtils.isBlank(result)) {
                // At this point, if returned label is empty, we use user id
                result += StringUtils.isNotBlank(userId) ? userId : "";
            }

            if (isForceDisplayEmailInSuggestion() || (displayEmailInSuggestion && !hideThirdLabel)) {
                if (StringUtils.isNotBlank(thirdLabelField)) {
                    final String thirdLabel = (String) obj.get(thirdLabelField);
                    if (StringUtils.isNotBlank(thirdLabel)) {
                        if (StringUtils.isNotBlank(result)) {
                            result += " ";
                        }
                        result += thirdLabel;
                    }
                } else {
                    // Else we use email
                    String email = (String) obj.get(UserConfig.EMAIL_COLUMN);
                    if (StringUtils.isNotBlank(email)) {
                        if (StringUtils.isNotBlank(result)) {
                            result += " ";
                        }
                        result += email;
                    }
                }
            }

            obj.put(LABEL, result);
        }
    }

    /**
     * Compute the field name of the directory that holds the value that we want to display.
     *
     * @param schema the directory schema
     * @param dbl10n are translations carried by directory fields
     * @param labelFieldName the name or pattern of the fields that held values
     * @param lang the current language
     * @throws IllegalArgumentException when cannot compute label field name
     * @return the final field name where we pick up the value
     * @since 5.7.3
     */
    public static String getLabelFieldName(final Schema schema, boolean dbl10n, String labelFieldName,
            final String lang) {
        if (labelFieldName == null || labelFieldName.isEmpty()) {
            // No labelFieldName provided, we assume it is 'label'
            labelFieldName = DIRECTORY_DEFAULT_LABEL_COL_NAME;
        }
        if (dbl10n) {
            int i = labelFieldName.indexOf(LANG_TOKEN);
            if (i >= 0) {
                // a pattern is provided, let's compute the field name
                // according
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
                    buf.append(DEFAULT_LANG);
                    buf.append(labelFieldName.substring(i + LANG_TOKEN.length()));
                    return buf.toString();
                }
            } else {
                // No pattern
                String result = labelFieldName + "_" + lang;
                if (schema.getField(result) != null) {
                    // we assume that fields are named like 'xxx_en',
                    // 'xxx_fr', etc.
                    return result;
                }

                log.warn(String.format(
                        "Unable to find field %s in directory schema %s. Trying to fallback on default one.",
                        labelFieldName, schema.getName()));

                result = DIRECTORY_DEFAULT_LABEL_COL_NAME + "_" + DEFAULT_LANG;
                if (schema.getField(result) != null) {
                    // no available locale, fallback to english by default
                    return result;
                }
                result = DIRECTORY_DEFAULT_LABEL_COL_NAME;
                if (schema.getField(result) != null) {
                    // no available default locale, fallback to label
                    return result;
                }

                if (schema.getField(labelFieldName) != null) {
                    // let's pretend this is not dbl10n
                    return labelFieldName;
                }

                throw new IllegalArgumentException(String.format("Unable to find field %s in directory schema %s",
                        labelFieldName, schema.getName()));
            }
        } else {
            if (schema.getField(labelFieldName) != null) {
                return labelFieldName;
            } else {
                throw new IllegalArgumentException(String.format("Unable to find field %s in directory schema %s",
                        labelFieldName, schema.getName()));
            }
        }
    }

    protected static boolean isForceDisplayEmailInSuggestion() {
        if (forceDisplayEmailInSuggestion == null) {
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            forceDisplayEmailInSuggestion = cs.isBooleanPropertyTrue(FORCE_DISPLAY_EMAIL_IN_SUGGESTION);
        }
        return forceDisplayEmailInSuggestion;
    }

    // no instantiation
    protected SuggestConstants() {
    }

}
