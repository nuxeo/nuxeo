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
 *     matic
 */
package org.nuxeo.ecm.automation.client.model;

import java.util.Date;

import org.nuxeo.ecm.automation.client.OperationRequest;

/**
 * @author matic
 * @deprecated in 5.7 (did not work in 5.6 either): pass Date instance directly to the {@link OperationRequest#setInput}
 *             method.
 */
@Deprecated
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
        return "date:" + DateUtils.formatDate(date);
    }

}
