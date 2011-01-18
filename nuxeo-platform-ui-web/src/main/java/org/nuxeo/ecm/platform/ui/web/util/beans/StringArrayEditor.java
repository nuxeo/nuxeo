package org.nuxeo.ecm.platform.ui.web.util.beans;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringArrayEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(text, ",");
        List<String> list = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        String[] value = new String[list.size()];
        setValue(list.toArray(value));
    }

    @Override
    public String getAsText() {
        String sep = "";
        String text = "";
        for (String element:(String[])getValue()) {
            text += sep + element;
            sep = ", ";
        }
        return text;
    }
}
