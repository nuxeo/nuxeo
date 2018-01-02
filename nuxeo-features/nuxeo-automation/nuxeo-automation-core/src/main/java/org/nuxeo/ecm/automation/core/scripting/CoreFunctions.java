/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.scripting;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.automation.context.ContextHelper;

/**
 * The functions exposed by the core library. You may want to extend this class to expose more functions under the "Fn"
 * context variable for scripting expressions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CoreFunctions implements ContextHelper {

    public DateWrapper date(Date date) {
        return new DateWrapper(date);
    }

    public DateWrapper calendar(Calendar date) {
        return new DateWrapper(date);
    }

    public String escapeHtml(Object obj) {
        return StringEscapeUtils.escapeHtml(obj.toString());
    }

}
