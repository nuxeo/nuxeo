/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.lock;

import java.net.URI;
import java.util.Date;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFriend;
import org.nuxeo.runtime.api.Framework;

/**
 * JPA implementation of the LockRecordProvider to get/create/update/delete
 * lockRecord. This class is using its own transaction manager (need to be run
 * in its own thread if already one running)
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class JPALockRecordProvider implements LockRecordProvider,
        LockComponentDelegate {

    public static final Log log = LogFactory.getLog(JPALockRecordProvider.class);

    public void activate(LockComponent component) {
        persistenceProvider = openProvider();
    }

    public void disactivate() {
        persistenceProvider.closePersistenceUnit();
        persistenceProvider = null;
    };

    PersistenceProvider persistenceProvider;

    protected PersistenceProvider persistenceProvider() {
        return persistenceProvider;
    }

    /**
     * Lazy initialisation for solving datasource not being published at
     * activation time TODO can be replaces with a framework listener on started
     * event
     * 
     * @return
     */
    protected PersistenceProvider openProvider() {
        Properties props = Framework.getProperties();
        String txType = props.getProperty(HibernateConfiguration.TXTYPE_PROPERTY_NAME);
        props.setProperty(HibernateConfiguration.TXTYPE_PROPERTY_NAME,
                HibernateConfiguration.RESOURCE_LOCAL);
        try {
            PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
            persistenceProvider = persistenceProviderFactory.newProvider("nxlocks");
            try {
                persistenceProvider.closePersistenceUnit();
            } catch (Throwable t) {
                ;
            }
            return persistenceProvider;
        } finally {
            if (txType != null) {
                props.setProperty(HibernateConfiguration.TXTYPE_PROPERTY_NAME,
                        txType);
            }
        }
    }

    protected EntityManager open(boolean start) {
        EntityManager em = PersistenceProviderFriend.acquireEntityManager(persistenceProvider());
        if (start == true) {
            em.getTransaction().begin();
        }
        return em;

    }

    protected void clear(EntityManager em) {
        em.clear();
    }

    protected void close(EntityManager em) {
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

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.lock.LockRecordProvider#delete(java.net.URI)
     */
    public void delete(URI resource) {
        EntityManager em = open(true);
        try {
            Query query = em.createNamedQuery("Lock.deleteByResource");
            query.setParameter("resource", resource);
            query.executeUpdate();
        } catch (Throwable e) {
            log.debug("Caught an exception while updating lock", e);
            throw new Error("Caught an exception while updating lock", e);
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.lock.LockRecordProvider#getRecord(java.net.URI)
     */
    public LockRecord getRecord(URI resourceUri) {
        EntityManager em = open(false);
        try {
            Query query = em.createNamedQuery("Lock.findByResource");
            query.setParameter("resource", resourceUri);
            return (LockRecord) query.getSingleResult();
        } finally {
            close(em);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.lock.LockRecordProvider#updateRecord(java.net.URI,
     * java.net.URI, java.lang.String, long)
     */
    public LockRecord updateRecord(URI self, URI resource, String comments,
            long timeout) {
        EntityManager em = open(true);
        LockRecord record;
        try {
            Date now = new Date();
            Date expire = new Date(now.getTime() + timeout);
            Query query = em.createNamedQuery("Lock.findByResource");
            query.setParameter("resource", resource);
            record = (LockRecord) query.getSingleResult();
            record.owner = self;
            record.comments = comments;
            record.lockTime = now;
            record.expireTime = expire;
        } finally {
            close(em);
        }
        return record;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.lock.LockRecordProvider#createRecord(java.net.URI,
     * java.net.URI, java.lang.String, long)
     */
    public LockRecord createRecord(URI self, URI resource, String comment,
            long timeout) {
        LockRecord record = null;
        EntityManager em = open(true);
        try {
            Date now = new Date();
            Date expire = new Date(now.getTime() + timeout);
            record = new LockRecord(self, resource, comment, now, expire);
            em.persist(record);
            close(em);
        } catch (RuntimeException e) {
            clear(em);
            throw e;
        }
        return record;
    }

}
