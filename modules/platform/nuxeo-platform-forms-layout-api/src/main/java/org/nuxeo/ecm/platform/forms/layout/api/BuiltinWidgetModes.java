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
 * $Id: BuiltinWidgetModes.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.util.List;

/**
 * List of built in widget modes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class BuiltinWidgetModes {

    public static final String VIEW = BuiltinModes.VIEW;

    public static final String EDIT = BuiltinModes.EDIT;

    /**
     * @since 5.4.2
     */
    public static final String PLAIN = BuiltinModes.PLAIN;

    /**
     * @since 5.4.2
     */
    public static final String CSV = BuiltinModes.CSV;

    /**
     * @since 5.4.2
     */
    public static final String PDF = BuiltinModes.PDF;

    public static final String HIDDEN = "hidden";

    private BuiltinWidgetModes() {
    }

    public static boolean isModeSupported(String widgetMode, List<String> supportedModes) {
        if (BuiltinWidgetModes.HIDDEN.equals(widgetMode)) {
            // always supported
            return true;
        } else if (supportedModes != null) {
            return supportedModes.contains(widgetMode);
        }
        return false;
    }

    /**
     * Returns true if given mode is one of {@link #PLAIN}, or {@link #CSV}.
     *
     * @since 5.4.2
     */
    public static boolean isLikePlainMode(String widgetMode) {
        if (widgetMode != null) {
            if (PLAIN.equals(widgetMode) || CSV.equals(widgetMode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if given mode is not null and is not one of {@link #EDIT}, {@link #PLAIN}, {@link #CSV},
     * {@link #PDF} or {@link #HIDDEN} mode.
     *
     * @since 5.4.2
     * @param widgetMode
     * @return
     */
    public static boolean isLikeViewMode(String widgetMode) {
        if (widgetMode == null) {
            return false;
        }
        if (BuiltinWidgetModes.EDIT.equals(widgetMode) || BuiltinWidgetModes.PLAIN.equals(widgetMode)
                || BuiltinWidgetModes.CSV.equals(widgetMode) || BuiltinWidgetModes.PDF.equals(widgetMode)
                || BuiltinWidgetModes.HIDDEN.equals(widgetMode)) {
            return false;
        }
        return true;
    }

}
