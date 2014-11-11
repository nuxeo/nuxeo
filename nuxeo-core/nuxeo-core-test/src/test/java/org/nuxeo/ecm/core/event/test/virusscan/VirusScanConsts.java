/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.event.test.virusscan;

public interface VirusScanConsts {

    public static final String VIRUS_SCAN_NEEDED_EVENT = "virusScanNeeded";

    public static final String VIRUSSCAN_FACET = "VIRUSSCAN";

    public static final String VIRUSSCAN_STATUS_PROP = "vscan:scanStatus";

    public static final String VIRUSSCAN_STATUS_PENDING = "pending";

    public static final String VIRUSSCAN_STATUS_DONE = "done";

    public static final String VIRUSSCAN_STATUS_FAILED = "failed";

    public static final String VIRUSSCAN_DATE_PROP = "vscan:scanDate";

    public static final String VIRUSSCAN_INFO_PROP = "vscan:scanInfo";

    public static final String VIRUSSCAN_OK_PROP = "vscan:virusFree";

    public static final String DISABLE_VIRUSSCAN_LISTENER = "disableVirusScanListener";

}
