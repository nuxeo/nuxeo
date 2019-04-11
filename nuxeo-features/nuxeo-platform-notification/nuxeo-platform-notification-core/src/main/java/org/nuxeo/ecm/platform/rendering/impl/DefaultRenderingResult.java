/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.nuxeo.ecm.platform.rendering.RenderingResult;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class DefaultRenderingResult implements RenderingResult {

    private static final long serialVersionUID = 6570212240033856735L;

    protected final String formatName;

    /**
     * Constructor taking a rendering result name as argument.
     *
     * @param formatName
     */
    protected DefaultRenderingResult(String formatName) {
        this.formatName = formatName;
    }

    /**
     * @return name of the engine that created it
     */
    @Override
    public String getFormatName() {
        return formatName;
    }

    @Override
    public abstract Object getOutcome();

    @Override
    public InputStream getStream() {
        Object outcome = getOutcome();
        if (outcome instanceof InputStream) {
            return (InputStream) outcome;
        } else if (outcome instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) outcome);
        } else if (outcome instanceof CharSequence) {
            return new ByteArrayInputStream(outcome.toString().getBytes());
        }
        return getAdapter(InputStream.class);
    }

    @Override
    public <E> E getAdapter(Class<E> adapter) {
        Object outcome = getOutcome();
        if (adapter.isAssignableFrom(outcome.getClass())) {
            return adapter.cast(outcome);
        }
        return null;
    }

}
