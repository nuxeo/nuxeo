package org.nuxeo.ecm.platform.ui.flex.remoting;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.BUILT_IN;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.web.AbstractResource;

@Startup
@Scope(APPLICATION)
@Name("org.nuxeo.ecm.platform.ui.flex.amfRemoteService")
@Install(precedence = BUILT_IN)
@BypassInterceptors
public class NuxeoAMFRemoteService extends AbstractResource {

    /**
     * Holds default resource path to this service.
     */
    public static final String AMF_RESOURCE_PATH = "/nuxeo-amf";

    /**
     * Holds property name, which uses to configuring resource path to this service in server
     * configuration.
     */
    public static final String AMF_REMOTE_SERVICE_PATH_PARAM = "NuxeoAMFRemoteServicePath";


    @Override
    public void getResource(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        NuxeoAMFToSeamRequestProcessor.instance().process(request, response);
    }

    @Override
    public String getResourcePath() {
        String param = getServletContext().getInitParameter(AMF_REMOTE_SERVICE_PATH_PARAM);
        return param != null ? param : AMF_RESOURCE_PATH;
    }

}
