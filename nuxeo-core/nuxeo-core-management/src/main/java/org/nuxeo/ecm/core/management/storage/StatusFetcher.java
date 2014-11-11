package org.nuxeo.ecm.core.management.storage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;

public class StatusFetcher extends UnrestrictedSessionRunner {

    protected String instanceId;
    protected String serviceId;

    protected List<String> allInstanceIds = new ArrayList<String>();
    protected List<AdministrativeStatus> statuses = new ArrayList<AdministrativeStatus>();

    public StatusFetcher(String repositoryName, String instanceId, String serviceId) {
        super(repositoryName);
        this.instanceId=instanceId;
        this.serviceId=serviceId;
    }

    @Override
    public void run() throws ClientException {

        boolean onlyFetchIds = false;

        StringBuffer sb = new StringBuffer("select * from ");
        sb.append(DocumentModelStatusPersister.ADMINISTRATIVE_STATUS_DOCUMENT_TYPE);

        if (instanceId==null) {
            onlyFetchIds=true;
        } else {
            sb.append(" where ");
            sb.append(DocumentModelStatusPersister.INSTANCE_PROPERTY);
            sb.append("='");
            sb.append(instanceId);
            sb.append("'");
            if (serviceId!=null) {
                sb.append(" AND ");
                sb.append(DocumentModelStatusPersister.SERVICE_PROPERTY);
                sb.append("='");
                sb.append(serviceId);
                sb.append("'");
            }
        }

        DocumentModelList result = session.query(sb.toString());

        for (DocumentModel doc : result) {
            if (onlyFetchIds) {
                String id = (String) doc.getPropertyValue(DocumentModelStatusPersister.INSTANCE_PROPERTY);
                if (!allInstanceIds.contains(id)) {
                    allInstanceIds.add(id);
                }
            } else {
                statuses.add(wrap(doc));
            }
        }
    }


    protected AdministrativeStatus wrap(DocumentModel doc) throws ClientException {

        String userLogin = (String) doc.getPropertyValue(DocumentModelStatusPersister.LOGIN_PROPERTY);
        String id = (String) doc.getPropertyValue(DocumentModelStatusPersister.INSTANCE_PROPERTY);
        String service = (String) doc.getPropertyValue(DocumentModelStatusPersister.SERVICE_PROPERTY);
        String message = (String) doc.getPropertyValue(DocumentModelStatusPersister.MESSAGE_PROPERTY);
        String state = (String) doc.getPropertyValue(DocumentModelStatusPersister.STATUS_PROPERTY);
        Calendar modified = (Calendar) doc.getPropertyValue("dc:modified");

        AdministrativeStatus status = new AdministrativeStatus(state,message,modified,userLogin,id,service);

        return status;
    }
}
