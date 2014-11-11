/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */

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

    protected HibernateConfiguration hibernateConfiguration;

    public PersistenceProvider(HibernateConfiguration configuration) {
        hibernateConfiguration = configuration;
    }

    public void setHibernateConfiguration(HibernateConfiguration configuration) {
        hibernateConfiguration = configuration;
    }

    public void openPersistenceUnit() {
        Ejb3Configuration cfg = new Ejb3Configuration();

        InputStream persistenceStream = getClass().getResourceAsStream(
                "persistence.xml");
        if (persistenceStream != null) {
            cfg.addInputStream(persistenceStream);
        }

        try {
            cfg.addProperties(hibernateConfiguration.getProperties());
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
        if (emf == null) {
            return;
        }
        if (emf.isOpen()) {
            emf.close();
        }
        emf = null;
    }

    protected EntityManager doEntityManager() {
        if (emf == null) {
           openPersistenceUnit();
        }
        return emf.createEntityManager();
    }

    public EntityManager acquireEntityManager() {
        return doEntityManager();
    }

    public EntityManager acquireEntityManagerWithActiveTransaction() {
        EntityManager em = doEntityManager();
        EntityTransaction et = em.getTransaction();
        et.begin();
        return em;
    }

    protected void doCommit(EntityManager em) {
        EntityTransaction et = em.getTransaction();
        if (!et.isActive()) {
            return;
        }
        em.flush();
        et.commit();
    }

    protected void doRollback(EntityManager em) {
        EntityTransaction et = em.getTransaction();
        if (!et.isActive()) {
            return;
        }
        em.flush();
        et.rollback();
    }

    public void releaseEntityManager(EntityManager em) {
        if (!em.isOpen()) {
            return;
        }
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
        if (!em.isOpen()) {
            return;
        }
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
