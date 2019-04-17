/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.wro.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.service.ThemeStylingService;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.SupportedResourceType;

/**
 * Wro processor to replace flavor variables inside linked CSS file.
 *
 * @since 7.3
 */
@SupportedResourceType(ResourceType.CSS)
public class FlavorResourceProcessor extends AbstractFlavorProcessor {

    private static final Log log = LogFactory.getLog(FlavorResourceProcessor.class);

    public static final String ALIAS = "flavor";

    @Override
    protected void process(final Resource resource, final Reader reader, final Writer writer, String flavorName)
            throws IOException {
        try (ReaderInputStream ris = new ReaderInputStream(reader, getEncoding());
                InputStream is = new ProxyInputStream(ris) {
                };
                OutputStream os = new ProxyOutputStream(new WriterOutputStream(writer, getEncoding()))) {
            Map<String, String> presets = null;
            if (flavorName != null) {
                ThemeStylingService s = Framework.getService(ThemeStylingService.class);
                presets = s.getPresetVariables(flavorName);
            }
            if (presets == null || presets.isEmpty()) {
                IOUtils.copy(is, os);
            } else {
                String content = IOUtils.toString(reader);
                for (Map.Entry<String, String> preset : presets.entrySet()) {
                    content = Pattern.compile("\"" + preset.getKey() + "\"", Pattern.LITERAL)
                                     .matcher(content)
                                     .replaceAll(Matcher.quoteReplacement(preset.getValue()));
                }
                writer.write(content);
                writer.flush();
            }
            is.close();
        } catch (final Exception e) {
            log.error("Error while serving resource " + resource.getUri(), e);
            throw WroRuntimeException.wrap(e);
        }
    }

    @Override
    public String getAlias() {
        return ALIAS;
    }

}
