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

package org.nuxeo.ecm.webengine.validation;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ValidationStatus {

    public static final ValidationStatus OK = new ValidationStatus(true, null, null);

    protected boolean isOk;
    protected String message;
    protected String field;

    public ValidationStatus(boolean isOk, String field) {
        this (isOk, field, "");
    }

    public ValidationStatus(boolean isOk, String field, String message) {
        this.isOk = isOk;
        this.field = field;
        this.message = message;
    }

    /**
     * @return the isOk.
     */
    public boolean isOk() {
        return isOk;
    }

    /**
     * @return the field.
     */
    public String getField() {
        return field;
    }

    /**
     * @return the message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return isOk ? "OK" : "KO "+message;
    }
}
