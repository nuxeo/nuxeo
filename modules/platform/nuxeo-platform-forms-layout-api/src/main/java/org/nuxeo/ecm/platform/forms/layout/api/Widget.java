/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Widget interface.
 * <p>
 * A widget is built from a {@link WidgetDefinition} in a given mode.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface Widget extends Serializable {

    /**
     * Returns the widget id, unique within the facelet context.
     */
    String getId();

    /**
     * Returns the unique identifier of this widget to be used in tag configuration.
     *
     * @since 5.4.2
     * @see {@link Layout#getTagConfigId()}.
     */
    String getTagConfigId();

    /**
     * Sets the widget id, unique within the facelet context.
     */
    void setId(String id);

    /**
     * Returns the widget name used to identify it within a layout.
     */
    String getName();

    /**
     * Returns the layout name.
     */
    String getLayoutName();

    /**
     * Returns the widget type used to render it.
     */
    String getType();

    /**
     * Returns the widget type category.
     *
     * @since 5.7.3
     */
    String getTypeCategory();

    /**
     * Gets the value name used to compute tag attributes.
     */
    String getValueName();

    /**
     * Sets the value name used to compute tag attributes.
     */
    void setValueName(String valueName);

    /**
     * Returns the list of fields managed by this widget.
     */
    FieldDefinition[] getFieldDefinitions();

    /**
     * Returns the widget mode.
     * <p>
     * This mode can be different from the layout mode.
     */
    String getMode();

    /**
     * Returns the label to use in this mode.
     */
    String getLabel();

    /**
     * Return the help label to use in this mode.
     */
    String getHelpLabel();

    /**
     * Returns true if all labels are messages that need to be translated.
     * <p>
     * Defaults to false.
     */
    boolean isTranslated();

    /**
     * Returns true if the widget is handling his own labels display (usual label and help label). This can be checked
     * by the layout template to render the widget using both label and widget usual places.
     * <p>
     * Defaults to false.
     *
     * @since 5.6
     */
    boolean isHandlingLabels();

    /**
     * Get properties to use in this mode.
     * <p>
     * The way that properties will be mapped to rendered components is managed by the widget type.
     */
    Map<String, Serializable> getProperties();

    /**
     * Returns property with given name in this mode.
     *
     * @param name the property name.
     * @return the property value or null if not found.
     */
    Serializable getProperty(String name);

    /**
     * Sets property with given name on the layout. If there is already a property with this name on the widget, it will
     * be overridden.
     *
     * @param name the property name.
     * @param value the property value or null if not found.
     * @since 5.3.2
     */
    void setProperty(String name, Serializable value);

    /**
     * Returns controls on this widget.
     *
     * @since 5.7
     * @see WidgetDefinition#getControls()
     */
    Map<String, Serializable> getControls();

    /**
     * Returns control with given name.
     *
     * @since 5.7
     * @see WidgetDefinition#getControls()
     */
    Serializable getControl(String name);

    /**
     * Sets control with given name and value.
     *
     * @since 5.7
     * @see WidgetDefinition#getControls()
     */
    void setControl(String name, Serializable value);

    /**
     * Returns true if the widget is required.
     * <p>
     * This is a short link for the "required" property, already evaluated from an EL expression (if needed). Defaults
     * to false.
     */
    boolean isRequired();

    /**
     * Returns sub widgets.
     */
    Widget[] getSubWidgets();

    /**
     * Returns the widget level in the widget hierarchy.
     * <p>
     * For instance a standard widget will have a level of 0, and its potential subwidgets will have a level of 1.
     */
    int getLevel();

    /**
     * Returns the select options for this widget.
     *
     * @since 5.4.2
     */
    WidgetSelectOption[] getSelectOptions();

    /**
     * Returns the list of rendering information.
     * <p>
     * Useful for preview management where some configuration needs to be changed: what's changed can be set as
     * rendering information here to be displayed.
     *
     * @since 5.5
     */
    List<RenderingInfo> getRenderingInfos();

    /**
     * Returns true if the widget is defined globally (as opposed to being held by a layout definition).
     *
     * @since 6.0
     */
    boolean isGlobal();

    /**
     * Returns true if this widget was generated from configuration on a service, and not generated on-the-fly using
     * dynamic behaviors.
     *
     * @since 6.0
     */
    boolean isDynamic();

    /**
     * Returns the definition from which this widget instance was generated.
     * <p>
     * Useful in dev mode to show the corresponding configuration in the UI.
     *
     * @since 6.0
     */
    WidgetDefinition getDefinition();

}
