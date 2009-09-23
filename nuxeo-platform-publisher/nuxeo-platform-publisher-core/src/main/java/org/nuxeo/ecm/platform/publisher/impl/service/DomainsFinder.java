package org.nuxeo.ecm.platform.publisher.impl.service;

import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

import java.util.List;
import java.util.ArrayList;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DomainsFinder extends UnrestrictedSessionRunner {

    protected List<DocumentModel> domains;

    public DomainsFinder(String repositoryName) {
        super(repositoryName);
    }

    @Override
    public void run() throws ClientException {
        domains = new ArrayList<DocumentModel>();
        DocumentRef rootRef = session.getRootDocument().getRef();
        for (DocumentModel doc : session.getChildren(rootRef, "Domain")) {
            domains.add(doc);
        }
    }

    public List<DocumentModel> getDomains() throws ClientException {
        if (domains == null) {
            runUnrestricted();
        }
        return domains;
    }

}
