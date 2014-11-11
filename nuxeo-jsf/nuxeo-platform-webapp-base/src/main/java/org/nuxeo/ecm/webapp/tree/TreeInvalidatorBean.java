package org.nuxeo.ecm.webapp.tree;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import static org.jboss.seam.ScopeType.SESSION;

@Name("treeInvalidator")
@Scope(SESSION)
@Install(precedence = Install.FRAMEWORK)
public class TreeInvalidatorBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected boolean needsInvalidation = false;

    public String forceTreeRefresh() throws IOException {

        needsInvalidation = true;

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
        needsInvalidation = false;
    }

}
