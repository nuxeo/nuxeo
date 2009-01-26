package org.nuxeo.theme.editor;

import javax.ws.rs.core.Response;

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
    
    @Override
    public Response getResponse() {
        return Response.status(500).entity(this.getMessage()).build();
    }

}
