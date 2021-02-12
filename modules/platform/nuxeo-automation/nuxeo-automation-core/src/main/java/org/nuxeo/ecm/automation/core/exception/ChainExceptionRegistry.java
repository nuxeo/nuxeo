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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.exception;

import java.util.Optional;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.core.ChainExceptionDescriptor;
import org.w3c.dom.Element;

/**
 * Registry for {@link ChainExceptionDescriptor} contributions.
 * <p>
 * Modified as of 11.5 to implement {@link Registry}.
 *
 * @since 5.7.3
 */
public class ChainExceptionRegistry extends MapRegistry {

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getInstance(Context ctx, XAnnotatedObject xObject, Element element) {
        ChainExceptionDescriptor desc = super.getInstance(ctx, xObject, element);
        ChainException contrib = new ChainExceptionImpl(desc);
        return (T) contrib;
    }

    public Optional<ChainException> getChainException(String onChainId) {
        return this.<ChainException> getContributionValues()
                   .stream()
                   .filter(ce -> onChainId.equals(ce.getOnChainId()))
                   .findFirst();
    }

}
