/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
