/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * @author matic
 *
 */
public class RemoteHostGuessExtractor {

    static final List<String> HEADER_NAMES = Arrays.asList(
            "x-forwarded-for", "x-forwarded", "forwarded-for", "via", "x-coming-from", "coming-from");

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
