/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core;

import java.util.ArrayList;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.7.3 The exception chain declaration for Automation exceptions.
 */
@XObject("catchChain")
public class ChainExceptionDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@onChainId")
    protected String onChainId;

    @XNodeList(value = "run", type = ArrayList.class, componentType = ChainExceptionRun.class)
    protected ArrayList<ChainExceptionRun> chainExceptionRuns;

    public String getOnChainId() {
        return onChainId;
    }

    public ArrayList<ChainExceptionRun> getChainExceptionsRun() {
        return chainExceptionRuns;
    }

    public String getId() {
        return id;
    }

    @XObject("run")
    public static class ChainExceptionRun {

        @XNode("@chainId")
        protected String chainId;

        @XNode("@priority")
        protected Integer priority = 0;

        @XNode("@rollBack")
        protected Boolean rollBack = true;

        @XNode("@filterId")
        protected String filterId = "";

        public String getChainId() {
            return chainId;
        }

        public Integer getPriority() {
            return priority;
        }

        public Boolean getRollBack() {
            return rollBack;
        }

        public String getFilterId() {
            return filterId;
        }
    }
}
