/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.exception;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.automation.AutomationFilter;
import org.nuxeo.ecm.automation.core.AutomationFilterDescriptor;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;

/**
 * @since 5.7.3
 */
public class ChainExceptionFilter implements AutomationFilter {

    protected String id;

    protected Expression value;

    @Override
    public Expression getValue() {
        return value;
    }

    @Override
    public String getId() {
        return id;
    }

    public ChainExceptionFilter(String id, String value) {
        this.id = id;
        if (value.startsWith("expr:")) {
            value = value.substring(5).trim();
            // Unescape xml checking
            value = StringEscapeUtils.unescapeXml(value);
            if (value.contains("@{")) {
                this.value = Scripting.newTemplate(value);
            } else {
                this.value = Scripting.newExpression(value);
            }
        }
    }

    public ChainExceptionFilter(
            AutomationFilterDescriptor automationFilterDescriptor) {
        this.id = automationFilterDescriptor.getId();
        String value = automationFilterDescriptor.getValue();
        if (value.startsWith("expr:")) {
            value = value.substring(5).trim();
            // Unescape xml checking
            value = StringEscapeUtils.unescapeXml(value);
            if (value.contains("@{")) {
                this.value = Scripting.newTemplate(value);
            } else {
                this.value = Scripting.newExpression(value);
            }
        }
    }

}
