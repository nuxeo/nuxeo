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
 *      Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class DummyService extends DefaultComponent implements Dummy,
        DummyMBean {

    protected String message = "hello world";

    public String getMessage() {
        return message;
    }

    public String sayHelloWorld() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getManagedMessage() {
        return message;
    }

    public String sayManagedHelloWorld() {
        return message;
    }

    public void setManagedMessage(String message) {
        this.message = message;
    }

}
