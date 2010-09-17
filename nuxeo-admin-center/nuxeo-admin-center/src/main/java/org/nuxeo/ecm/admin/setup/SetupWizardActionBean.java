package org.nuxeo.ecm.admin.setup;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;

@Scope(ScopeType.SESSION)
@Name("setupWizardAction")
public class SetupWizardActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    protected Map<String, String> parameters = null;

    protected static boolean needsRestart=false;

    @Factory(value="setupRequiresRestart", scope = ScopeType.EVENT)
    public boolean isNeedsRestart() {
        return needsRestart;
    }

    @Factory(value="setupParams", scope=ScopeType.PAGE)
    public Map<String, String> getParameters() {
        if (parameters==null) {
            readParameters();
        }
        return parameters;
    }

    protected void readParameters() {
        parameters = new HashMap<String, String>();

        // TODO : read parameters from properties file

        parameters.put("nuxeo.template", "default");
        parameters.put("nuxeo.bind.address", "0.0.0.0");
        parameters.put("nuxeo.url","http://localhost:8080/nuxeo");
    }

    public void saveParameters() {

        // TODO : save back in properties

        facesMessages.add(FacesMessage.SEVERITY_INFO, "label.parameters.saved");
        needsRestart=true;
        resetParameters();
    }

    public void resetParameters() {
        readParameters();
        Contexts.getPageContext().remove("setupParams");
        Contexts.getEventContext().remove("setupRequiresRestart");
    }

}
