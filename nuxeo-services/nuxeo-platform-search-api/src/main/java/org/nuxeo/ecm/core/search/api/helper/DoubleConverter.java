/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.api.helper;

import java.text.DecimalFormat;

public class DoubleConverter {

    private static final char NEGATIVE_PREFIX = '-';
    private static final char POSITIVE_PREFIX = '0';

    private static final Double MAX_ALLOWED = 99999999999999.00;
    private static final Double MIN_ALLOWED = -100000000000000.00;

    private static final String FORMAT = "000000000000000.00";

    private DoubleConverter() {
    }

    public static String format(Object value) {
        Double i = (Double) value;
        if (i < MIN_ALLOWED || i > MAX_ALLOWED) {
            throw new IllegalArgumentException("out of allowed range");
        }
        char prefix;
        if (i < 0) {
            prefix = NEGATIVE_PREFIX;
            i = MAX_ALLOWED + i + 1;
        } else {
            prefix = POSITIVE_PREFIX;
        }
        DecimalFormat fmt = new DecimalFormat(FORMAT);
        return prefix + fmt.format(i);
    }

}
