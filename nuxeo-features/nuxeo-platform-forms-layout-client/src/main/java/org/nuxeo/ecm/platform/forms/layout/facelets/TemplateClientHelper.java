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
 * $Id: TemplateClientHelper.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for iterations over rows, widgets and subwidgets.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TemplateClientHelper {

    public static final String ROW_FACELET_NAME = "row";

    public static final String WIDGET_FACELET_NAME = "widget";

    public static final String SUBWIDGET_FACELET_NAME = "subwidget";

    final static String PATTERN = "_([0-9]+)";

    private TemplateClientHelper() {
    }

    private static Integer parse(String prefix, String name) {
        Pattern pat = Pattern.compile(prefix + PATTERN);
        Matcher m = pat.matcher(name);
        if (m.matches()) {
            String number = m.group(1);
            return new Integer(number);
        }
        return null;
    }

    private static String format(String prefix, int number) {
        return String.format("%s_%s", prefix, number);
    }

    // row

    public static Integer getRowNumber(String name) {
        return parse(ROW_FACELET_NAME, name);
    }

    public static String generateRowNumber(int number) {
        return format(ROW_FACELET_NAME, number);
    }

    // widget

    public static Integer getWidgetNumber(String name) {
        return parse(WIDGET_FACELET_NAME, name);
    }

    public static String generateWidgetNumber(int number) {
        return format(WIDGET_FACELET_NAME, number);
    }

    // subwidget

    public static Integer getSubWidgetNumber(String name) {
        return parse(SUBWIDGET_FACELET_NAME, name);
    }

    public static String generateSubWidgetNumber(int number) {
        return format(SUBWIDGET_FACELET_NAME, number);
    }

}
