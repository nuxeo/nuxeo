/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */

package org.nuxeo.ecm.platform.lock;

import java.net.URI;

/**
 * LockRecordProvider api for dealing with lock record.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public interface LockRecordProvider {

    void delete(URI resource) throws InterruptedException;

    LockRecord getRecord(URI resourceUri) throws InterruptedException;

    LockRecord updateRecord(URI self, URI resource, String comments,
            long timeout) throws InterruptedException;

    LockRecord createRecord(URI self, URI resource, String comment, long timeout)
            throws InterruptedException;

}