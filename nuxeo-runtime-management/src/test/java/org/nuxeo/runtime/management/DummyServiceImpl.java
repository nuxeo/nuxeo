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
public class DummyServiceImpl extends DefaultComponent implements
        DummyService, DummyMBean {

    protected String message = "hello world";
    /* (non-Javadoc)
     * @see org.nuxeo.runtime.management.DummyManagedServiceManagement#getMessage()
     */
    public String getMessage() {
      return message;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.runtime.management.DummyManagedServiceManagement#sayHelloWorld()
     */
    public String sayHelloWorld() {
       return message;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.runtime.management.DummyManagedServiceManagement#setMessage(java.lang.String)
     */
    public void setMessage(String message) {
       this.message = message;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.runtime.management.DummyManagedServiceManagement#getManagedMessage()
     */
    public String getManagedMessage() {
       return message;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.runtime.management.DummyManagedServiceManagement#sayManagedHelloWorld()
     */
    public String sayManagedHelloWorld() {
       return message;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.runtime.management.DummyManagedServiceManagement#setManagedMessage(java.lang.String)
     */
    public void setManagedMessage(String message) {
       this.message = message;
    }

}
