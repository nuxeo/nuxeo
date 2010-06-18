package org.nuxeo.ecm.platform.lock;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;

import org.mortbay.log.Log;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.platform.lock.api.AlreadyLockedException;
import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.ecm.platform.lock.api.LockInfo;
import org.nuxeo.ecm.platform.lock.api.NoSuchLockException;
import org.nuxeo.ecm.platform.lock.api.NotOwnerException;
import org.nuxeo.runtime.api.Framework;

public class LockCoordinatorImpl implements LockCoordinator {

    PersistenceProvider persistenceProvider;

    public PersistenceProvider getOrCreatePersistenceProvider() {
        if (persistenceProvider != null) {
            return persistenceProvider;
        }
        PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
        return persistenceProvider = persistenceProviderFactory.newProvider("nxlocks");
    }

    protected void deactivatePersistenceProvider() {
        if (persistenceProvider == null) {
            return;
        }
        persistenceProvider.closePersistenceUnit();
        persistenceProvider = null;
    }

    public LockInfo getInfo(final URI resource) throws NoSuchLockException {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<LockInfo>() {
                        public LockInfo runWith(EntityManager em)
                                throws ClientException {
                            return getInfo(em, resource);
                        }
                    });
        } catch (NoSuchLockException e) {
            throw e;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected LockInfo getInfo(EntityManager em, URI resource)
            throws NoSuchLockException {
        LockRecord record = new LockRecordProvider(em).getRecord(resource);
        return new LockInfoImpl(record);
    }

    public void lock(final URI self, final URI resource, final String comment,
            final long timeout) throws AlreadyLockedException,
            InterruptedException {
        try {
            getOrCreatePersistenceProvider().run(false,
                    new RunCallback<Integer>() {
                        public Integer runWith(EntityManager em)
                                throws ClientException {
                            lock(em, self, resource, comment, timeout);
                            return 0;
                        }
                    });
        } catch (AlreadyLockedException e) {
            throw e;
        } catch (WrappingInterruptedException e) {
            throw (InterruptedException) e.getCause();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void waitFor(LockRecord record) throws InterruptedException {
        long remaining = remaining(record);
        while (remaining > 0) {
            Thread.sleep(remaining);
            remaining = remaining(record);
        }
    }

    protected static class WrappingInterruptedException extends ClientException {

        private static final long serialVersionUID = 1L;

        public WrappingInterruptedException(InterruptedException cause) {
            super(cause);
        }

    }

    public void lock(EntityManager em, URI self, URI resource, String comment,
            long timeout) throws AlreadyLockedException,
            WrappingInterruptedException {

        LockRecordProvider lockProvider = new LockRecordProvider(em);

        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            lockProvider.createRecord(self, resource, comment, timeout);
            transaction.commit();
            return;
        } catch (EntityExistsException exists) {
            try {
                waitAndTryRelock(self, resource, comment, timeout);
            } catch (InterruptedException interrupted) {
                throw new WrappingInterruptedException(interrupted);
            }
        }
    }

    public void waitAndTryRelock(final URI self, final URI resource,
            final String comment, final long timeout)
            throws AlreadyLockedException, InterruptedException {
        try {
            getOrCreatePersistenceProvider().run(false,
                    new RunCallback<Integer>() {
                        public Integer runWith(EntityManager em)
                                throws ClientException {
                            waitAndTryRelock(em, self, resource, comment,
                                    timeout);
                            return 0;
                        }
                    });
        } catch (AlreadyLockedException e) {
            throw e;
        } catch (WrappingInterruptedException e) {
            throw (InterruptedException) e.getCause();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void waitAndTryRelock(EntityManager em, URI self, URI resource,
            String comment, long timeout) throws WrappingInterruptedException,
            AlreadyLockedException {
        EntityTransaction transaction = em.getTransaction();
        LockRecordProvider lockProvider = new LockRecordProvider(em);

        LockRecord record = lockProvider.getRecord(resource);
        try {
            waitFor(record);
        } catch (InterruptedException e) {
            throw new WrappingInterruptedException(e);
        }
        try {
            transaction.begin();
            lockProvider.updateRecord(self, resource, comment, timeout);
            transaction.commit();
        } catch (OptimisticLockException e) {
            Log.debug("Concurent access detected, trying relocking", e);
            try {
                lock(self, resource, comment, timeout);
            } catch (InterruptedException interrupted) {
                throw new WrappingInterruptedException(interrupted);
            }
        } catch (EntityNotFoundException e) {
            throw new AlreadyLockedException(resource);
        }
    }

    private long remaining(LockRecord record) {
        long now = new Date().getTime();
        long remaining = record.expireTime.getTime() - now;
        return remaining;
    }

    public void saveInfo(URI self, URI resource, Serializable info)
            throws NotOwnerException {

    }

    public void unlock(final URI self, final URI resource)
            throws NoSuchLockException, NotOwnerException {
        try {
            getOrCreatePersistenceProvider().run(true,
                    new RunCallback<Integer>() {
                        public Integer runWith(EntityManager em)
                                throws ClientException {
                            unlock(em, self, resource);
                            return 0;
                        }
                    });
        } catch (NoSuchLockException e) {
            throw e;
        } catch (NotOwnerException e) {
            throw e;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void unlock(EntityManager em, URI self, URI resource)
            throws NoSuchLockException, NotOwnerException {
        LockRecordProvider lockProvider = new LockRecordProvider(em);
        LockRecord record;
        // entity there ?
        try {
            record = lockProvider.getRecord(resource);
        } catch (EntityNotFoundException e) {
            throw new NoSuchLockException(resource);
        }
        // same owner ?
        if (!self.equals(record.owner)) {
            throw new NotOwnerException(resource);
        }

        lockProvider.delete(resource);
    }

}
