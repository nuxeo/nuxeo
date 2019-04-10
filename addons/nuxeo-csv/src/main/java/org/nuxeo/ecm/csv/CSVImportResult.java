package org.nuxeo.ecm.csv;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImportResult {

    protected final long totalLineCount;

    protected final long successLineCount;

    protected final long skippedLineCount;

    protected final long errorLineCount;

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
