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

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.Map;

/**
 * Single select option top be held by the {@link WidgetDefinition} and
 * {@link Widget} generated from the definition.
 *
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public interface WidgetSelectOption extends Serializable {

    /**
     * Returns the value representing the option.
     * <p>
     * This value is optional when using static label and values, it can be
     * useful to use it in conjunction with the {@link #getVar()} method to
     * retrieve the id and label from the object.
     */
    Serializable getValue();

    /**
     * Returns the var representing the value returned by {@link #getValue()}
     * <p>
     * This value can be used in the potential EL expressions returned by
     * {@link #getItemLabel()}, {@link #getItemValue()},
     * {@link #getItemDisabled()} and {@link #getItemRendered()}.
     */
    String getVar();

    /**
     * Returns the item label for the select option.
     * <p>
     * This can be an EL expression if {@link #getValue()} and
     * {@link #getVar()} return a non-null value.
     */
    String getItemLabel();

    /**
     * Getter to handle l10n localization of select options.
     *
     * @since 5.9.6
     */
    String getItemLabel(String locale);

    /**
     * Getter to handle l10n localization of select options.
     *
     * @since 5.9.6
     */
    Map<String, String> getItemLabels();

    /**
     * Returns the item value for the select option.
     * <p>
     * This can be an EL expression if {@link #getValue()} and
     * {@link #getVar()} return a non-null value.
     */
    String getItemValue();

    /**
     * Returns the disabled behaviour for the select option.
     * <p>
     * This value can either be an EL expression that should resolve to a
     * boolean value, either a string representing a boolean ("true" or
     * "false") either a Boolean value.
     */
    Serializable getItemDisabled();

    /**
     * Returns the rendered behaviour for the select option.
     * <p>
     * This value can either be an EL expression that should resolve to a
     * boolean value, either a string representing a boolean ("true" or
     * "false") either a Boolean value.
     */
    Serializable getItemRendered();

    /**
     * Returns the unique identifier of this select option to be used in tag
     * configuration.
     *
     * @see {@link Layout#getTagConfigId()}.
     */
    String getTagConfigId();

    /**
     * @since 5.5
     */
    WidgetSelectOption clone();

}
