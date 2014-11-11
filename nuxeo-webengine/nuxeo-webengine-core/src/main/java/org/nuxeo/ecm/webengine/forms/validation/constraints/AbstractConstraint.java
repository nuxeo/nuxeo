/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.forms.validation.constraints;

import org.nuxeo.ecm.webengine.forms.FormInstance;
import org.nuxeo.ecm.webengine.forms.validation.Constraint;
import org.nuxeo.ecm.webengine.forms.validation.ErrorStatus;
import org.nuxeo.ecm.webengine.forms.validation.Field;
import org.nuxeo.ecm.webengine.forms.validation.MultiStatus;
import org.nuxeo.ecm.webengine.forms.validation.Status;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AbstractConstraint implements Constraint {

    protected String errorMessage;

    public void init(Field field, String value) {
        throw new UnsupportedOperationException("adding sub-constraints or values is not supported");
    }

    public void add(Constraint value) {
        throw new UnsupportedOperationException("adding sub-constraints or values is not supported");
    }

    public boolean isContainer() {
        return false;
    }

    public Status validate(FormInstance form, Field field, String rawValue, Object value) {
        return Status.OK;
    }

    public Constraint newInstance() {
        try {
            return (Constraint)getClass().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    protected ErrorStatus error(Field field) {
        return new ErrorStatus(field.getId(), errorMessage);
    }

    protected MultiStatus error(Status status) {
        MultiStatus ms =  new MultiStatus(status.getField(), errorMessage);
        ms.add(status);
        return ms;
    }

}
