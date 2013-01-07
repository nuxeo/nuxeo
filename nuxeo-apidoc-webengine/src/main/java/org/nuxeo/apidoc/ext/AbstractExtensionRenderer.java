package org.nuxeo.apidoc.ext;

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.nuxeo.apidoc.api.ExtensionInfo;

public abstract class AbstractExtensionRenderer implements ExtensionRenderer {

    protected final ExtensionInfo ei;

    public AbstractExtensionRenderer(ExtensionInfo ei) {
        this.ei = ei;
    }

    public abstract Object getRenderObject(Document xmlContrib);

    @Override
    public Object getRenderObjectByIndex(int idx) {
        return getRenderObject(ei.getContributionItems().get(idx).getRawXml());
    }

    @Override
    public Object getRenderObject(String xml) {
        SAXReader reader = new SAXReader();
        try {
            Document xmlContrib = reader.read(new StringReader(xml));
            return getRenderObject(xmlContrib);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return null;
    }

}
