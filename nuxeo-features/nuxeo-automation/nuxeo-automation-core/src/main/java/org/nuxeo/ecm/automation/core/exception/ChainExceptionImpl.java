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
package org.nuxeo.ecm.automation.core.exception;

import java.util.ArrayList;

import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.core.ChainExceptionDescriptor;

/**
 * @since 5.7.3
 */
public class ChainExceptionImpl implements ChainException {

    protected final String id;

    protected final String onChainId;

    protected final ArrayList<CatchChainException> catchChainExceptions = new ArrayList<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getOnChainId() {
        return onChainId;
    }

    @Override
    public ArrayList<CatchChainException> getCatchChainExceptions() {
        return catchChainExceptions;
    }

    public ChainExceptionImpl(String id, String onChainId,
            ArrayList<CatchChainException> chainExceptionRuns) {
        this.id = id;
        this.onChainId = onChainId;
        this.catchChainExceptions.addAll(chainExceptionRuns);
    }

    public ChainExceptionImpl(ChainExceptionDescriptor chainExceptionDescriptor) {
        this.id = chainExceptionDescriptor.getId();
        this.onChainId = chainExceptionDescriptor.getOnChainId();
        for(ChainExceptionDescriptor.ChainExceptionRun chainExceptionRunDesc: chainExceptionDescriptor.getChainExceptionsRun()){
            CatchChainException chainExceptionRun = new CatchChainException(chainExceptionRunDesc.getChainId(),
                    chainExceptionRunDesc.getPriority(),chainExceptionRunDesc.getRollBack(),chainExceptionRunDesc.getFilterId());
            catchChainExceptions.add(chainExceptionRun);
        }
    }
}
