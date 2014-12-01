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

import java.io.IOException;

import org.wikimodel.wem.WikiParameters;

import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WikiMacro {

    String getName();

    void eval(WikiParameters params, String content,
            WikiSerializerHandler serializer) throws IOException,
            TemplateException;

    void evalInline(WikiParameters params, String content,
            WikiSerializerHandler serializer) throws IOException,
            TemplateException;

}
