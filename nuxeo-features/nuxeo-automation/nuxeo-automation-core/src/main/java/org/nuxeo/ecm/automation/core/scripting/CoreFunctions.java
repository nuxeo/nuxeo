/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.scripting;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * The functions exposed by the core library.
 * You may want to extend this class to expose more functions under
 * the "Fn" context variable for scripting expressions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CoreFunctions {

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
