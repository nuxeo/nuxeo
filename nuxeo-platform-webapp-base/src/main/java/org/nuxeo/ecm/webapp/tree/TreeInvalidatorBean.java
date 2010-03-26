package org.nuxeo.ecm.webapp.tree;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("treeInvalidator")
@Scope(SESSION)
@Install(precedence = Install.FRAMEWORK)
public class TreeInvalidatorBean {

    protected boolean needsInvalidation=false;

    public String forceTreeRefresh() throws IOException {

        needsInvalidation=true;

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setContentType("application/xml; charset=UTF-8");
        response.getWriter().write("<response>OK</response>");
        context.responseComplete();

        return null;
    }

    public boolean needsInvalidation() {
        return needsInvalidation;
    }

    public void invalidationDone() {
        needsInvalidation=false;
    }

}
