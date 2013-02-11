package org.nuxeo.ecm.platform.groups.audit.service.acl;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.IExcelBuilder;

public interface IAclExcelLayoutBuilder {

    /**
     * Analyze and render an ACL audit for the complete repository in
     * unrestricted mode.
     */
    public void renderAudit(CoreSession session)
            throws ClientException;

    /**
     * Analyze and render an ACL audit for the complete document tree in
     * unrestricted mode.
     */
    public void renderAudit(CoreSession session, DocumentModel doc)
            throws ClientException;

    /** Analyze and render an ACL audit for the input document and its children. */
    public void renderAudit(CoreSession session, DocumentModel doc,
            boolean unrestricted) throws ClientException;

    public IExcelBuilder getExcel();

}