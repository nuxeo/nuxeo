/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.Map;

/**
 * Single select option top be held by the {@link WidgetDefinition} and {@link Widget} generated from the definition.
 *
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public interface WidgetSelectOption extends Serializable {

    /**
     * Returns the value representing the option.
     * <p>
     * This value is optional when using static label and values, it can be useful to use it in conjunction with the
     * {@link #getVar()} method to retrieve the id and label from the object.
     */
    Serializable getValue();

    /**
     * Returns the var representing the value returned by {@link #getValue()}
     * <p>
     * This value can be used in the potential EL expressions returned by {@link #getItemLabel()},
     * {@link #getItemValue()}, {@link #getItemDisabled()} and {@link #getItemRendered()}.
     */
    String getVar();

    /**
     * Returns the item label for the select option.
     * <p>
     * This can be an EL expression if {@link #getValue()} and {@link #getVar()} return a non-null value.
     */
    String getItemLabel();

    /**
     * Getter to handle l10n localization of select options.
     *
     * @since 6.0
     */
    String getItemLabel(String locale);

    /**
     * Getter to handle l10n localization of select options.
     *
     * @since 6.0
     */
    Map<String, String> getItemLabels();

    /**
     * Returns the item value for the select option.
     * <p>
     * This can be an EL expression if {@link #getValue()} and {@link #getVar()} return a non-null value.
     */
    String getItemValue();

    /**
     * Returns the disabled behaviour for the select option.
     * <p>
     * This value can either be an EL expression that should resolve to a boolean value, either a string representing a
     * boolean ("true" or "false") either a Boolean value.
     */
    Serializable getItemDisabled();

    /**
     * Returns the rendered behaviour for the select option.
     * <p>
     * This value can either be an EL expression that should resolve to a boolean value, either a string representing a
     * boolean ("true" or "false") either a Boolean value.
     */
    Serializable getItemRendered();

    /**
     * Returns the unique identifier of this select option to be used in tag configuration.
     *
     * @see Layout#getTagConfigId()
     */
    String getTagConfigId();

    /**
     * @since 5.5
     */
    WidgetSelectOption clone();

}
