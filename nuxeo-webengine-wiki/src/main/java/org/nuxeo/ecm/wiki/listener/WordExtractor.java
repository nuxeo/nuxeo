package org.nuxeo.ecm.wiki.listener;

import org.wikimodel.wem.PrintListener;
import org.wikimodel.wem.WikiPrinter;

public class WordExtractor extends PrintListener{

    protected final StringBuilder words = new StringBuilder();

    StringBuffer collector;

    public WordExtractor(StringBuffer collector) {
        super(new WikiPrinter());
        this.collector = collector;
    }

    @Override
    public void onWord(String str) {
        if (collector != null ){
            collector.append(str);
        }
    }

    @Override
    public void onSpecialSymbol(String str) {
        if ( collector == null){
            return;
        }
        if  ( ".".equals(str)){
            collector.append(str);
        } else {
            collector.append(" ");
        }
    }

    @Override
    public void onSpace(String str) {
        if ( collector != null ){
            collector.append(str);
        }
    }

    @Override
    public void onEmptyLines(int count) {
        if ( collector != null ){
            collector.append(" ");
        }
    }

    @Override
    public void onNewLine() {
        if ( collector != null ){
            collector.append(" ");
        }
    }


}
