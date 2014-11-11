/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.uidgen.ejb;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The UID sequence manager implementation as a stateless session bean.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Stateless
@Local(UIDSequencerManager.class)
@Remote(UIDSequencerManager.class)
public class UIDSequencerManagerBean implements UIDSequencerManager {

    public static final String RemoteJNDIName = "nuxeo/"
            + UIDSequencerManagerBean.class.getSimpleName() + "/remote";

    public static final String LocalJNDIName = "nuxeo/"
            + UIDSequencerManagerBean.class.getSimpleName() + "/local";

    private static final Log log = LogFactory.getLog(UIDSequencerManagerBean.class);

    public static final String SEMAPHORE = "semaphore";

    /**
     * Handle transaction synchronizing on a static field so that two calls to
     * this method will give a distinct index (see NXP-2157)
     */
    @SuppressWarnings({"unchecked"})
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int getNext(String key) {
        synchronized (SEMAPHORE) {

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("NXUIDSequencer");
            EntityManager em = emf.createEntityManager();
            EntityTransaction et = em.getTransaction();
            if (!et.isActive()) {
                em.getTransaction().begin();
            }
            // UIDSequenceBean sequence = em.find(UIDSequenceBean.class, key);
            final Query q = em.createQuery("from "
                    + UIDSequenceBean.class.getSimpleName() + " where key = '"
                    + key + "'");
            final List<UIDSequenceBean> l = q.getResultList();

            final UIDSequenceBean sequence;
            if (l.isEmpty()) {
                // create a new entry for the specified key
                sequence = new UIDSequenceBean(key);
                // em.persist(sequence);
            } else {
                // TODO : maybe integrity tests : l.size = 1;
                sequence = l.get(0);
                if (sequence == null) {
                    // something wrong has happened; maybe not deployed
                    // correctly
                    log.error("Cannot obtain valid sequence for key: " + key
                            + "; returning 0");
                    return 0;
                }
            }

            final int lastIndex = sequence.getIndex();
            // increment the index
            final int newIndex = lastIndex + 1;
            sequence.setIndex(newIndex);
            em.persist(sequence);

            em.getTransaction().commit();
            emf.close();

            return newIndex;
        }
    }

}
