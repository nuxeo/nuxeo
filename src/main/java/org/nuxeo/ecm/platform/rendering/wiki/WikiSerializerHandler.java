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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.wikimodel.wem.IWikiPrinter;
import org.wikimodel.wem.PrintListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiParameters;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiSerializerHandler extends PrintListener {
    public static final Log log = LogFactory.getLog(WikiSerializerHandler.class);

    protected WikiSerializer engine;
    protected StringBuilder word = new StringBuilder();
    protected RenderingContext ctx;

    public WikiSerializerHandler(WikiSerializer engine, IWikiPrinter printer) {
        this (engine, printer, null);
    }

    public WikiSerializerHandler(WikiSerializer engine, IWikiPrinter printer, RenderingContext ctx) {
        super (printer);
        this.engine = engine;
        this.ctx = ctx;
    }

    /**
     * @return the context.
     */
    public RenderingContext getContext() {
        return ctx;
    }

    @Override
    public void print(String str) {
        super.print(str);
    }

    @Override
    public void println(String str) {
        super.println(str);
    }

    @Override
    public void println() {
        super.println();
    }

    protected void beginElement() {
        flushWord();
    }

    protected void endElement() {
        flushWord();
    }

    protected void flushWord() {
        if (word.length() == 0) return;
        String text = word.toString();
        word.setLength(0);
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
        super.beginPropertyBlock(propertyUri, doc);
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
        super.endPropertyBlock(propertyUri, doc);
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
        flushWord();
        super.onEmptyLines(count);
    }

    @Override
    public void onHorizontalLine() {
        flushWord();
        super.onHorizontalLine();
    }

    @Override
    public void onLineBreak() {
        flushWord();
        super.onLineBreak();
    }

    @Override
    public void onReference(String ref, boolean explicitLink) {
        flushWord();
        super.onReference(ref, explicitLink);
    }

    @Override
    public void onTableCaption(String str) {
        flushWord();
        super.onTableCaption(str);
    }

    @Override
    public void onVerbatimBlock(String str) {
        flushWord();
        super.onVerbatimBlock(str);
    }

    @Override
    public void onVerbatimInline(String str) {
        flushWord();
        super.onVerbatimInline(str);
    }

    @Override
    public void onMacroBlock(String macroName, WikiParameters params,
            String content) {
        flushWord();
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
        flushWord();
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
        flushWord();
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
        flushWord();
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
        if (str.length() != 1) {
            super.onSpecialSymbol(str);
        }
        if ("-$#@~^_=+:*".indexOf(str.charAt(0)) > -1) {
            word.append(str);
        } else {
            super.onSpecialSymbol(str);
        }
    }

    @Override
    public void onSpace(String str) {
        flushWord();
        super.onSpace(str);
    }

    @Override
    public void onNewLine() {
        flushWord();
        super.onNewLine();
    }


    @Override
    public void onEscape(String str) {
        flushWord();
        //TODO
        super.onEscape(str);
    }


    public void onWord(String word) {
        this.word.append(word);
    }


}
