/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv.core;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImportResult implements Serializable {

    private static final long serialVersionUID = 1L;

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
        return new CSVImportResult(totalLineCount, successLineCount, skippedLineCount, errorLineCount);
    }

    public CSVImportResult(long totalLineCount, long successLineCount, long skippedLineCount, long errorLineCount) {
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
