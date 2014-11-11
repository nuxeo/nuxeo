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
 *     Thierry Delprat
 * *
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * @author Thierry Delprat
 */
@XObject("groupComputer")
public class GroupComputerDescriptor implements Serializable {

    protected static Log log = LogFactory.getLog(GroupComputerDescriptor.class);

    private static final long serialVersionUID = 1L;

    @XNode("computer")
    protected Class<GroupComputer> computerClass;

    protected GroupComputer groupComputer;

    @XNode("@name")
    protected String name;

    public String getName() {
        if (name != null) {
            return name;
        }
        return computerClass.getSimpleName();
    }

    public GroupComputer getComputer() throws ClientException {
        if (groupComputer == null) {
            if (computerClass != null) {
                try {
                    groupComputer = computerClass.newInstance();
                } catch (Exception e) {
                    log.error("Enable to instanciate GroupComputer", e);
                    throw new ClientException(
                            "Enable to instanciate GroupComputer", e);
                }
            } else {
                groupComputer = null;
            }
        }
        return groupComputer;
    }

}
