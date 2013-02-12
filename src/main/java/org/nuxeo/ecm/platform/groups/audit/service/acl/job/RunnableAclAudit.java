package org.nuxeo.ecm.platform.groups.audit.service.acl.job;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.groups.audit.service.acl.AclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.IAclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;

public class RunnableAclAudit implements Runnable {
    private final static Log log = LogFactory.getLog(RunnableAclAudit.class);

    protected transient CoreSession session;

    protected transient Work work;

    protected File out;

    protected DocumentModel root;

    /**
     * Initialize a runnable Acl Audit process, and register this process in the
     * {@link Work} instance that will execute it.
     */
    public RunnableAclAudit(CoreSession session, DocumentModel root, Work work,
            File out) {
        this.session = session;
        this.root = root;
        this.work = work;
        this.out = out;

        if (this.work != null)
            this.work.setRunnable(this);
    }

    /**
     * Initialize a runnable Acl Audit process that can be executed outside of a
     * {@link Work}.
     */
    public RunnableAclAudit(CoreSession session, DocumentModel root, File out) {
        this(session, root, null, out);
    }

    @Override
    public void run() {
        doAudit();
        onAuditDone();
    }

    public void doAudit() {
        // setup
        ReportLayoutSettings s = AclExcelLayoutBuilder.defaultLayout();
        s.setPageSize(1000);
        IContentFilter filter = null;

        // generate XLS report
        log.debug("Start audit");
        IAclExcelLayoutBuilder v = new AclExcelLayoutBuilder(s, filter);
        try {
            v.renderAudit(session, root, true, work);
            log.debug("End audit");
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }

        // save
        try {
            v.getExcel().save(out);
            log.debug("End save");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onAuditDone() {
    }

    public File getOutputFile() {
        return out;
    }
}
