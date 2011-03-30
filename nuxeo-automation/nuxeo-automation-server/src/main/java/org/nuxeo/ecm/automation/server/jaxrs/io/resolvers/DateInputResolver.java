/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.server.jaxrs.io.resolvers;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.nuxeo.ecm.automation.server.jaxrs.io.InputResolver;

/**
 * @author matic
 * 
 */
public class DateInputResolver implements InputResolver {

    @Override
    public String getType() {
        return "date";
    }

    @Override
    public Object getInput(String input) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parseObject(input);
        } catch (ParseException e) {
            throw new IllegalArgumentException(" date value " + input);
        }
    }

}
