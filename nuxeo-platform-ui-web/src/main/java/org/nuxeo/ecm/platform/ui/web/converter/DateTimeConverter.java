/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.converter;

import java.util.TimeZone;

/**
 * Formats dates according to the TimeZone of the server.
 * <p>
 * In the future the TimeZone of the client should be used
 *
 * @author Narcis Paslaru
 *
 */
public class DateTimeConverter extends javax.faces.convert.DateTimeConverter {

    public static final String CONVERTER_ID = "org.nuxeo.ecm.platform.ui.web.util.DateTimeConverter";

    public DateTimeConverter() {
        setTimeZone(TimeZone.getDefault());
        setPattern("dd/MM/yyyy HH:mm");
    }

}
