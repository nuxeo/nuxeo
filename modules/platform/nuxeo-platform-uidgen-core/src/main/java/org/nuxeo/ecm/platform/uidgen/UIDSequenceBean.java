/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Dragos Mihalache
 */
package org.nuxeo.ecm.platform.uidgen;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UID entity - keeps last indexes of all generated UIDs.
 */
@Entity
@NamedQueries({ @NamedQuery(name = "UIDSequence.findByKey", query = "from UIDSequenceBean seq where seq.key = :key") })
@Table(name = "NXP_UIDSEQ")
public class UIDSequenceBean {

    public static final Log log = LogFactory.getLog(UIDSequenceBean.class);

    @Id
    @Column(name = "SEQ_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected int id;

    @Column(name = "SEQ_KEY", nullable = false, unique = true)
    private String key;

    @Column(name = "SEQ_INDEX", nullable = false)
    private int index;

    /**
     * Default constructor needed for EJB container instantiation.
     */
    public UIDSequenceBean() {
    }

    /**
     * Constructor taking as argument the key for which this sequence is created. The index is defaulted to 1.
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

    public static String stringify(UIDSequenceBean bean) {
        return "UIDSeq(" + bean.key + "," + bean.index + ")";
    }

    @Override
    public String toString() {
        return stringify(this);
    }

    public int nextIndex() {
        index += 1;
        log.debug("updated to " + this);
        return index;
    }

}
