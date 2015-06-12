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
package org.nuxeo.ecm.automation.core.impl.adapters;

import java.text.ParseException;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.core.schema.utils.DateParser;

/**
 * @author <a href="mailto:vvergnolle@nuxeo.com">Vincent Vergnolle</a>
 */
public class StringToCalendar implements TypeAdapter {

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        String str = (String) objectToAdapt;
        try {
            return DateParser.parse(str);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse ISO 8601 date: " + str, e);
        }
    }
}
