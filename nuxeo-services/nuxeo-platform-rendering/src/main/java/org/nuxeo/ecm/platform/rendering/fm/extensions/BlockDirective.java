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

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlockDirective implements TemplateDirectiveModel {

    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        String name = null;
        SimpleScalar scalar = (SimpleScalar) params.get("name");
        if (scalar != null) {
            name = scalar.getAsString();
        }

        scalar = (SimpleScalar) params.get("ifBlockDefined");
        String ifBlockDefined = null;
        if (scalar != null) {
            ifBlockDefined = scalar.getAsString();
        }

        String page = env.getTemplate().getName();
        BlockWriter writer = (BlockWriter) env.getOut();
        BlockWriterRegistry reg = writer.getRegistry();
        BlockWriter bw = new BlockWriter(page, name, reg);
        bw.ifBlockDefined = ifBlockDefined;
        writer.writeBlock(bw);
        // render this block
        if (body != null) {
            body.render(bw);
        }
    }

}
