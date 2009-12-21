/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
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
    public String getFormatName() {
        return formatName;
    }

    public abstract Object getOutcome();

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

    public <E> E getAdapter(Class<E> adapter) {
        Object outcome = getOutcome();
        if (adapter.isAssignableFrom(outcome.getClass())) {
            return adapter.cast(outcome);
        }
        return null;
    }

}
