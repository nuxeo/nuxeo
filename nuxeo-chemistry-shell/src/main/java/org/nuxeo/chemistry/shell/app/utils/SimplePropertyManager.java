package org.nuxeo.chemistry.shell.app.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Document;
import org.apache.chemistry.Property;
import org.apache.chemistry.impl.simple.SimpleContentStream;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.command.CommandException;

public class SimplePropertyManager {

    protected final CMISObject item;

    public SimplePropertyManager(CMISObject item) {
        this.item = item;
    }

    public String getPropertyAsString(String name) {
        Property p = item.getProperty(name);
        if (p == null) {
            return "[null]";
        }
        Serializable val = p.getValue();
        return val != null ? val.toString() : "[null]";
    }

    public void setProperty(String name, Serializable value) throws Exception{
        item.setValue(name, value);
        item.save();
    }

    public void dumpProperties() throws IOException {
        Map<String, Property> props = item.getProperties();

        List<String> keys = new LinkedList<String>(props.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            Property prop = props.get(key);
            Object value = prop.getValue();
            Console.getDefault().println(key + " = " + (value != null ? value : "[null]"));
        }
    }

    public ContentStream getStream() throws IOException {
        return  item.getContentStream(null);
    }

    public void setStream(InputStream in, String name) throws Exception {
        if (item instanceof Document) {
            Document doc = (Document) item;
            String mimeType = MimeTypeHelper.getMimeType(name);
            ContentStream stream = new SimpleContentStream(in, mimeType, name);
            doc.setContentStream(stream);
            doc.save();
        } else {
            throw new CommandException("Target object is not a Document, can not set stream");
        }
    }

}
