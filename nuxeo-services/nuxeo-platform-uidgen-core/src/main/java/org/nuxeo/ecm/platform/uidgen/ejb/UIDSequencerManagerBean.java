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
import javax.persistence.PersistenceContext;

import org.nuxeo.ecm.platform.uidgen.service.UIDSequencerImpl;

/**
 * The UID sequence manager implementation as a stateless session bean.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 * @author Bogdan Stefanescu
 */
@Stateless
@Local(UIDSequencerManagerLocal.class)
@Remote(UIDSequencerManager.class)
public class UIDSequencerManagerBean implements UIDSequencerManagerLocal {

    @PersistenceContext(unitName = "NXUIDSequencer")
    private EntityManager em;

    protected UIDSequencerImpl service() {
        return new UIDSequencerImpl();
    }

    /**
     * Handle transaction synchronizing on a static field so that two calls to
     * this method will give a distinct index (see NXP-2157)
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int getNext(String key) {
        return service().getNext(em, key);
    }

}
