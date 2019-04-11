/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.platform.groups.audit.service;

import java.io.File;
import java.util.Map;

/**
 * Excel Export service provides contributions registration to build excel export with template and injected data (See
 * examples in audit-groups-template.xls)
 *
 * @since 5.7
 */
public interface ExcelExportService {

    File getExcelReport(String exportName);

    File getExcelReport(String exportName, Map<String, Object> beans);
}
