/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
public class BlockDirective implements TemplateDirectiveModel {

    @Override
    public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
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
        @SuppressWarnings("resource") // not ours to close
        BlockWriter writer = (BlockWriter) env.getOut();
        BlockWriterRegistry reg = writer.getRegistry();
        @SuppressWarnings("resource") // BlockWriter chaining makes close() hazardous
        BlockWriter bw = new BlockWriter(page, name, reg);
        bw.ifBlockDefined = ifBlockDefined;
        writer.writeBlock(bw);
        // render this block
        if (body != null) {
            body.render(bw);
        }
    }

}
