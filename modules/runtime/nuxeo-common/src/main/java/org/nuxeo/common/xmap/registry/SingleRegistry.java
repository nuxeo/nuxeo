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

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedMember;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * @since TODO
 */
public class SingleRegistry extends AbstractRegistry implements Registry {

    protected Object contribution;

    protected boolean enabled = true;

    public SingleRegistry() {
        super();
    }

    public Object getContribution() {
        checkInitialized();
        if (enabled) {
            return contribution;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getContribution(Class<T> contributionClass) {
        return (T) getContribution();
    }

    protected void setContribution(Object contribution) {
        this.contribution = contribution;
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element) {
        XAnnotatedMember remove = xObject.getRemove();
        if (remove != null && Boolean.TRUE.equals(remove.getValue(ctx, element))) {
            setContribution(null);
            return;
        }
        Object contrib;
        XAnnotatedMember merge = xObject.getMerge();
        if (merge != null && Boolean.TRUE.equals(merge.getValue(ctx, element))) {
            contrib = xObject.newInstance(ctx, element, contribution);
        } else {
            contrib = xObject.newInstance(ctx, element);
        }
        setContribution(contrib);
        XAnnotatedMember enable = xObject.getEnable();
        if (enable != null) {
            Object enabled = enable.getValue(ctx, element);
            if (enabled != null) {
                this.enabled = Boolean.TRUE.equals(enabled);
            }
        }
    }

}
