package org.nuxeo.theme.webwidgets;

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;

public class WidgetEditorException extends WebException {

    private static final long serialVersionUID = 1L;

    public WidgetEditorException(Throwable cause) {
        super(cause);
    }

    public WidgetEditorException(String message) {
        super(message);
    }

    public WidgetEditorException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public Response getResponse() {
        return Response.status(500).entity(this.getMessage()).build();
    }

}
