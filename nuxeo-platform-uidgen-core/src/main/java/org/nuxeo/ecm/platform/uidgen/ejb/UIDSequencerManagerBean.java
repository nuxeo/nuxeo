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

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
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

    @PersistenceContext(unitName = "NXUIDSequencer")
    private EntityManager em;
    
    protected UIDSequenceBean doGetOrCreateSeq(String key) {
        final Query q = em.createNamedQuery("UIDSequence.findByKey");
        q.setParameter("key", key);
        UIDSequenceBean sequence;
        try {
            sequence = (UIDSequenceBean)q.getSingleResult();
        } catch (NoResultException e) {
            sequence = new UIDSequenceBean(key);
            em.persist(sequence);
            log.debug("created seq " + key);
        }
        return sequence;
    }
    /**
     * Handle transaction synchronizing on a static field so that two calls to
     * this method will give a distinct index (see NXP-2157)
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int getNext(String key) {
        return doGetOrCreateSeq(key).nextIndex();
    }

}
