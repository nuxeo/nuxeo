/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.uidgen.service;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.uidgen.ejb.UIDSequenceBean;
import org.nuxeo.runtime.api.Framework;

/**
 * This implementation uses a static persistence provider to be able to
 * instantiate this class without passing by Framework.getService -> this is to
 * avoid potential problems do to sequencer factories. Anyway sequencer
 * factories should be removed (I don't think they are really needed).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UIDSequencerImpl implements UIDSequencer {

    private static volatile PersistenceProvider persistenceProvider;

    /**
     * Must be called when the service is no longer needed
     */
    public static void dispose() {
        deactivatePersistenceProvider();
    }

    public static PersistenceProvider getOrCreatePersistenceProvider() {
        if (persistenceProvider == null) {
            synchronized (UIDSequencerImpl.class) {
                if (persistenceProvider == null) {
                    activatePersistenceProvider();
                }
            }
        }
        return persistenceProvider;
    }

    private static void activatePersistenceProvider() {
        Thread thread = Thread.currentThread();
        ClassLoader last = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(PersistenceProvider.class.getClassLoader());
            PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
            persistenceProvider = persistenceProviderFactory.newProvider("NXUIDSequencer");
            persistenceProvider.openPersistenceUnit();
        } finally {
            thread.setContextClassLoader(last);
        }
    }

    private static void deactivatePersistenceProvider() {
        if (persistenceProvider != null) {
            synchronized (UIDSequencerImpl.class) {
                if (persistenceProvider != null) {
                    persistenceProvider.closePersistenceUnit();
                    persistenceProvider = null;
                }
            }
        }
    }

    public int getNext(final String key) {
        try {
            return getOrCreatePersistenceProvider().run(true,
                    new RunCallback<Integer>() {
                        public Integer runWith(EntityManager em) {
                            return getNext(em, key);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public int getNext(EntityManager em, String key) {
        UIDSequenceBean seq;
        try {
            seq = (UIDSequenceBean) em.createNamedQuery("UIDSequence.findByKey").setParameter(
                    "key", key).getSingleResult();
            // createQuery("FROM UIDSequenceBean seq WHERE seq.key = :key")
        } catch (NoResultException e) {
            seq = new UIDSequenceBean(key);
            em.persist(seq);
        }
        return seq.nextIndex();
    }

}
