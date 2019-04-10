/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.platform.groups.audit.service;

import java.io.File;
import java.util.Map;

/**
 * Excel Export service provides contributions registration to build excel
 * export with template and injected data (See examples in
 * audit-groups-template.xls)
 * 
 * @since 5.7
 */
public interface ExcelExportService {

    File getExcelReport(String exportName);

    File getExcelReport(String exportName, Map<String, Object> beans);
}
