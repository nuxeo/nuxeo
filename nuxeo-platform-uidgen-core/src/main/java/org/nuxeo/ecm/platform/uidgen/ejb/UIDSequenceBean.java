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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * UID entity - keeps last indexes of all generated UIDs.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
@Entity
@Table(name = "UID_SEQUENCE", uniqueConstraints =
    @UniqueConstraint(columnNames = { "SEQ_KEY" }))
public class UIDSequenceBean {

    @Id
    @Column(name = "SEQ_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected int id;

    @Column(name = "SEQ_KEY")
    private String key;

    @Column(name = "SEQ_INDEX")
    private int index;

    /**
     * Default constructor needed for EJB container instantiation.
     *
     */
    public UIDSequenceBean() {
    }

    /**
     * Constructor taking as argument the key for which this sequence is
     * created. The index is defaulted to 1.
     *
     * @param key
     */
    public UIDSequenceBean(String key) {
        this.key = key;
        index = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

}
