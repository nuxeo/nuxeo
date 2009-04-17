/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.editor.filters;

import java.util.regex.Pattern;

import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;

public final class ScriptRemover extends StandaloneFilter {

    static final Pattern scriptPattern = Pattern.compile(
            "<script(.*?)>(.*?)</script>", Pattern.DOTALL
                    | Pattern.CASE_INSENSITIVE);

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        info.setMarkup(scriptPattern.matcher(info.getMarkup()).replaceAll(""));
        return info;
    }

}
