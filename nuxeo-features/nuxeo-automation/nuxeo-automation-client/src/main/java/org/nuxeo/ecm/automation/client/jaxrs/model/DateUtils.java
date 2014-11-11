/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Parse and encode W3c dates. Only UTC dates are supported (ending in Z):
 * YYYY-MM-DDThh:mm:ssZ (without milliseconds) We use a custom parser since it
 * should work on GWT too.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DateUtils {

    // Utility class.
    private DateUtils() {
    }

    public static Date parseDate(String date) {
        try {
            if (date.endsWith("Z")) {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z").parse(date+"GMT+00:00");
            } else {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz").parse(date);
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date value " + date, e);
        }
    }


    public static String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz").format(date);
    }

    public static void main(String[] args) {
        Date d = new Date();
        String s = formatDate(d);
        System.out.println(s);
        Date d2 = parseDate(s);
        System.out.println(d + " = " + d2);
    }
}
