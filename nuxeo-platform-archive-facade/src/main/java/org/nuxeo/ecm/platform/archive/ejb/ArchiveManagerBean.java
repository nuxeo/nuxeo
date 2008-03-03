/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.archive.ejb;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.archive.api.ArchiveManager;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecord;
import org.nuxeo.ecm.platform.archive.ejb.local.ArchiveManagerLocal;

/**
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 *
 */
@Stateful
@Local(ArchiveManagerLocal.class)
@Remote(ArchiveManager.class)
public class ArchiveManagerBean implements ArchiveManager {

    private static final long serialVersionUID = 6184215911401444935L;

    private static final Log log = LogFactory.getLog(ArchiveManager.class);

    @PersistenceContext(unitName = "nxarchive")
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<ArchiveRecord> getArchiveRecordsByDocUID(String docUID) {
        log.info("getArchiveRecordsByDocUID() UID=" + docUID);
        return em.createNamedQuery("listArchiveRecordsByDocUID").setParameter(
                "docUID", docUID).getResultList();
    }

    public void addArchiveRecord(ArchiveRecord record) {
        em.persist(record);
    }

    public Boolean deleteArchiveRecord(long recordId) {
        ArchiveRecord rec = findArchiveRecordById(recordId);
        if (rec != null) {
            em.remove(rec);
            return true;
        }
        return false;
    }

    public void editArchiveRecord(ArchiveRecord record) {
        em.merge(record);
        em.flush();
    }

    public ArchiveRecord searchArchiveRecord() {
        // TODO Auto-generated method stub
        return null;
    }

    @Remove
    public void remove() {
        log.debug("instance remove");
    }

    public ArchiveRecord findArchiveRecordById(long archiveRecordId) {
        return em.find(ArchiveRecordImpl.class, archiveRecordId);
    }

    public List<ArchiveRecord> findArchiveRecords(String qlString, Map<String, Object>  parameters) {
        Query query = em.createQuery(qlString);
        for (String name : parameters.keySet()) {
            Object value = parameters.get(name);
            if (value instanceof Date) {
                query.setParameter(name, (Date) value, TemporalType.DATE);
            } else if (value instanceof Calendar) {
                query.setParameter(name, (Calendar) value, TemporalType.DATE);
            } else {
                query.setParameter(name, value);
            }
        }
        List<ArchiveRecord> records = (List<ArchiveRecord>) query.getResultList();
        return records;
    }

}
