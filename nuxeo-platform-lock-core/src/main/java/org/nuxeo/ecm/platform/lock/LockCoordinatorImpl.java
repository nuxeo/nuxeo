package org.nuxeo.ecm.platform.lock;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFriend;
import org.nuxeo.ecm.platform.lock.api.AlreadyLockedException;
import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.ecm.platform.lock.api.LockInfo;
import org.nuxeo.ecm.platform.lock.api.NoSuchLockException;
import org.nuxeo.ecm.platform.lock.api.NotOwnerException;
import org.nuxeo.runtime.api.Framework;

public class LockCoordinatorImpl implements LockCoordinator {

    public static final Log log = LogFactory.getLog(LockCoordinatorImpl.class);

    PersistenceProvider persistenceProvider;

    public PersistenceProvider getOrCreatePersistenceProvider() {
        PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
        persistenceProvider = persistenceProviderFactory.newProvider("nxlocks");
        return persistenceProvider;
    }

    public void activate() {
    }

    public void desactivate() {
        if (persistenceProvider != null) {
            persistenceProvider.closePersistenceUnit();
            persistenceProvider = null;
        }
    }

    protected void debug(String message, URI self, URI resource,
            String comment, long timeout) {
        if (log.isDebugEnabled()) {
            log.debug(message + " owner: " + self + " resource: " + resource
                    + " comments: " + comment + " timeout: " + timeout);
        }
    }

    protected LockRecordProvider open(boolean start) {
        EntityManager em = PersistenceProviderFriend.acquireEntityManager(getOrCreatePersistenceProvider());
        if (start == true) {
            em.getTransaction().begin();
        }
        return new LockRecordProvider(em);
    }

    protected void clear(LockRecordProvider provider) {
        provider.em.clear();
    }

    protected void close(LockRecordProvider provider) {
        EntityManager em = provider.em;
        try {
            EntityTransaction et = em.getTransaction();
            if (et != null && et.isActive()) {
                et.commit();
            }
        } finally {
            em.clear();
            em.close();
        }
    }

    public void lock(final URI self, final URI resource, final String comment,
            final long timeout) throws AlreadyLockedException,
            InterruptedException {
        debug("Lock", self, resource, comment, timeout);
        doLock(self, resource, comment, timeout);
    }

    protected void doLock(final URI self, final URI resource,
            final String comment, final long timeout)
            throws AlreadyLockedException, InterruptedException {

        LockRecordProvider provider = open(true);
        try {
            debug("createRecord", self, resource, comment, timeout);
            provider.createRecord(self, resource, comment, timeout);
            close(provider);
        } catch (EntityExistsException exists) {
            debug("Couldn't create, entity already exists, fetching resource",
                    self, resource, comment, timeout);

            LockRecord record = doFetch(resource);

            debug("wait for existing lock timeout", self, resource, comment,
                    timeout);
            doWaitFor(record);

            debug("do update", self, resource, comment, timeout);
            doUpdate(self, resource, comment, timeout);

        }

    }

    protected void doWaitFor(LockRecord record) throws InterruptedException {
        long remaining = remaining(record);
        while (remaining > 0) {
            Thread.sleep(remaining);
            remaining = remaining(record);
        }
    }

    protected LockRecord doFetch(URI resource) throws AlreadyLockedException {
        LockRecordProvider provider = open(false);
        try {
            return provider.getRecord(resource);
        } catch (EntityNotFoundException notfound) {
            throw new AlreadyLockedException(resource);
        } finally {
            close(provider);
        }
    }

    protected void doUpdate(URI self, URI resource, String comment, long timeout)
            throws AlreadyLockedException, InterruptedException {
        LockRecordProvider np = open(true);
        try {
            np.updateRecord(self, resource, comment, timeout);
            close(np);
        } catch (OptimisticLockException e) {
            debug("doUpdate: concurent access detected", self, resource,
                    comment, timeout);
            log.debug("Concurent access detected, trying relocking", e);
            doLock(self, resource, comment, timeout);
        } catch (NoResultException e) {
            throw new AlreadyLockedException(resource);
        } catch (Throwable e) {
            log.warn("Unexpected problem while updating", e);
            throw new Error("Unexpected problem while updating " + resource, e);
        }

    }

    private long remaining(LockRecord record) {
        long now = new Date().getTime();
        long remaining = record.expireTime.getTime() - now;
        return remaining;
    }

    public LockInfo getInfo(final URI resource) throws NoSuchLockException {
        LockRecordProvider provider = open(false);
        try {
            LockRecord record = provider.getRecord(resource);
            return new LockInfoImpl(record);
        } finally {
            close(provider);
        }
    }

    public void saveInfo(URI self, URI resource, Serializable info)
            throws NotOwnerException {
        LockRecordProvider provider = open(true);
        LockRecord record = provider.getRecord(resource);
        if (!self.equals(record.owner)) {
            close(provider);
            throw new NotOwnerException(resource);
        }
        record.info = info;
        close(provider);
    }

    public void unlock(URI self, URI resource) throws NoSuchLockException,
            NotOwnerException {
        LockRecordProvider lockProvider = open(false);
        try {
            LockRecord record;
            // entity there ?
            try {
                record = lockProvider.getRecord(resource);
            } catch (NoResultException e) {
                throw new NoSuchLockException(e, resource);
            }
            // same owner ?
            if (!self.equals(record.owner)) {
                throw new NotOwnerException(resource);
            }
        } finally {
            close(lockProvider);
        }

        lockProvider = open(true);
        try {
            lockProvider.delete(resource);
        } catch (Throwable e) {
            log.trace("Caught an exception while updating lock", e);
            throw new Error("Caught an exception while updating lock", e);
        } finally {
            try {
                close(lockProvider);
            } catch (EntityNotFoundException notfound) {
                throw new NoSuchLockException(notfound, resource);
            }
        }
        log.trace("deleted " + resource);
    }

}
