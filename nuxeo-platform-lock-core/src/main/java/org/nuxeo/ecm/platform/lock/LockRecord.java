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

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * JPA Entity for a lock record
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
@Entity(name = "Lock")
@Table(name = "NX_LOCK")
@NamedQueries( {
        @NamedQuery(name = "Lock.findByResource", query = "from Lock lock where lock.resource = :resource"),
        @NamedQuery(name = "Lock.findExpired", query = "from Lock lock where lock.expireTime < :time order by expireTime"),
        @NamedQuery(name = "Lock.deleteByResource", query = "delete Lock lock where lock.resource = :resource") })
public class LockRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "LOCK_ID", nullable = false)
    protected Long id;

    @Version
    protected Long version;

    @Column(name = "LOCK_RESOURCE", unique = true)
    protected URI resource;

    @Column(name = "LOCK_LOCK_TIME", nullable = false)
    protected Date lockTime;

    @Column(name = "LOCK_EXPIRE_TIME", nullable = false)
    protected Date expireTime;

    @Column(name = "LOCK_OWNER", nullable = false)
    protected URI owner;

    @Column(name = "LOCK_COMMENTS")
    protected String comments;

    @Column(name = "LOCK_INFO")
    protected Serializable info;

    public LockRecord() {
    }

    protected LockRecord(URI owner, URI resource, String comments,
            Date lockTime, Date expireTime) {
        this.owner = owner;
        this.resource = resource;
        this.lockTime = lockTime;
        this.comments = comments;
        this.expireTime = expireTime;
    }
}
