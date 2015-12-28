/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
@XObject("mapping")
public class ChainToTypeMappingDescriptor {
    @XNode("@documentType")
    protected String documentType;

    @XNode("@chainId")
    protected String chainId;

    @XNode("@undoChainIdFromRunning")
    protected String undoChainIdFromRunning;

    @XNode("@undoChainIdFromDone")
    protected String undoChainIdFromDone;

    public String getDocumentType() {
        return documentType;
    }

    public String getChainId() {
        return chainId;
    }

    public String getUndoChainIdFromRunning() {
        return undoChainIdFromRunning;
    }

    public String getUndoChainIdFromDone() {
        return undoChainIdFromDone;
    }
}
