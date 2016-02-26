/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
