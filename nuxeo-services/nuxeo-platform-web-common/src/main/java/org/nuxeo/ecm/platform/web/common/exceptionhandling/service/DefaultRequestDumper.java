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
 */
public class DefaultRequestDumper implements RequestDumper {

    private static final Log log = LogFactory.getLog(DefaultRequestDumper.class);

    protected List<String> attributes = new ArrayList<>();

    @Override
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
            } catch (RuntimeException exc) {
                // avoid errors when printing the error dump
                log.error("ERROR TRYING TO GET THIS REQUEST ATTRIBUTE VALUE: " + name);
                builder.append("ERROR TRYING TO GET THIS REQUEST ATTRIBUTE VALUE");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    @Override
    public void setNotListedAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

}
