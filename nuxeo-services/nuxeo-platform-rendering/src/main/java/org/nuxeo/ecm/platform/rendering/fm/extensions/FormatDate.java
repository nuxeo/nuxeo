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
 *     stan
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import freemarker.template.SimpleDate;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Format a date with the specified locale.
 *
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 */
public class FormatDate implements TemplateMethodModelEx {

    @Override
    public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
        if (arguments.size() != 2) {
            throw new TemplateModelException(
                    "Invalid number of arguments for formatDate(Date date, String locale) method");
        }
        if (!(arguments.get(0) instanceof SimpleDate && arguments.get(1) instanceof SimpleScalar)) {
            throw new TemplateModelException(
                    "Invalid arguments format for the method formatDate : expecting (Date date, String local).");
        }
        SimpleScalar scalar = (SimpleScalar) arguments.get(1);
        if (scalar == null) {
            throw new TemplateModelException("the argument local is not defined");
        }

        SimpleDate simpledate = (SimpleDate) arguments.get(0);
        if (simpledate == null) {
            throw new TemplateModelException("the argument date is not defined");
        }

        Date date = simpledate.getAsDate();

        Locale locale = new Locale(scalar.getAsString());

        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, locale);

        return new SimpleScalar(df.format(date));
    }

}
