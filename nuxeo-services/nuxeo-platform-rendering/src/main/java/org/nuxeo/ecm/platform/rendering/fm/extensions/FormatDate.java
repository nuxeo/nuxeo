/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public Object exec(List arguments) throws TemplateModelException {
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
            throw new TemplateModelException(
                    "the argument local is not defined");
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
