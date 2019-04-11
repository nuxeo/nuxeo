/*
 * (C) Copyright 2008 JBoss and others.
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
 *     Original file from org.jboss.seam.rss.ui.UIEntry in jboss-seam-rss
 *     Thierry Martins
 */
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.faces.context.FacesContext;

import org.jboss.seam.contexts.Contexts;

import yarfraw.core.datamodel.ChannelFeed;
import yarfraw.core.datamodel.FeedFormat;
import yarfraw.core.datamodel.ItemEntry;
import yarfraw.core.datamodel.Person;
import yarfraw.core.datamodel.Text;

/**
 * Override to support date formatting for RSS 2.0.
 * <p>
 * Default component only deals with ATOM format.
 *
 * @since 5.6
 */
public class UIEntry extends org.jboss.seam.rss.ui.UIEntry {

    private static final String COMPONENT_TYPE = UIEntry.class.getName();

    protected static final String RSS20_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";

    private FeedFormat feedFormat;

    @Override
    public String getFamily() {
        return COMPONENT_TYPE;
    }

    private Text makeText(String textString) {
        Text text = new Text(getTextFormat());
        text.setText(textString);
        return text;
    }

    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        ChannelFeed channelFeed = (ChannelFeed) Contexts.getEventContext().get(FEED_IMPL_KEY);

        ItemEntry itemEntry = new ItemEntry();
        itemEntry.setUid(getUid());
        itemEntry.setTitle(makeText(getTitle()));
        itemEntry.addLink(getLink());
        String author = getAuthor();
        if (author != null) {
            Person authorPerson = new Person();
            authorPerson.setName(author);
            itemEntry.addAuthorOrCreator(authorPerson);
        }
        itemEntry.setDescriptionOrSummary(makeText(getSummary()));
        if (getUpdated() != null) {
            itemEntry.setUpdatedDate(getUpdated(), new SimpleDateFormat(getFeedDateFormat()));
        }
        if (getPublished() != null) {
            itemEntry.setPubDate(getPublished(), new SimpleDateFormat(getFeedDateFormat()));
        }

        channelFeed.addItem(itemEntry);
    }

    public FeedFormat getFeedFormat() {
        return (FeedFormat) valueOf("feedFormat", feedFormat);
    }

    public void setFeedFormat(FeedFormat feedFormat) {
        this.feedFormat = feedFormat;
    }

    public String getFeedDateFormat() {
        if (FeedFormat.RSS20.equals(getFeedFormat())) {
            return RSS20_DATE_FORMAT;
        } else {
            return ATOM_DATE_FORMAT;
        }
    }
}
