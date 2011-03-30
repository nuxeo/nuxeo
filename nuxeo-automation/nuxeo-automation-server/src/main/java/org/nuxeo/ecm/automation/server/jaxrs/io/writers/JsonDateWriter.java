/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     slacoin
 */
package org.nuxeo.ecm.automation.server.jaxrs.io.writers;

import java.text.SimpleDateFormat;
import java.util.Date;


public class JsonDateWriter extends JsonObjectWriter<Date> {

    public JsonDateWriter() {
        super(Date.class);
    }

    @Override
    protected String type() {
        return "date";
    }
    @Override
    protected Object encode(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(date);
    }

}
