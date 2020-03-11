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

    public ChainExceptionImpl(String id, String onChainId, ArrayList<CatchChainException> chainExceptionRuns) {
        this.id = id;
        this.onChainId = onChainId;
        catchChainExceptions.addAll(chainExceptionRuns);
    }

    public ChainExceptionImpl(ChainExceptionDescriptor chainExceptionDescriptor) {
        id = chainExceptionDescriptor.getId();
        onChainId = chainExceptionDescriptor.getOnChainId();
        for (ChainExceptionDescriptor.ChainExceptionRun chainExceptionRunDesc : chainExceptionDescriptor.getChainExceptionsRun()) {
            CatchChainException chainExceptionRun = new CatchChainException(chainExceptionRunDesc.getChainId(),
                    chainExceptionRunDesc.getPriority(), chainExceptionRunDesc.getRollBack(),
                    chainExceptionRunDesc.getFilterId());
            catchChainExceptions.add(chainExceptionRun);
        }
    }
}
