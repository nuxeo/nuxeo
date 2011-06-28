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
package org.nuxeo.ecm.automation.client.model;

import java.util.Date;

/**
 * @author matic
 *
 */
public class DateInput implements OperationInput {

    private static final long serialVersionUID = -240778472381265434L;

    public DateInput(Date date) {
        this.date = date;
    }

    protected final Date date;

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public String getInputType() {
        return "date";
    }

    @Override
    public String getInputRef() {
        return "date:"+DateUtils.formatDate(date);
    }

}
