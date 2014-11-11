/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api;

/**
 * Exception that can be handled at UI level to display a dedicated user
 * message
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class RecoverableClientException extends ClientException {

    private static final long serialVersionUID = 1L;

    protected final String localizedMessage;

    protected final String[] params;

    public enum Severity {
        WARN, ERROR, FATAL
    };

    protected Severity severity = Severity.ERROR;

    public RecoverableClientException(String message, String localizedMessage,
            String[] params) {
        super(message);
        this.localizedMessage = localizedMessage;
        this.params = params;
    }

    public RecoverableClientException(String message, String localizedMessage,
            String[] params, Throwable cause) {
        super(message, cause);
        this.localizedMessage = localizedMessage;
        this.params = params;
    }

    public String getLocalizedMessage() {
        return localizedMessage;
    }

    public String[] geLocalizedMessageParams() {
        return params;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
}
