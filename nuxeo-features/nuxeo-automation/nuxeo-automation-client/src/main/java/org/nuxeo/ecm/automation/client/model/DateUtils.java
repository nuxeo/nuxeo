/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.util.Date;

/**
 * Parse and encode W3c dates. Only UTC dates are supported (ending in Z): YYYY-MM-DDThh:mm:ssZ (without milliseconds)
 * We use a custom parser since it should work on GWT too.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DateUtils {

    // Utility class.
    private DateUtils() {
    }

    public static Date parseDate(String date) {
        return DateParser.parseW3CDateTime(date);
    }

    public static String formatDate(Date date) {
        return DateParser.formatW3CDateTime(date);
    }

}
