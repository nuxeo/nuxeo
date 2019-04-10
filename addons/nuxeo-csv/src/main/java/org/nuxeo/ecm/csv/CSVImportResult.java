/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv;

import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImportResult {

    protected final long totalLineCount;

    protected final long successLineCount;

    protected final long skippedLineCount;

    protected final long errorLineCount;

    public static final CSVImportResult fromImportLogs(List<CSVImportLog> importLogs) {
        long totalLineCount = importLogs.size();
        long successLineCount = 0;
        long skippedLineCount = 0;
        long errorLineCount = 0;
        for (CSVImportLog importLog : importLogs) {
            if (importLog.isSuccess()) {
                successLineCount++;
            } else if (importLog.isSkipped()) {
                skippedLineCount++;
            } else if (importLog.isError()) {
                errorLineCount++;
            }
        }
        return new CSVImportResult(totalLineCount, successLineCount,
            skippedLineCount, errorLineCount);
    }

    public CSVImportResult(long totalLineCount, long successLineCount,
            long skippedLineCount, long errorLineCount) {
        this.totalLineCount = totalLineCount;
        this.successLineCount = successLineCount;
        this.skippedLineCount = skippedLineCount;
        this.errorLineCount = errorLineCount;
    }

    public long getTotalLineCount() {
        return totalLineCount;
    }

    public long getSuccessLineCount() {
        return successLineCount;
    }

    public long getSkippedLineCount() {
        return skippedLineCount;
    }

    public long getErrorLineCount() {
        return errorLineCount;
    }
}
