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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.web.resources.wro.processor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.SassImport;
import org.w3c.css.sac.InputSource;

import com.vaadin.sass.internal.ScssStylesheet;
import com.vaadin.sass.internal.handler.SCSSDocumentHandlerImpl;
import com.vaadin.sass.internal.handler.SCSSErrorHandler;
import com.vaadin.sass.internal.parser.ParseException;
import com.vaadin.sass.internal.parser.Parser;
import com.vaadin.sass.internal.parser.SCSSParseException;
import com.vaadin.sass.internal.tree.Node;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.SupportedResourceType;

/**
 * Use Sass css processor to replace variables, mixin, etc. according to a given flavor.
 *
 * @since 7.4
 */
@SupportedResourceType(ResourceType.CSS)
public class SassCssFlavorProcessor extends AbstractFlavorProcessor {

    private static final Log log = LogFactory.getLog(SassCssFlavorProcessor.class);

    public static final String ALIAS = "sassCss";

    @Override
    public void process(final Resource resource, final Reader reader, final Writer writer, String flavorName)
            throws IOException {
        if (isEnabled(resource)) {
            Reader finalReader = null;
            try {
                String varContents = "";
                if (flavorName != null) {
                    ThemeStylingService s = Framework.getService(ThemeStylingService.class);
                    FlavorDescriptor fd = s.getFlavor(flavorName);
                    if (fd != null) {
                        List<SassImport> sassVars = fd.getSassImports();
                        if (sassVars != null) {
                            for (SassImport var : sassVars) {
                                varContents += var.getContent();
                            }
                        }
                    }
                }

                InputSource source = null;
                if (StringUtils.isNoneBlank(varContents)) {
                    byte[] varBytes = varContents.getBytes();
                    byte[] initalBytes = IOUtils.toByteArray(reader);
                    reader.close();
                    byte[] finalBytes = ArrayUtils.addAll(varBytes, initalBytes);
                    finalReader = new InputStreamReader(new ByteArrayInputStream(finalBytes));
                } else {
                    finalReader = reader;
                }
                source = new InputSource(finalReader);
                source.setEncoding(getEncoding());
                SCSSDocumentHandlerImpl scssDocumentHandlerImpl = new SCSSDocumentHandlerImpl();
                ScssStylesheet stylesheet = scssDocumentHandlerImpl.getStyleSheet();

                Parser parser = new Parser();
                parser.setErrorHandler(new SCSSErrorHandler());
                parser.setDocumentHandler(scssDocumentHandlerImpl);

                try {
                    parser.parseStyleSheet(source);
                } catch (ParseException e) {
                    log.error("Error while parsing resource " + resource.getUri(), e);
                    throw WroRuntimeException.wrap(new SCSSParseException(e, resource.getUri()));
                }

                stylesheet.setCharset(getEncoding());
                stylesheet.addSourceUris(Arrays.asList(resource.getUri()));

                stylesheet.compile();

                StringBuilder string = new StringBuilder("");
                String delimeter = "\n\n";
                List<Node> children = stylesheet.getChildren();
                if (children.size() > 0) {
                    string.append(ScssStylesheet.PRINT_STRATEGY.build(children.get(0)));
                }
                if (children.size() > 1) {
                    for (int i = 1; i < children.size(); i++) {
                        String childString = ScssStylesheet.PRINT_STRATEGY.build(children.get(i));
                        if (childString != null) {
                            string.append(delimeter).append(childString);
                        }
                    }
                }

                String content = string.toString();

                writer.write(content);
                writer.flush();
                if (finalReader != null) {
                    finalReader.close();
                }
            } catch (final Exception e) {
                log.error("Error while serving resource " + resource.getUri(), e);
                throw WroRuntimeException.wrap(e);
            } finally {
                IOUtils.closeQuietly(finalReader);
            }

        } else {
            IOUtils.copy(reader, writer);
        }
    }

    @Override
    public String getAlias() {
        return ALIAS;
    }

}
