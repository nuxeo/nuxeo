/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.webengine.blogs.fragments;

import static org.nuxeo.webengine.blogs.utils.BlogConstants.BLOG_POST_DOC_TYPE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_CONTENT;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.blogs.models.BlogPostListModel;
import org.nuxeo.webengine.blogs.models.BlogPostModel;
import org.nuxeo.webengine.blogs.models.BlogSiteArchiveDayModel;
import org.nuxeo.webengine.sites.utils.SiteQueriesCollection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to retrieving a certain
 * number of blog posts with information about the last <b>BlogPost</b>-s that
 * are made under an <b>BlogSite</b>.
 *
 * @author rux
 */
public class RecentBlogPostsFragment extends AbstractFragment {

    public static final int noForBlogSite = 15;
    public static final int noForBlogPost = 5;

    private static String[] weekDays;

    /**
     * Retrieves a certain number of blog posts with information about the last
     * <b>BlogPost</b>-s that are made under an <b>BlogSite</b> that is received
     * as parameter.
     */
    @Override
    public Model getModel() throws ModelException {
        BlogPostListModel model = new BlogPostListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            SimpleDateFormat simpleMonthFormat = new SimpleDateFormat(
                    "dd MMMM yyyy", WebEngine.getActiveContext().getLocale());
            try {
                DocumentModel blogSite = SiteUtils.getFirstWebSiteParent(
                        session, documentModel);
                DocumentModelList blogPosts = SiteQueriesCollection.queryLastModifiedPages(
                        session,
                        blogSite.getPathAsString(),
                        BLOG_POST_DOC_TYPE,
                        BLOG_POST_DOC_TYPE.equals(documentModel.getType()) ? noForBlogPost
                                : noForBlogSite);

                for (DocumentModel blogPost : blogPosts) {
                    String title = SiteUtils.getString(blogPost, "dc:title");
                    String path = SiteUtils.getPagePath(blogSite, blogPost);

                    String description = SiteUtils.getString(blogPost,
                            "dc:description");

                    String content = SiteUtils.getFistNWordsFromString(
                            SiteUtils.getString(blogPost, WEBPAGE_CONTENT),
                            Integer.MAX_VALUE);
                    String author = SiteUtils.getString(blogPost, "dc:creator");

                    GregorianCalendar creationDate = SiteUtils.getGregorianCalendar(blogPost,
                            "dc:created");

                    String day = getWeekDay(creationDate.get(Calendar.DAY_OF_WEEK));
                    BlogSiteArchiveDayModel dayModel = getYearModel(model, day);
                    if (dayModel == null) {
                        dayModel = new BlogSiteArchiveDayModel(
                                day,
                                simpleMonthFormat.format(creationDate.getTime()),
                                0);
                        model.addItem(dayModel);
                    }
                    dayModel.increaseCount();

                    BlogPostModel blogPostModel = new BlogPostModel(title, path, description,
                            content, author);
                    dayModel.addItem(blogPostModel);
                }
            } catch (Exception e) {
                throw new ModelException(e);
            }

        }
        return model;
    }

    /**
     * Utility method used to return the day of the week as a string
     * representation.
     *
     * @param day day of the week as integer
     * @return day of the week as string
     */
    private static String getWeekDay(int day) {
        if (weekDays == null) {
            DateFormatSymbols dfs = new DateFormatSymbols(
                    WebEngine.getActiveContext().getLocale());
            weekDays = dfs.getWeekdays();
        }
        return weekDays[day];
    }

    /**
     * Returns the model corresponding to the day received as parameter.
     *
     * @param model the model list in which the day model will be searched
     * @param day the name of the day
     * @return the model corresponding to the day received as parameter.
     */
    private static BlogSiteArchiveDayModel getYearModel(Model model, String day) {
        BlogSiteArchiveDayModel dayModel = null;
        for (Model item : model.getItems()) {
            if (item instanceof BlogSiteArchiveDayModel) {
                String itemDay = ((BlogSiteArchiveDayModel) item).getDay();
                if (day.equals(itemDay)) {
                    dayModel = (BlogSiteArchiveDayModel) item;
                    break;
                }
            }
        }
        return dayModel;
    }

}
