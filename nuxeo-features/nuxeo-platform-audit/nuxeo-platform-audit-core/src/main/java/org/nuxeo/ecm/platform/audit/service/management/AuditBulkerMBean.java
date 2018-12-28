/*******************************************************************************
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.ecm.platform.audit.service.management;

/**
 * @deprecated since 10.10, audit bulker is now handled with nuxeo-stream, no replacement
 */
@Deprecated
public interface AuditBulkerMBean {

    int getBulkTimeout();

    void setBulkTimeout(int value);

    int getBulkSize();

    void setBulkSize(int value);

    void resetMetrics();
}
