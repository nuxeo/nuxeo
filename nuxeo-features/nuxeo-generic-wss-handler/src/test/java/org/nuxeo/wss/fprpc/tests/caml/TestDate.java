/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.fprpc.tests.caml;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import junit.framework.TestCase;

public class TestDate extends TestCase {

    static final SimpleDateFormat HTTP_HEADER_DATE_FORMAT = new SimpleDateFormat(
            "dd MMM yyyy HH:mm:ss -0000", Locale.US);

    public void testDate() {
        Calendar date = Calendar.getInstance();
        String s = HTTP_HEADER_DATE_FORMAT.format(date.getTime());
        // System.out.print(s);
    }

}
