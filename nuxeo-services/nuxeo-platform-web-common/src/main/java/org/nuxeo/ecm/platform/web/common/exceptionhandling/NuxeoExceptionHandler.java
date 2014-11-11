package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface NuxeoExceptionHandler {


    public abstract void handleException(HttpServletRequest request,
            HttpServletResponse response, Throwable t) throws IOException,
            ServletException;
    
    public void setParameters(NuxeoExceptionHandlerParameters parameters);

}