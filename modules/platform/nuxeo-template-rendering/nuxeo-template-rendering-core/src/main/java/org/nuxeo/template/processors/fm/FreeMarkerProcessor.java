/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.processors.fm;

import static java.util.Objects.requireNonNullElse;
import static org.nuxeo.ecm.core.api.impl.blob.AbstractBlob.UTF_8;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.fm.FMContextBuilder;
import org.nuxeo.template.fm.FreeMarkerVariableExtractor;
import org.nuxeo.template.processors.AbstractTemplateProcessor;

import freemarker.cache.StringTemplateLoader;

public class FreeMarkerProcessor extends AbstractTemplateProcessor implements TemplateProcessor {

    /**
     * Property to configure the maximum content size (in bytes) used for MIME type detection.
     * <p>
     * Accepts size notation like "8KiB", "1MB", etc. using {@link ByteSize#parse(String)}.
     *
     * @since 2025.17
     */
    protected static final String GUESS_MIMETYPE_MAX_SIZE_PROP = "nuxeo.freemarker.processor.mimetype.max.size";

    /**
     * Default maximum content size for MIME type detection (8 KiB).
     *
     * @since 2025.17
     */
    protected static final String DEFAULT_GUESS_MIMETYPE_MAX_SIZE = "8KiB";

    protected StringTemplateLoader loader = new StringTemplateLoader();

    protected FreemarkerEngine fmEngine = null;

    protected FMContextBuilder fmContextBuilder = new FMContextBuilder();

    protected FreemarkerEngine getEngine() {
        if (fmEngine == null) {
            fmEngine = new FreemarkerEngine();
            fmEngine.getConfiguration().setTemplateLoader(loader);
        }
        return fmEngine;
    }

    protected final static Pattern XMLStartPattern = Pattern.compile("^\\s*<\\?xml");

    /**
     * Pattern to detect HTML content by matching opening and closing tag pairs.
     * <p>
     * This pattern is deprecated because it requires finding complete tag pairs, which may not work reliably with
     * truncated content where closing tags might be cut off.
     *
     * @deprecated since 2025.17, use {@link #HtmlStartPattern} instead
     */
    @Deprecated(since = "2025.17", forRemoval = true)
    protected final static Pattern HtmlTagPattern = Pattern.compile("<(\\S+?)(.*?)>(.*?)</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Pattern to detect HTML content by looking for HTML start tag or DOCTYPE declaration at the beginning of the
     * content.
     * <p>
     * Anchored to the start of the content (optionally allowing leading whitespace) to avoid false positives from
     * {@code <html>} occurrences in the middle of the content. Uses prefix-based matching to work reliably with
     * truncated content, avoiding the need to find matching opening/closing tag pairs which may be split by truncation.
     *
     * @since 2025.17
     */
    protected final static Pattern HtmlStartPattern = Pattern.compile("^\\s*(?:<\\s*html\\b|<!doctype\\s+html)",
            Pattern.CASE_INSENSITIVE);

    protected String guessMimeType(Blob result, MimetypeRegistry mreg) {
        var content = getTruncatedContent(result);
        if (content == null) {
            return null;
        }

        if (XMLStartPattern.matcher(content).lookingAt()) {
            return "text/xml";
        }

        if (HtmlStartPattern.matcher(content).lookingAt()) {
            return "text/html";
        }

        return mreg.getMimetypeFromBlobWithDefault(result, "text/plain");
    }

    /**
     * Extracts content from the blob for MIME type detection, limiting it to a configurable maximum size to avoid
     * performance issues with large files.
     * <p>
     * The content is read from the blob's input stream and limited to the configured maximum size using
     * {@link BoundedInputStream}. This prevents loading entire large files into memory when only a small portion is
     * needed for MIME type pattern matching.
     * <p>
     * The blob's encoding is used for reading the content, defaulting to UTF-8 if no encoding is specified.
     * <p>
     * If the configured maximum size is unlimited ({@code -1}), the entire content will be read without truncation.
     *
     * @param result the blob to extract content from
     * @return the content string, truncated if the blob is larger than the configured maximum size, or {@code null} if
     *         the blob is {@code null}
     * @since 2025.17
     */
    protected String getTruncatedContent(Blob result) {
        if (result == null) {
            return null;
        }

        var maxSize = ByteSize.parse(
                Framework.getProperty(GUESS_MIMETYPE_MAX_SIZE_PROP, DEFAULT_GUESS_MIMETYPE_MAX_SIZE));
        try (var is = result.getStream()) {
            var encoding = requireNonNullElse(result.getEncoding(), UTF_8);
            // If unlimited, read entire content without truncation
            if (maxSize == ByteSize.unlimited()) {
                return IOUtils.toString(is, encoding);
            }
            // Otherwise, use BoundedInputStream to truncate
            try (var bounded = BoundedInputStream.builder().setInputStream(is).setMaxCount(maxSize.toBytes()).get()) {
                return IOUtils.toString(bounded, encoding);
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected void setBlobAttributes(Blob result, TemplateBasedDocument templateBasedDocument) {

        // try to guess mimetype and extension of the resulting Blob

        MimetypeRegistry mreg = Framework.getService(MimetypeRegistry.class);

        String mimetype = "text/html";
        String extension = ".html";

        if (mreg != null) {
            String found_mimetype = guessMimeType(result, mreg);
            if (found_mimetype != null) {
                mimetype = found_mimetype;
                List<String> extensions = mreg.getExtensionsFromMimetypeName(mimetype);
                if (extensions != null && extensions.size() > 0) {
                    extension = "." + extensions.get(0);
                }
            }
        }
        if ("text/xml".equalsIgnoreCase(mimetype)) {
            // because MimetypeRegistry return a stupid result for XML
            extension = ".xml";
        }
        result.setMimeType(mimetype);
        String targetFileName = FileUtils.getFileNameNoExt(templateBasedDocument.getAdaptedDoc().getTitle());
        result.setFilename(targetFileName + extension);
    }

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument, String templateName) throws IOException {

        Blob sourceTemplateBlob = getSourceTemplateBlob(templateBasedDocument, templateName);

        String fmTemplateKey = "main" + System.currentTimeMillis();

        String ftl = sourceTemplateBlob.getString();

        loader.putTemplate(fmTemplateKey, ftl);

        Map<String, Object> ctx = fmContextBuilder.build(templateBasedDocument, templateName);

        FMBindingResolver resolver = new FMBindingResolver();
        resolver.resolve(templateBasedDocument.getParams(templateName), ctx, templateBasedDocument);

        StringWriter writer = new StringWriter();
        try {
            getEngine().render(fmTemplateKey, ctx, writer);
        } catch (RenderingException e) {
            throw new IOException(e);
        }

        Blob result = Blobs.createBlob(writer.toString());
        setBlobAttributes(result, templateBasedDocument);

        return result;
    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) throws IOException {
        List<TemplateInput> params = new ArrayList<>();

        if (blob != null) {
            String xmlContent = blob.getString();

            if (xmlContent != null) {
                List<String> vars = FreeMarkerVariableExtractor.extractVariables(xmlContent);

                for (String var : vars) {
                    TemplateInput input = new TemplateInput(var);
                    params.add(input);
                }
            }
        }
        return params;
    }

}
