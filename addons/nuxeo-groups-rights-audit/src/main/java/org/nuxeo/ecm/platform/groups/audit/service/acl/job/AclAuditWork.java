/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.groups.audit.service.acl.job;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.groups.audit.service.acl.AclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.IAclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.IResultPublisher;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class AclAuditWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private final static Log log = LogFactory.getLog(AclAuditWork.class);

    public static final String PROPERTY_ACL_AUDIT_TIMEOUT = "nuxeo.audit.acl.timeout";

    public static final int DEFAULT_TIMEOUT = 1200; // 20 min

    public static final int UNDEFINED_TIMEOUT = -1;

    protected String name;

    protected int timeout;

    protected IResultPublisher publisher;

    protected File out;

    /**
     * Initialize a runnable Acl Audit process, and register this process in the {@link Work} instance that will execute
     * it.
     */
    public AclAuditWork(String name, String repositoryName, String rootId, File out, IResultPublisher publisher) {
        this(name, repositoryName, rootId, out, publisher, getAclAuditTimeoutFromProperties());
    }

    public static int getAclAuditTimeoutFromProperties() {
        String v = Framework.getProperty(PROPERTY_ACL_AUDIT_TIMEOUT, UNDEFINED_TIMEOUT + "");
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return UNDEFINED_TIMEOUT;
        }
    }

    /**
     * Initialize a runnable Acl Audit process, and register this process in the {@link Work} instance that will execute
     * it.
     */
    public AclAuditWork(String name, String repositoryName, String rootId, File out, IResultPublisher publisher,
            int timeout) {
        super(repositoryName + ':' + rootId + ":aclAudit");
        setDocument(repositoryName, rootId, true);
        this.name = name;
        this.out = out;
        this.publisher = publisher;
        if (timeout == UNDEFINED_TIMEOUT) {
            timeout = DEFAULT_TIMEOUT;
        }
        this.timeout = timeout;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public void work() {
        // use an explicit transaction timeout
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction(timeout);
        }
        setProgress(Progress.PROGRESS_0_PC);
        openSystemSession();
        doAudit();
        onAuditDone();
        setProgress(Progress.PROGRESS_100_PC);
    }

    public void doAudit() {
        // setup
        ReportLayoutSettings s = AclExcelLayoutBuilder.defaultLayout();
        s.setPageSize(1000);
        IContentFilter filter = null;

        // generate XLS report
        log.debug("Start audit");
        IAclExcelLayoutBuilder v = new AclExcelLayoutBuilder(s, filter);
        DocumentModel root = session.getDocument(new IdRef(docId));
        v.renderAudit(session, root, true, timeout);
        log.debug("End audit");

        // save
        try {
            v.getExcel().save(out);
            log.debug("End save");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onAuditDone() {
        try {
            // content to send
            Blob fb = Blobs.createBlob(getOutputFile(), "application/xls");
            // do publish
            publisher.publish(fb);
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public File getOutputFile() {
        return out;
    }
}
