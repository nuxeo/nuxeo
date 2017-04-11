/*
 * (C) Copyright 2016-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;

/**
 * MarkLogic Repository Descriptor.
 *
 * @since 8.3
 */
@XObject(value = "repository")
public class MarkLogicRepositoryDescriptor extends DBSRepositoryDescriptor {

    public MarkLogicRepositoryDescriptor() {
    }

    @XNode("host")
    public String host;

    @XNode("port")
    public Integer port;

    @XNode("user")
    public String user;

    @XNode("password")
    public String password;

    @XNode("dbname")
    public String dbname;

    @XNodeList(value = "range-element-indexes/range-element-index", type = ArrayList.class, componentType = MarkLogicRangeElementIndexDescriptor.class)
    public List<MarkLogicRangeElementIndexDescriptor> rangeElementIndexes = new ArrayList<>(0);

    @Override
    public MarkLogicRepositoryDescriptor clone() {
        MarkLogicRepositoryDescriptor clone = (MarkLogicRepositoryDescriptor) super.clone();
        clone.rangeElementIndexes = rangeElementIndexes.stream().map(MarkLogicRangeElementIndexDescriptor::new).collect(
                Collectors.toList());
        return clone;
    }

    public void merge(MarkLogicRepositoryDescriptor other) {
        super.merge(other);
        if (other.host != null) {
            host = other.host;
        }
        if (other.port != null) {
            port = other.port;
        }
        if (other.password != null) {
            password = other.password;
        }
        if (other.dbname != null) {
            dbname = other.dbname;
        }
        for (MarkLogicRangeElementIndexDescriptor regi : other.rangeElementIndexes) {
            rangeElementIndexes.add(new MarkLogicRangeElementIndexDescriptor(regi));
        }
    }

}
