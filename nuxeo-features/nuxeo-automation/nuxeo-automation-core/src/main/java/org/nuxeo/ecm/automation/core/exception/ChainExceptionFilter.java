/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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

    public ChainExceptionFilter(AutomationFilterDescriptor automationFilterDescriptor) {
        id = automationFilterDescriptor.getId();
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
