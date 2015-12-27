/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
