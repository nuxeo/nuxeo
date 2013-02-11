package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import java.util.Collection;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessor.ProcessorStatus;

public interface IDataProcessor {
    public void analyze(CoreSession session) throws ClientException;

    public void analyze(CoreSession session, DocumentModel doc)
            throws ClientException;

    public Set<String> getUserAndGroups();

    public Set<String> getPermissions();

    /** The maximum doc tree depth */
    public int getDocumentTreeMaxDepth();

    /**
     * The minimum doc tree depth, 0 if analysis was run on repository root, >0
     * if the analysis was run on a child document of repository root.
     */
    public int getDocumentTreeMinDepth();

    public int getNumberOfDocuments();

    public Collection<DocumentSummary> getAllDocuments();

    /** A status concerning data analysis */
    public ProcessorStatus getStatus();

    /** Some text information related to the status */
    public String getInformation();
}