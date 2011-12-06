/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bchaffangeon
 *
 */

package org.nuxeo.ecm.platform.syndication.serializer;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.syndication.workflow.DashBoardItem;
import org.nuxeo.ecm.platform.syndication.workflow.TaskModule;
import org.nuxeo.ecm.platform.syndication.workflow.TaskModuleImpl;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Response;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * @author bchaffangeon
 *
 */
public class ATOMSerializer extends AbstractSyndicationSerializer implements
        DashBoardItemSerializer {

    private static final String ATOM_TYPE_old = "atom_0.3";
    private static final String ATOM_TYPE = "atom_1.0";

    @Override
    public String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req) {
        setSyndicationFormat(ATOM_TYPE);
        return super.serialize(summary, docList, columnsDefinition, req);
    }

    public void serialize(ResultSummary summary, List<DashBoardItem> workItems,
            String columnsDefinition, List<String> labels,
            String lang, Response response, HttpServletRequest req)
            throws ClientException {
        // TODO labels, lang

        SyndFeed atomFeed = new SyndFeedImpl();
        atomFeed.setFeedType(ATOM_TYPE);

        // XXX TODO : To be translated
        atomFeed.setTitle(summary.getTitle());

        atomFeed.setLink(summary.getLink());

        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        for (DashBoardItem item : workItems) {
            entries.add(adaptDashBoardItem(item, req));
        }

        atomFeed.setEntries(entries);

        SyndFeedOutput output = new SyndFeedOutput();

        // Try to return feed
        try {
            response.setEntity(output.outputString(atomFeed),
                    MediaType.TEXT_XML);
            response.getEntity().setCharacterSet(CharacterSet.UTF_8);
        } catch (FeedException fe) {
        }
    }

    /**
     * Adapts a DashboardItems to a SyndEntry.
     */
    private static SyndEntry adaptDashBoardItem(DashBoardItem item, HttpServletRequest req)
            throws ClientException {

        SyndEntry entry = new SyndEntryImpl();
        TaskModule taskModule = new TaskModuleImpl();

        taskModule.setDirective(item.getDirective());
        taskModule.setDueDate(item.getDueDate());
        taskModule.setStartDate(item.getStartDate());
        taskModule.setDescription(item.getDescription());
        taskModule.setName(item.getName());
        taskModule.setComment(item.getComment());

        List<TaskModule> modules = new ArrayList<TaskModule>();
        modules.add(taskModule);
        entry.setModules(modules);

        String docTitle = (String) item.getDocument().getProperty("dublincore",
                "title");
        entry.setTitle(docTitle);

        entry.setLink(DocumentModelFunctions.documentUrl(null,
                item.getDocument(), null, null, true,req));

        return entry;
    }

}
