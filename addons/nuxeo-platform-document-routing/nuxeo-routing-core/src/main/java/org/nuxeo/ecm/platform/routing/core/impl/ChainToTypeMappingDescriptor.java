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
