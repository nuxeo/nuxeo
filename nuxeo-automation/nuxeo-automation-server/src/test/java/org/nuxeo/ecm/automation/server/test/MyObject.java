/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.automation.server.test;


/**
 * @author matic
 *
 */
public class MyObject {

    protected String msg = "hello world";

    public String getMessage() {
        return msg;
    }

    public void setMessage(String msg) {
        if (msg == null) {
            throw new IllegalArgumentException("msg cannot be null");
        }
        this.msg = msg;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MyObject)) {
            return false;
        }
        MyObject other = (MyObject)obj;
        return this.msg.equals(other.msg);
    }

    @Override
    public int hashCode() {
        return msg.hashCode();
    }
}
