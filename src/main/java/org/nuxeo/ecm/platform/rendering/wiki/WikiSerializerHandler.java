/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.wiki;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriter;
import org.wikimodel.wem.PrintListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiPageUtil;
import org.wikimodel.wem.WikiParameters;

import freemarker.core.Environment;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiSerializerHandler extends PrintListener {
    public static final Log log = LogFactory.getLog(WikiSerializerHandler.class);

    protected static String LINE_SEP = System.getProperty("line.separator");

    protected WikiSerializer engine;
    protected StringBuilder words = new StringBuilder();
    protected RenderingContext ctx;
    protected Environment env;
    protected Writer writer;
    protected BlockWriter parentWriter; // we allow only one level of block: doc properties
    protected boolean parentSuppressOutput = false; // suppress output state saved from parent

    public WikiSerializerHandler(WikiSerializer engine, Writer writer) {
        this (engine, writer, null);
    }

    public WikiSerializerHandler(WikiSerializer engine, Writer writer, RenderingContext ctx) {
        super (null); // cannot base on the wikiprinter - so we don't use it
        this.engine = engine;
        this.ctx = ctx;
        this.writer = writer;
    }

    public void print(String str) {
        try {
            writer.write(str);
        } catch (IOException e) {
            log.error("Failed to print: "+str, e);
        }
    }

    public void println() {
        try {
            writer.write(LINE_SEP);
        } catch (IOException e) {
            log.error("Failed to print newline", e);
        }
    }

    public void println(String str) {
        try {
            writer.write(str);
            writer.write(LINE_SEP);
        } catch (IOException e) {
            log.error("Failed to print: "+str, e);
        }
    }

    public Environment getEnvironment() {
        if (env == null) {
            env = Environment.getCurrentEnvironment();
        }
        return env;
    }

    /**
     * @return the context.
     */
    public RenderingContext getContext() {
        return ctx;
    }

    protected void beginElement() {
        flushWords();
    }

    protected void endElement() {
        flushWords();
    }


    protected void flushWords() {
        if (words.length() == 0) return;
        String text = words.toString();
        words.setLength(0);
        for (int i=0, len=engine.filters.size(); i<len; i++) {
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
            if (parentWriter == null && (writer instanceof BlockWriter)) {
                String name = propertyUri.substring(6);
                parentWriter = (BlockWriter)writer;
                BlockWriter bw = new BlockWriter("__dynamic__wiki", name, parentWriter.getRegistry());
                try {
                    parentSuppressOutput = parentWriter.getSuppressOutput();
                    parentWriter.setSuppressOutput(true);
                    parentWriter.writeBlock(bw);
                    writer = bw;
                } catch (IOException e) {
                    log.error("Failed to write block", e);
                }
            } else {
                log.error("Illegal state - Rendering block ignored");
            }
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
        endElement();
        super.endHeader(level, params);
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
            if (parentWriter != null && (writer instanceof BlockWriter)) {
                parentWriter.setSuppressOutput(parentSuppressOutput);
                writer = parentWriter;
            } else {
                log.error("Illegal state exception - ignoring block end");
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
    public void onMacroBlock(String macroName, WikiParameters params,
            String content) {
        flushWords();
        WikiMacro expression = engine.macros.get(macroName);
        if (expression != null) {
            try {
                expression.eval(params, content, this);
            } catch (Exception e) {
                log.error("Failed to eval macro", e);
            }
        } else {
            log.warn("Unknown wiki macro: "+macroName);
        }
    }

    @Override
    public void onMacroInline(String macroName, WikiParameters params,
            String content) {
        flushWords();
        WikiMacro expression = engine.macros.get(macroName);
        if (expression != null) {
            try {
                expression.evalInline(params, content, this);
            } catch (Exception e) {
                log.error("Failed to eval macro", e);
            }
        } else {
            log.warn("Unknown wiki macro: "+macroName);
        }
    }

    @Override
    public void onExtensionBlock(String extensionName, WikiParameters params) {
        flushWords();
        WikiExpression expression = engine.expressions.get(extensionName);
        if (expression != null) {
            try {
                expression.eval(params, this);
            } catch (Exception e) {
                log.error("Failed to eval expression", e);
            }
        } else {
            log.warn("Unknown wiki expression: "+extensionName);
        }
    }

    @Override
    public void onExtensionInline(String extensionName, WikiParameters params) {
        flushWords();
        WikiExpression expression = engine.expressions.get(extensionName);
        if (expression != null) {
            try {
                expression.evalInline(params, this);
            } catch (Exception e) {
                log.error("Failed to eval expression", e);
            }
        } else {
            log.warn("Unknown wiki expression: "+extensionName);
        }
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


    public void onWord(String word) {
        this.words.append(word);
        //writeWord(word);
    }


    protected void writeWord(String word) {
        for (int i=0, len=engine.filters.size(); i<len; i++) {
            String result = engine.filters.get(i).apply(word);
            if (result != null) {
                print(result);
                return;
            }
        }
        print(word);
    }


    /**
     * Returns an HTML/XML entity corresponding to the specified special symbol.
     * Depending on implementation it can be real entities (like &amp;amp;
     * &amp;lt; &amp;gt; or the corresponding digital codes (like &amp;#38;,
     * &amp;#&amp;#38; or &amp;#8250;). Digital entity representation is better
     * for generation of XML files.
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
     * Returns <code>true</code> if special Wiki entities should be
     * represented as the corresponding HTML entities or they should be
     * visualized using the corresponding XHTML codes (like &amp;amp; and so
     * on). This method can be overloaded in subclasses to re-define the
     * visualization style.
     *
     * @return <code>true</code> if special Wiki entities should be
     *         represented as the corresponding HTML entities or they should be
     *         visualized using the corresponding XHTML codes (like &amp;amp;
     *         and so on).
     */
    protected boolean isHtmlEntities() {
        return true;
    }

}
