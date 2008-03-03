package org.nuxeo.ecm.core.search.transaction;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * Utility class used to encapsulate Transactions.
 * Code borrowed from JBoss Seam 1.1.5
 *
 */
public class Transactions {
    private static String userTransactionName = "UserTransaction";

    public static void setTransactionRollbackOnly() throws SystemException,
            NamingException {
        try {
            setUTRollbackOnly();
        } catch (NameNotFoundException ne) {
            // XXX
        }
    }

    public static boolean isTransactionActive() throws SystemException,
            NamingException {
        try {
            return isUTTransactionActive();
        } catch (NameNotFoundException ne) {
            // XXX
            return false;
        }
        // temporary workaround for a bad bug in Glassfish!
        catch (IllegalStateException ise) {
            // XXX
            return false;
        }
    }

    public static boolean isTransactionActiveOrMarkedRollback()
            throws SystemException, NamingException {
        try {
            return isUTTransactionActiveOrMarkedRollback();
        } catch (NameNotFoundException ne) {
            // XXX
            return false;
        }
        // temporary workaround for a bad bug in Glassfish!
        catch (IllegalStateException ise) {
            // XXX
            return false;
        }
    }

    public static boolean isTransactionMarkedRollback() throws SystemException,
            NamingException {
        try {
            return isUTTransactionMarkedRollback();
        } catch (NameNotFoundException ne) {
            // XXX
            return false;
        }
        // temporary workaround for a bad bug in Glassfish!
        catch (IllegalStateException ise) {
            return false;
        }
    }

    private static void setUTRollbackOnly() throws SystemException,
            NamingException {
        getUserTransaction().setRollbackOnly();
    }

    private static boolean isUTTransactionActive() throws SystemException,
            NamingException {
        return getUserTransaction().getStatus() == STATUS_ACTIVE;
    }

    private static boolean isUTTransactionActiveOrMarkedRollback()
            throws SystemException, NamingException {
        int status = getUserTransaction().getStatus();
        return status == STATUS_ACTIVE || status == STATUS_MARKED_ROLLBACK;
    }

    private static boolean isUTTransactionMarkedRollback()
            throws SystemException, NamingException {
        return getUserTransaction().getStatus() == STATUS_MARKED_ROLLBACK;
    }

    protected static Context getInitialContext() throws Exception {
        return new InitialContext();
    }

    public static UserTransaction getUserTransaction() throws NamingException {
        try {
            try {
                Context ctx = getInitialContext();
                return (UserTransaction) ctx.lookup(userTransactionName);
            } catch (Exception nnfe) {
                return null;
            }
        }
        // not really necessary, but just in case...
        catch (IllegalStateException ise) {
            throw new NameNotFoundException("Lookup " + userTransactionName
                    + " threw IllegalStateException: " + ise.getMessage());
        }
    }

    private Transactions() {
    }

    public static void setUserTransactionName(String userTransactionName) {
        Transactions.userTransactionName = userTransactionName;
    }

    public static String getUserTransactionName() {
        return userTransactionName;
    }

}
