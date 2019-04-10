package org.nuxeo.ecm.user.registration.actions;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.INFO;
import static org.nuxeo.ecm.user.registration.UserRegistrationService.CONFIGURATION_NAME;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.user.invite.RegistrationRules;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */

@Name("userRegistrationConfigurationActions")
@Scope(ScopeType.CONVERSATION)
public class UserRegistrationConfigurationActions implements Serializable {

    private static Log log = LogFactory.getLog(UserRegistrationConfigurationActions.class);

    private static final long serialVersionUID = 53124326502194L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient UserRegistrationService userRegistrationService;

    protected DocumentModel selectedConfigurationDocument;

    protected String selectedConfiguration = CONFIGURATION_NAME;

    public String getSelectedConfiguration() {
        return selectedConfiguration;
    }

    public Set<String> getConfigurationsName() {
        return userRegistrationService.getConfigurationsName();
    }

    public void setSelectedConfiguration(String selectedConfiguration) {
        this.selectedConfiguration = selectedConfiguration;
        selectedConfigurationDocument = null;
    }

    public RegistrationRules getRules(String configurationName) throws ClientException {
        return userRegistrationService.getRegistrationRules(configurationName);
    }

    public DocumentModel getConfigurationDocument() throws ClientException {
        if (selectedConfigurationDocument == null) {
            selectedConfigurationDocument = userRegistrationService.getRegistrationRulesDocument(
                    documentManager, selectedConfiguration);
        }
        return selectedConfigurationDocument;
    }

    public void saveConfiguration() {
        try {
            documentManager.saveDocument(selectedConfigurationDocument);
            selectedConfigurationDocument = null;
            facesMessages.add(
                    INFO,
                    resourcesAccessor.getMessages().get(
                            "label.save.configuration.registration"));
        } catch (ClientException e) {
            log.warn("Unable to save configuration document: " + e.getMessage());
            log.info(e);
            facesMessages.add(
                    ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.unable.save.configuration.registration"));
        }
    }

    @Observer({ EventNames.DOCUMENT_CHANGED })
    public void resetState() {
        selectedConfiguration = CONFIGURATION_NAME;
        selectedConfigurationDocument = null;
    }
}
