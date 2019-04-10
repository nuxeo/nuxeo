package org.nuxeo.ecm.user.registration.webengine;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.START_PAGE_SAVE_KEY;
import static org.nuxeo.ecm.user.registration.UserRegistrationService.REGISTRATION_DATA_DOC;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.user.registration.AlreadyProcessedRegistrationException;
import org.nuxeo.ecm.user.registration.DocumentRegistrationInfo;
import org.nuxeo.ecm.user.registration.UserRegistrationException;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@Path("/userRegistration")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "userRegistration")
public class UserRegistrationObject extends ModuleRoot {
    private static final Log log = LogFactory.getLog(UserRegistrationObject.class);

    @GET
    @Path("validate/{requestId}")
    public Object validateTrialForm(@PathParam("requestId")
    String requestId) throws Exception {
        UserRegistrationService usr = Framework.getLocalService(UserRegistrationService.class);
        String redirectUrl = ctx.getServerURL() + "/" + BaseURL.getWebAppName();
        try {
            Map<String, Serializable> additionalInfo = buildAdditionalInfos();
            Map<String, Serializable> registrationData = usr.validateRegistration(requestId, additionalInfo);
            DocumentModel regDoc = (DocumentModel) registrationData.get(REGISTRATION_DATA_DOC);
            String docId = (String) regDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD);
            if (!StringUtils.isEmpty(docId)) {
                redirectUrl = new DocumentUrlFinder(docId).getDocumentUrl();
            }
        } catch (AlreadyProcessedRegistrationException ape) {
            log.info("Try to validate an already processed registration");
        } catch (UserRegistrationException ue) {
            log.warn("Unable to validate registration request", ue);
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ue.getMessage());
        } catch (ClientException e) {
            log.error("Error while validating registration request", e);
            return getView("ValidationErrorTemplate").arg("error", e);
        }

        return redirect("/" + BaseURL.getWebAppName() + "/logout");
    }

    protected Map<String, Serializable> buildAdditionalInfos() {
        return new HashMap<String, Serializable>();
    }

    protected class DocumentUrlFinder extends UnrestrictedSessionRunner {

        protected String docId;

        protected String documentUrl;

        public DocumentUrlFinder(String docId) throws Exception {
            super(Framework.getService(RepositoryManager.class).getDefaultRepository().getName());
            this.docId = docId;
        }

        public String getDocumentUrl() throws ClientException {
            runUnrestricted();
            return documentUrl;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel target = session.getDocument(new IdRef(docId));
            documentUrl = DocumentModelFunctions.documentUrl(null, target,
                    "view_documents", null, true, ctx.getRequest());
        }
    }
}
