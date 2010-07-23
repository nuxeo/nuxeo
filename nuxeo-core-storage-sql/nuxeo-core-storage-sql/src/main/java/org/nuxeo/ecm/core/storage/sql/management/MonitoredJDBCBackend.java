/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin 
 */

package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCBackend;

public class MonitoredJDBCBackend extends MonitoredBackend {

    public MonitoredJDBCBackend() {
       super(new JDBCBackend());
    }

}
