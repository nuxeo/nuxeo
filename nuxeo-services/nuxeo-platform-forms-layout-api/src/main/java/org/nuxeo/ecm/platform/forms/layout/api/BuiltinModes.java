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
 * $Id: BuiltinModes.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

/**
 * List of built-in modes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class BuiltinModes {

    public static final String ANY = "any";

    public static final String VIEW = "view";

    public static final String EDIT = "edit";

    public static final String BULK_EDIT = "bulkEdit";

    public static final String CREATE = "create";

    public static final String SEARCH = "search";

    public static final String SUMMARY = "summary";

    /**
     * @since 5.4.2
     */
    public static final String CSV = "csv";

    /**
     * @since 5.4.2
     */
    public static final String PDF = "pdf";

    /**
     * @since 5.4.2
     */
    public static final String PLAIN = "plain";

    /**
     * @since 6.0
     */
    public static final String DEV = "dev";

    private BuiltinModes() {
    }

    /**
     * Returns true if given layout mode is mapped by default to the edit widget mode.
     */
    public static boolean isBoundToEditMode(String layoutMode) {
        if (layoutMode != null) {
            if (layoutMode.startsWith(CREATE) || layoutMode.startsWith(EDIT) || layoutMode.startsWith(SEARCH)
                    || layoutMode.startsWith(BULK_EDIT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the default mode to use for a widget, given the layout mode.
     * <p>
     * Returns {@link BuiltinWidgetModes#EDIT} for all modes bound to edit, {@link BuiltinWidgetModes#VIEW} for modes
     * {@link #VIEW}, {@link #HEADER} and {@link #SUMMARY}. {@link #PDF} and {@link #CSV} are respectively bound to
     * {@link BuiltinWidgetModes#PDF} and {@link BuiltinWidgetModes#CSV}. In other cases, returns
     * {@link BuiltinWidgetModes#PLAIN}.
     * <p>
     * This method is not called when mode is explicitely set on the widget.
     */
    public static String getWidgetModeFromLayoutMode(String layoutMode) {
        if (layoutMode != null) {
            if (isBoundToEditMode(layoutMode)) {
                return BuiltinWidgetModes.EDIT;
            } else if (layoutMode.startsWith(VIEW) || layoutMode.startsWith(SUMMARY)) {
                return BuiltinWidgetModes.VIEW;
            } else if (layoutMode.startsWith(CSV)) {
                return BuiltinWidgetModes.CSV;
            } else if (layoutMode.startsWith(PDF)) {
                return BuiltinWidgetModes.PDF;
            }
        }
        return BuiltinWidgetModes.PLAIN;
    }

}
