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

package org.nuxeo.ecm.platform.rendering.wiki;

import java.io.IOException;

import org.wikimodel.wem.WikiParameters;

/**
 * Build the Table of Contents of a wiki page
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TocMacro implements WikiMacro {

    @Override
    public String getName() {
        return "toc";
    }

    @Override
    public void eval(WikiParameters params, String content, WikiSerializerHandler serializer) throws IOException {
        evalInline(params, content, serializer);
    }

    @Override
    public void evalInline(WikiParameters params, String content, WikiSerializerHandler serializer) throws IOException {
        serializer.writer.writeText(new TocText(content));
    }

}
