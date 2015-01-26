/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.automation.core.util.PaginablePageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;

public class LogEntryList extends PaginablePageProvider<LogEntry> {

    private static final long serialVersionUID = 1L;

    public LogEntryList(PageProvider<LogEntry> pageProvider) {
        super(pageProvider);
    }
}
