package org.nuxeo.ecm.csv;

import java.util.Locale;

import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImportLog {

    public enum Status {
        SUCCESS, SKIPPED, ERROR
    }

    protected final long line;

    protected final Status status;

    protected final String message;

    protected final String localizedMessage;

    protected final Object[] params;

    public CSVImportLog(long line, Status status, String message,
            String localizedMessage, Object... params) {
        this.line = line;
        this.status = status;
        this.message = message;
        this.localizedMessage = localizedMessage;
        this.params = params;
    }

    public long getLine() {
        return line;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage() {
        return localizedMessage;
    }

    public Object[] getLocalizedMessageParams() {
        return params;
    }

    public String getI18nMessage(Locale locale) {
        return I18NUtils.getMessageString("messages", getLocalizedMessage(),
                getLocalizedMessageParams(), locale);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isSkipped() {
        return status == Status.SKIPPED;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
