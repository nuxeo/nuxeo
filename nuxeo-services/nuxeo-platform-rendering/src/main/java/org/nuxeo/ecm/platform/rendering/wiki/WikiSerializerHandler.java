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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.WikiBlockWriter;
import org.wikimodel.wem.PrintListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiParameters;

import freemarker.core.Environment;
import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WikiSerializerHandler extends PrintListener {

    public static final Log log = LogFactory.getLog(WikiSerializerHandler.class);

    protected static final String LINE_SEP = System.getProperty("line.separator");

    protected final WikiSerializer engine;

    protected final StringBuilder words = new StringBuilder();

    protected Environment env;

    protected WikiWriter writer;

    protected int mark = -1; // used to mark the current buffer to be able to retrieve printed text that starts at the
                             // mark

    protected Toc toc;

    public WikiSerializerHandler(WikiSerializer engine) {
        super(null); // cannot base on the wikiprinter - so we don't use it
        this.engine = engine;
        writer = new WikiWriter();
        if (engine.macros.containsKey("toc")) {
            toc = new Toc();
        }
    }

    @Override
    protected void print(String str) {
        writer.print(str);
    }

    @Override
    protected void println() {
        writer.println();
    }

    @Override
    protected void println(String str) {
        writer.println(str);
    }

    public WikiWriter getWriter() {
        return writer;
    }

    public Environment getEnvironment() {
        if (env == null) {
            env = Environment.getCurrentEnvironment();
        }
        return env;
    }

    protected void beginElement() {
        flushWords();
    }

    protected void endElement() {
        flushWords();
    }

    protected void flushWords() {
        if (words.length() == 0) {
            return;
        }
        String text = words.toString();
        words.setLength(0);
        for (int i = 0, len = engine.filters.size(); i < len; i++) {
            String result = engine.filters.get(i).apply(text);
            if (result != null) {
                print(result);
                return;
            }
        }
        print(text);
    }

    @Override
    public void beginDefinitionDescription() {
        beginElement();
        super.beginDefinitionDescription();
    }

    @Override
    public void beginDefinitionList(WikiParameters parameters) {
        beginElement();
        super.beginDefinitionList(parameters);
    }

    @Override
    public void beginDefinitionTerm() {
        beginElement();
        super.beginDefinitionTerm();
    }

    @Override
    public void beginDocument() {
        beginElement();
        super.beginDocument();
    }

    @Override
    public void beginFormat(WikiFormat format) {
        beginElement();
        super.beginFormat(format);
    }

    @Override
    public void beginHeader(int level, WikiParameters params) {
        beginElement();
        super.beginHeader(level, params);
        if (toc != null) {
            String id = toc.addHeading(null, level); // we don't know the title yet
            print("<a name=\"heading_" + id + "\">");
            mark = writer.getBuffer().length();
        }
    }

    @Override
    public void beginInfoBlock(char infoType, WikiParameters params) {
        beginElement();
        super.beginInfoBlock(infoType, params);
    }

    @Override
    public void beginList(WikiParameters parameters, boolean ordered) {
        beginElement();
        super.beginList(parameters, ordered);
    }

    @Override
    public void beginListItem() {
        beginElement();
        super.beginListItem();
    }

    @Override
    public void beginParagraph(WikiParameters params) {
        beginElement();
        super.beginParagraph(params);
    }

    @Override
    public void beginPropertyBlock(String propertyUri, boolean doc) {
        beginElement();
        if (propertyUri.startsWith("block:")) {
            String name = propertyUri.substring(6);
            WikiBlockWriter bwriter = new WikiBlockWriter(writer, name);
            writer.writeText(bwriter);
            writer = bwriter;
        } else {
            super.beginPropertyBlock(propertyUri, doc);
        }
    }

    @Override
    public void beginPropertyInline(String str) {
        beginElement();
        super.beginPropertyInline(str);
    }

    @Override
    public void beginQuotation(WikiParameters params) {
        beginElement();
        super.beginQuotation(params);
    }

    @Override
    public void beginQuotationLine() {
        beginElement();
        super.beginQuotationLine();
    }

    @Override
    public void beginTable(WikiParameters params) {
        beginElement();
        super.beginTable(params);
    }

    @Override
    public void beginTableCell(boolean tableHead, WikiParameters params) {
        beginElement();
        super.beginTableCell(tableHead, params);
    }

    @Override
    public void beginTableRow(WikiParameters params) {
        beginElement();
        super.beginTableRow(params);
    }

    @Override
    public void endDefinitionDescription() {
        endElement();
        super.endDefinitionDescription();
    }

    @Override
    public void endDefinitionList(WikiParameters parameters) {
        endElement();
        super.endDefinitionList(parameters);
    }

    @Override
    public void endDefinitionTerm() {
        endElement();
        super.endDefinitionTerm();
    }

    @Override
    public void endDocument() {
        endElement();
        super.endDocument();
    }

    @Override
    public void endFormat(WikiFormat format) {
        endElement();
        super.endFormat(format);
    }

    @Override
    public void endHeader(int level, WikiParameters params) {
        if (toc != null) {
            if (mark == -1) {
                throw new IllegalStateException("marker was not set");
            }
            toc.tail.title = writer.getBuffer().substring(mark);
            mark = -1;
            print("</a>");
            super.endHeader(level, params);
        } else {
            super.endHeader(level, params);
        }
        endElement();
    }

    @Override
    public void endInfoBlock(char infoType, WikiParameters params) {
        endElement();
        super.endInfoBlock(infoType, params);
    }

    @Override
    public void endList(WikiParameters parameters, boolean ordered) {
        endElement();
        super.endList(parameters, ordered);
    }

    @Override
    public void endListItem() {
        endElement();
        super.endListItem();
    }

    @Override
    public void endParagraph(WikiParameters params) {
        endElement();
        super.endParagraph(params);
    }

    @Override
    public void endPropertyBlock(String propertyUri, boolean doc) {
        endElement();
        if (propertyUri.startsWith("block:")) {
            writer = writer.getParent();
            if (writer == null) {
                throw new IllegalStateException("block macro underflow");
            }
        } else {
            super.endPropertyBlock(propertyUri, doc);
        }
    }

    @Override
    public void endPropertyInline(String inlineProperty) {
        endElement();
        super.endPropertyInline(inlineProperty);
    }

    @Override
    public void endQuotation(WikiParameters params) {
        endElement();
        super.endQuotation(params);
    }

    @Override
    public void endQuotationLine() {
        endElement();
        super.endQuotationLine();
    }

    @Override
    public void endTable(WikiParameters params) {
        endElement();
        super.endTable(params);
    }

    @Override
    public void endTableCell(boolean tableHead, WikiParameters params) {
        endElement();
        super.endTableCell(tableHead, params);
    }

    @Override
    public void endTableRow(WikiParameters params) {
        endElement();
        super.endTableRow(params);
    }

    @Override
    public void onEmptyLines(int count) {
        flushWords();
        super.onEmptyLines(count);
    }

    @Override
    public void onHorizontalLine() {
        flushWords();
        super.onHorizontalLine();
    }

    @Override
    public void onLineBreak() {
        flushWords();
        super.onLineBreak();
    }

    @Override
    public void onReference(String ref, boolean explicitLink) {
        flushWords();
        super.onReference(ref, explicitLink);
    }

    @Override
    public void onTableCaption(String str) {
        flushWords();
        super.onTableCaption(str);
    }

    @Override
    public void onVerbatimBlock(String str) {
        flushWords();
        super.onVerbatimBlock(str);
    }

    @Override
    public void onVerbatimInline(String str) {
        flushWords();
        super.onVerbatimInline(str);
    }

    @Override
    public void onMacroBlock(String macroName, WikiParameters params, String content) {
        flushWords();
        WikiMacro expression = engine.macros.get(macroName);
        if (expression != null) {
            try {
                expression.eval(params, content, this);
            } catch (IOException | TemplateException e) {
                log.error("Failed to eval macro", e);
            }
        } else {
            log.warn("Unknown wiki macro: " + macroName);
        }
    }

    @Override
    public void onMacroInline(String macroName, WikiParameters params, String content) {
        flushWords();
        WikiMacro expression = engine.macros.get(macroName);
        if (expression != null) {
            try {
                expression.evalInline(params, content, this);
            } catch (IOException | TemplateException e) {
                log.error("Failed to eval macro", e);
            }
        } else {
            log.warn("Unknown wiki macro: " + macroName);
        }
    }

    @Override
    public void onExtensionBlock(String extensionName, WikiParameters params) {
        flushWords();
        log.warn("Unknown wiki expression: " + extensionName);
    }

    @Override
    public void onExtensionInline(String extensionName, WikiParameters params) {
        flushWords();
        log.warn("Unknown wiki expression: " + extensionName);
    }

    @Override
    public void onSpecialSymbol(String str) {
        String entity = getSymbolEntity(str);
        if (entity != null) {
            words.append(entity);
        } else { // do not escape - to be able to use filters on it
            words.append(str);
        }
    }

    @Override
    public void onSpace(String str) {
        flushWords();
        super.onSpace(str);
    }

    @Override
    public void onNewLine() {
        flushWords();
        super.onNewLine();
    }

    @Override
    public void onEscape(String str) {
        flushWords();
        super.onEscape(str);
    }

    @Override
    public void onWord(String word) {
        words.append(word);
        // writeWord(word);
    }

    protected void writeWord(String word) {
        for (int i = 0, len = engine.filters.size(); i < len; i++) {
            String result = engine.filters.get(i).apply(word);
            if (result != null) {
                print(result);
                return;
            }
        }
        print(word);
    }

    /**
     * Returns an HTML/XML entity corresponding to the specified special symbol. Depending on implementation it can be
     * real entities (like &amp;amp; &amp;lt; &amp;gt; or the corresponding digital codes (like &amp;#38;,
     * &amp;#&amp;#38; or &amp;#8250;). Digital entity representation is better for generation of XML files.
     *
     * @param str the special string to convert to an HTML/XML entity
     * @return an HTML/XML entity corresponding to the specified special symbol.
     */
    protected String getSymbolEntity(String str) {
        String entity = null;
        if (isHtmlEntities()) {
            entity = WikiEntityUtil.getHtmlSymbol(str);
        } else {
            int code = WikiEntityUtil.getHtmlCodeByWikiSymbol(str);
            if (code > 0) {
                entity = "#" + Integer.toString(code);
            }
        }
        if (entity != null) {
            entity = "&" + entity + ";";
            if (str.startsWith(" --")) {
                entity = "&nbsp;" + entity + " ";
            }
        }
        return entity;
    }

    /**
     * Returns <code>true</code> if special Wiki entities should be represented as the corresponding HTML entities or
     * they should be visualized using the corresponding XHTML codes (like &amp;amp; and so on). This method can be
     * overloaded in subclasses to re-define the visualization style.
     *
     * @return <code>true</code> if special Wiki entities should be represented as the corresponding HTML entities or
     *         they should be visualized using the corresponding XHTML codes (like &amp;amp; and so on).
     */
    protected boolean isHtmlEntities() {
        return true;
    }

}
