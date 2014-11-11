package org.nuxeo.ecm.core.api;

import java.util.Calendar;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;

import org.nuxeo.runtime.api.Framework;

/**
 * Resolve the conflict with a checkin/checkout of the last version in a new
 * transaction.
 * 
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 */
public class ConflictResolver extends Thread {

    private static final Log log = LogFactory.getLog(ConflictResolver.class);

    private final static String USER_TRANSACTION_NAME = "UserTransaction";

    private CoreSession session = null;

    private UserTransaction userTransaction;

    private LoginContext loginContext;

    private Repository repository;

    private DocumentRef docRef;

    private String repositoryName;

    private boolean isTransactionRollbacked = false;

    public ConflictResolver(DocumentRef docRef, String repositoryName)
            throws Exception {
        this.docRef = docRef;
        this.repositoryName = repositoryName;

    }

    protected void beginTransaction() throws Exception {
        Context ctx = new InitialContext();
        userTransaction = (UserTransaction) ctx.lookup(USER_TRANSACTION_NAME);
        userTransaction.begin();
    }

    protected void finalizeTransaction() {

        if (!isTransactionRollbacked) {
            try {
                userTransaction.commit();
            } catch (Exception e) {
                log.error("Can't correctly commit");
            }
        } else {
            try {
                userTransaction.rollback();
            } catch (Exception rbException) {
                log.error(rbException, rbException);
            }

        }
    }

    @Override
    public void run() {
        super.run();

        try {
            beginTransaction();

            loginContext = Framework.login();
            repository = Framework.getService(RepositoryManager.class).getRepository(
                    repositoryName);

            if (repository == null) {
                throw new ClientException("Cannot get repository: "
                        + repositoryName);
            }
            session = repository.open();

            VersionModel version = new VersionModelImpl();
            version.setCreated(Calendar.getInstance());
            version.setLabel(session.generateVersionLabelFor(docRef));
            version.setDescription("Automatic increment since a conflict was detected");

            session.checkIn(docRef, version);
            session.checkOut(docRef);

            DocumentModel doc = session.getDocument(docRef);

            major = (Long) doc.getPropertyValue("uid:major_version");
            minor = (Long) doc.getPropertyValue("uid:minor_version");

            session.save();

        } catch (Exception e) {
            isTransactionRollbacked = true;
            log.error(e, e);
        } finally {
            if (session != null) {
                try {
                    repository.close(session);
                } catch (Exception e) {
                    log.error("Can't correctly close session");
                }
                session = null;
            }
            try {
                loginContext.logout();
            } catch (Exception e) {
                log.error("Can't correctly logout from loginContext");
            }

            finalizeTransaction();
        }
    }

    private Long minor;

    private Long major;

    public Long getMinor() {
        return minor;
    }

    public Long getMajor() {
        return major;
    }

}
