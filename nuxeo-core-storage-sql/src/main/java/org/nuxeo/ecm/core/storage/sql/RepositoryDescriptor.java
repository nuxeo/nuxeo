/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Repository descriptor.
 *
 * @author Florent Guillaume
 */
@XObject(value = "repository")
public class RepositoryDescriptor {

    private static final Log log = LogFactory.getLog(RepositoryDescriptor.class);

    @XNode("@name")
    public String name;

    @XNode("xa-datasource")
    public String xaDataSourceName;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties;

    /** The possible id generation policies. */
    public static enum IdGenPolicy {

        /**
         * Let the Nuxeo application generate a random UUID.
         */
        APP_UUID("application-uuid"),

        /**
         * Let the database generate its own integer using sequences or
         * identity.
         */
        DB_IDENTITY("database-identity");

        private String value;

        private IdGenPolicy(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static IdGenPolicy fromString(String value) {
            for (IdGenPolicy e : values()) {
                if (e.value.equals(value)) {
                    return e;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }

    /**
     * Which id generation policy to use.
     * <p>
     * The default is {@link IdGenPolicy#APP_UUID}.
     */
    @XNode("id-generation")
    public void setIdGeneration(String value) {
        try {
            idGenPolicy = IdGenPolicy.fromString(value);
        } catch (IllegalArgumentException e) {
            log.error("Illegal id generation policy: " + value +
                    ", using default: " + idGenPolicy.getValue());
        }
    }

    public IdGenPolicy idGenPolicy = IdGenPolicy.APP_UUID;

    /**
     * Is the "main" table (containing type information and from which ids are
     * generated) separate from the "hierarchy" table.
     * <p>
     * Having it separate is only needed if a node can be in several places of
     * the hierarchy at the same time (shared nodes) -- this is not implemented
     * anyway for now.
     * <p>
     * Having it <em>not</em> separate improves performance.
     */
    public boolean separateMainTable = false;

}
