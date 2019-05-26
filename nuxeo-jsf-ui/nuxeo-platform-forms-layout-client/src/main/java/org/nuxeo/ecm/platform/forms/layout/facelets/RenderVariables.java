/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: RenderVariables.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

/**
 * List of render variables.
 * <p>
 * Variables which names will be available in a layout/widget rendering context.
 * <p>
 * This allows to use them in properties definitions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class RenderVariables {

    public enum globalVariables {
        value,
        // deprecate document: not exposed anymore
        @Deprecated
        document,
        //
        layoutValue, mode, layoutMode
    }

    public enum layoutVariables {
        layout, layoutProperty, layoutRowCount,
    }

    public enum rowVariables {
        layoutRow, layoutRowIndex,
    }

    public enum columnVariables {
        layoutColumn, layoutColumnIndex,
    }

    public enum widgetVariables {
        //
        widget,
        //
        widgetIndex,
        //
        field,
        //
        widgetProperty,
        // @since 5.8
        widgetProperties,
        // @since 5.7
        widgetControl,
        //
        fieldOrValue,
    }

    /**
     * @since 6.0
     */
    public enum widgetTemplatingZones {
        // @since 6.0, templating zone for inputs, useful for ajax
        // interactions propagated from template (typically a f:ajax element)
        inside_input_widget,
    }

}
