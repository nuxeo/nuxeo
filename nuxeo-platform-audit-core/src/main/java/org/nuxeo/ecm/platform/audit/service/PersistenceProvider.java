package org.nuxeo.ecm.platform.audit.service;

import java.io.InputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

public class PersistenceProvider {

    protected static final Log log = LogFactory.getLog(PersistenceProvider.class);

    protected EntityManagerFactory emf;

    public PersistenceProvider() {
        super();
    }

    public static Class<? extends HibernateConfiguration> hibernateConfigurationClass = DefaultHibernateConfiguration.class;

    public void openPersistenceUnit() {
        Ejb3Configuration cfg = new Ejb3Configuration();

        InputStream persistenceStream = getClass().getResourceAsStream(
                "persistence.xml");
        if (persistenceStream != null) {
            cfg.addInputStream(persistenceStream);
        }
        
        try {
            cfg.addProperties(hibernateConfigurationClass.newInstance().getProperties());
        } catch (Exception error) {
            throw new AuditRuntimeException(
                    "Cannot load hibernate configuration", error);
        }

        cfg.addAnnotatedClass(LogEntry.class);
        cfg.addAnnotatedClass(ExtendedInfo.class);
        cfg.addAnnotatedClass(ExtendedInfo.BlobInfo.class);
        cfg.addAnnotatedClass(ExtendedInfo.BooleanInfo.class);
        cfg.addAnnotatedClass(ExtendedInfo.DateInfo.class);
        cfg.addAnnotatedClass(ExtendedInfo.DoubleInfo.class);
        cfg.addAnnotatedClass(ExtendedInfo.StringInfo.class);
        cfg.addAnnotatedClass(ExtendedInfo.LongInfo.class);

        cfg.configure("fake-hibernate.cfg.xml");

        emf = cfg.buildEntityManagerFactory();
    }

    public void closePersistenceUnit() {
        if (emf.isOpen()) {
            emf.close();
        }
        emf = null;
    }

    protected EntityManager guardedEntityManager() {
        if (emf == null) {
            log.error("Unable to get EntityManager, there is no factory");
            throw new AuditRuntimeException(
                    "Cannot get entity manager, no factory available");
        }
        return emf.createEntityManager();
    }

    public EntityManager acquireEntityManager() {
        return guardedEntityManager();
    }

    public EntityManager acquireEntityManagerWithActiveTransaction() {
        EntityManager em = guardedEntityManager();
        EntityTransaction et = em.getTransaction();
        et.begin();
        return em;
    }

    protected void doCommit(EntityManager em) {
        em.flush();
        EntityTransaction et = em.getTransaction();
        if (et.isActive() == false)
            return;
        et.commit();
    }

    protected void doRollback(EntityManager em) {
        EntityTransaction et = em.getTransaction();
        if (et.isActive() == false)
            return;
        et.rollback();
    }

    public void releaseEntityManager(EntityManager em) {
        if (em.isOpen() == false)
            return;
        try {
            doCommit(em);
        } finally {
            if (em.isOpen()) {
                em.clear();
                em.close();
            }
        }
    }

    public void releaseEntityManagerWithRollback(EntityManager em) {
        if (em.isOpen() == false)
            return;
        try {
            doRollback(em);
        } finally {
            if (em.isOpen()) {
                em.clear();
                em.close();
            }
        }
    }

}
