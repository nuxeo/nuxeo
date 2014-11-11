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

    public static final boolean isModeSupported(String widgetMode,
            List<String> supportedModes) {
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
    public static final boolean isLikePlainMode(String widgetMode) {
        if (widgetMode != null) {
            if (PLAIN.equals(widgetMode) || CSV.equals(widgetMode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if given mode is not null and is not one of {@link #EDIT},
     * {@link #PLAIN}, {@link #CSV}, {@link #PDF} or {@link #HIDDEN} mode.
     *
     * @since 5.4.2
     * @param widgetMode
     * @return
     */
    public static final boolean isLikeViewMode(String widgetMode) {
        if (widgetMode == null) {
            return false;
        }
        if (BuiltinWidgetModes.EDIT.equals(widgetMode)
                || BuiltinWidgetModes.PLAIN.equals(widgetMode)
                || BuiltinWidgetModes.CSV.equals(widgetMode)
                || BuiltinWidgetModes.PDF.equals(widgetMode)
                || BuiltinWidgetModes.HIDDEN.equals(widgetMode)) {
            return false;
        }
        return true;
    }

}
