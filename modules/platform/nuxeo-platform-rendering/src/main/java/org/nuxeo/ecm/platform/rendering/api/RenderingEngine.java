/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.rendering.api;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RenderingEngine {

    void setMessageBundle(ResourceBundle messages);

    ResourceBundle getMessageBundle();

    void setResourceLocator(ResourceLocator locator);

    ResourceLocator getResourceLocator();

    void setSharedVariable(String key, Object value);

    /**
     * Renders and returns the given template.
     *
     * @since 2025.14
     */
    default String render(String template, Object input) throws RenderingException {
        var writer = new StringWriter();
        render(template, input, writer);
        return writer.toString();
    }

    /**
     * Starts the rendering for the given document context.
     */
    void render(String template, Object input, Writer writer) throws RenderingException;

    /**
     * Renders and returns the given value as inline template.
     *
     * @since 2025.14
     */
    default String renderInline(String inlineTemplate, Object input) throws RenderingException {
        var writer = new StringWriter();
        renderInline(inlineTemplate, input, writer);
        return writer.toString();
    }

    /**
     * Renders the given value as inline template.
     * 
     * @since 2025.14
     */
    default void renderInline(String inlineTemplate, Object input, Writer writer) throws RenderingException {
        throw new UnsupportedOperationException("Not implemented");
    }

    View getView(String path);

    View getView(String path, Object object);

    void flushCache();

}
