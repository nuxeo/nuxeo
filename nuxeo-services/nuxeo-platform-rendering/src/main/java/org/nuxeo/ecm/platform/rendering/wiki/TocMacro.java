/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.wiki;

import org.wikimodel.wem.WikiParameters;

/**
 * Build the Table of Contents of a wiki page
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TocMacro implements WikiMacro {


    public String getName() {
        return "toc";
    }

    public void eval(WikiParameters params, String content,
            WikiSerializerHandler serializer) throws Exception {
        evalInline(params, content, serializer);
    }

    public void evalInline(WikiParameters params, String content,
            WikiSerializerHandler serializer) throws Exception {
        serializer.writer.writeText(new TocText(content));
    }

}
