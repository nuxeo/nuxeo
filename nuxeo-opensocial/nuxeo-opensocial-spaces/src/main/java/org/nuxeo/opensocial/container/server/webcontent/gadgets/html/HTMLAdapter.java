package org.nuxeo.opensocial.container.server.webcontent.gadgets.html;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.server.webcontent.abs.AbstractWebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.HTMLData;

/**
 * @author Stéphane Fourrier
 */
public class HTMLAdapter extends AbstractWebContentAdapter implements
        WebContentAdapter<HTMLData> {

    public HTMLAdapter(DocumentModel doc) {
        super(doc);
    }

    public void feedFrom(HTMLData data) throws ClientException {
        super.setMetadataFrom(data);
        doc.setPropertyValue("wchtml:html",
                StringEscapeUtils.escapeHtml(data.getHtml()));
        doc.setPropertyValue("wchtml:htmltitle", data.getHtmlTitle());
    }

    public HTMLData getData() throws ClientException {
        HTMLData data = new HTMLData();
        super.getMetadataFor(data);
        data.setHtml(StringEscapeUtils.unescapeHtml((String) doc.getPropertyValue("wchtml:html")));
        data.setHtmlTitle((String) doc.getPropertyValue("wchtml:htmltitle"));
        return data;
    }
}
