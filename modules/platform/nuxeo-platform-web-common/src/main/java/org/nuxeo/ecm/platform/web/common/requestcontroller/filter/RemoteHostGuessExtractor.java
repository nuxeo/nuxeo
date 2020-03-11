/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * @author matic
 */
public class RemoteHostGuessExtractor {

    static final List<String> HEADER_NAMES = Arrays.asList("x-forwarded-for", "x-forwarded", "forwarded-for", "via",
            "x-coming-from", "coming-from");

    public static String getRemoteHost(HttpServletRequest request) {
        for (String name : HEADER_NAMES) {
            String value = request.getHeader(name);
            if (value != null) {
                return value;
            }
        }
        return request.getRemoteAddr();
    }

}
