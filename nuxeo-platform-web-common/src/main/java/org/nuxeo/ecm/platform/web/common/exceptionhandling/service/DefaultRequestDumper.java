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
 *     arussel
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling.service;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author arussel
 *
 */
public class DefaultRequestDumper implements RequestDumper {

    private static final Log log = LogFactory.getLog(DefaultRequestDumper.class);

    protected List<String> attributes = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    public String getDump(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("\nRequest Attributes:\n\n");
        Enumeration<String> e = request.getAttributeNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            if (attributes.contains(name)) {
                continue;
            }
            builder.append(name);
            builder.append(" : ");
            try {
                Object obj = request.getAttribute(name);
                builder.append(obj.toString());
            } catch (Exception error) {
                // avoid errors when printing the error dump
                log.error("ERROR TRYING TO GET THIS REQUEST ATTRIBUTE VALUE: "
                        + name);
                builder.append("ERROR TRYING TO GET THIS REQUEST ATTRIBUTE VALUE");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public void setNotListedAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

}
