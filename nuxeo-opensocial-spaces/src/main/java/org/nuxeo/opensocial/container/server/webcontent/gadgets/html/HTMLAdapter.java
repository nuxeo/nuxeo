package org.nuxeo.opensocial.container.server.webcontent.gadgets.html;

import static org.nuxeo.ecm.spaces.api.Constants.WC_HTML_HTML_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WC_HTML_HTML_TEMPLATE_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WC_HTML_HTML_TITLE_PROPERTY;

import java.io.Serializable;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.server.webcontent.abs.AbstractWebContentAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.HTMLData;

/**
 * @author Stéphane Fourrier
 */
public class HTMLAdapter extends AbstractWebContentAdapter<HTMLData> {

    public HTMLAdapter(DocumentModel doc) {
        super(doc);
    }

    public void feedFrom(HTMLData data) throws ClientException {
        super.setMetadataFrom(data);
        doc.setPropertyValue(WC_HTML_HTML_PROPERTY,
                StringEscapeUtils.escapeHtml(data.getHtml()));
        doc.setPropertyValue(WC_HTML_HTML_TITLE_PROPERTY, data.getHtmlTitle());
        doc.setPropertyValue(WC_HTML_HTML_TEMPLATE_PROPERTY, data.getTemplate());
        doc.setPropertyValue("wchtml:htmlpicturelegend",
                  data.getHtmlPictureLegend());
        doc.setPropertyValue("wchtml:htmlpicturelink",
                  data.getHtmlPictureLink());

        if (!data.getFiles().isEmpty()) {
            Serializable pictureFile = data.getFiles().get(0);
            doc.setPropertyValue("file:content", pictureFile);
        } else if(!data.hasPicture()) {
            doc.setPropertyValue("file:content", null);
        }
    }

    public HTMLData getData() throws ClientException {
        HTMLData data = new HTMLData();
        super.getMetadataFor(data);
        data.setHtml(StringEscapeUtils.unescapeHtml((String) doc.getPropertyValue(WC_HTML_HTML_PROPERTY)));
        data.setHtmlTitle((String) doc.getPropertyValue(WC_HTML_HTML_TITLE_PROPERTY));
        data.setHtmlPictureLegend((String) doc.getPropertyValue("wchtml:htmlpicturelegend"));
        data.setHtmlPictureLink((String) doc.getPropertyValue("wchtml:htmlpicturelink"));
        data.setTemplate((String) doc.getPropertyValue("wchtml:template"));

        if (doc.getPropertyValue("file:content") == null) {
            data.setHasPicture(false);
        } else {
            data.setHasPicture(true);
        }
        return data;
    }

}
