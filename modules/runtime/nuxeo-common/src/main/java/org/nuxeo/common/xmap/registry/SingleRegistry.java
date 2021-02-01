/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.registry;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedMember;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * Registry for a single contribution.
 *
 * @since 11.5
 */
public class SingleRegistry extends AbstractRegistry implements Registry {

    private static final Logger log = LogManager.getLogger(SingleRegistry.class);

    // volatile for double-checked locking
    protected volatile Object contribution;

    // volatile for double-checked locking
    protected volatile boolean enabled = true;

    @Override
    public void initialize() {
        setContribution(null);
        enabled = true;
        super.initialize();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getContribution() {
        checkInitialized();
        return enabled ? Optional.ofNullable((T) contribution) : Optional.empty();
    }

    protected void setContribution(Object contribution) {
        this.contribution = contribution;
    }

    @Override
    protected boolean shouldMerge(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        if (super.shouldMerge(ctx, xObject, element, extensionId)) {
            XAnnotatedMember merge = xObject.getMerge();
            if (contribution != null && xObject.getCompatWarnOnMerge() && !merge.hasValue(ctx, element)) {
                log.warn("A contribution on extension '{}' has been implicitly merged: the compatibility "
                        + "mechanism on its descriptor class '{}' detected it, and the attribute merge=\"true\" "
                        + "should be added to this definition.", extensionId, contribution.getClass().getName());
            }
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        if (shouldRemove(ctx, xObject, element, extensionId)) {
            setContribution(null);
            return null;
        }

        Object contrib;
        if (shouldMerge(ctx, xObject, element, extensionId)) {
            contrib = getMergedInstance(ctx, xObject, element, contribution);
        } else {
            contrib = getInstance(ctx, xObject, element);
        }
        setContribution(contrib);

        Boolean enable = shouldEnable(ctx, xObject, element, extensionId);
        if (enable != null) {
            this.enabled = Boolean.TRUE.equals(enable);
        }

        return (T) contrib;
    }

}
