package org.nuxeo.theme.editor;

import org.nuxeo.ecm.webengine.WebException;

public class ThemeEditorException extends WebException {

    private static final long serialVersionUID = 1L;

    public ThemeEditorException(Throwable cause) {
        super(cause);
    }

    public ThemeEditorException(String message) {
        super(message);
    }

    public ThemeEditorException(String message, Throwable cause) {
        super(message, cause);
    }

}
