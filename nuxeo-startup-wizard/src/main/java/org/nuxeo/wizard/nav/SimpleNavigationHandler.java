/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.wizard.nav;

import java.util.ArrayList;
import java.util.List;

/**
 * Very basic Navigation handler
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.1
 */
public class SimpleNavigationHandler {

    // I am too lazy to load a file
    protected static final String[] nav = { " |welcome.jsp",
            "General|generalSettings.jsp", "Proxy|proxySettings.jsp",
            "DB|dbSettings.jsp", "Smtp|smtpSettings.jsp",
            "Connect|connectForm.jsp", "ConnectFinish|connectFinish.jsp",
            "Recap|recapScreen.jsp", "Restart|reStarting.jsp" };

    protected List<Page> pages = new ArrayList<Page>();

    protected static SimpleNavigationHandler instance;

    public static SimpleNavigationHandler instance() {
        if (instance == null) {
            instance = new SimpleNavigationHandler();
        }
        return instance;
    }

    protected SimpleNavigationHandler() {

        Page previousPage = null;
        for (int idx = 0; idx < nav.length; idx++) {
            String token = nav[idx];
            String[] parts = token.split("\\|");
            Page page = new Page(parts[0].trim(), parts[1].trim());
            pages.add(page);

            if (previousPage != null) {
                previousPage.next = page;
                page.prev = previousPage;
            }

            page.progress = new Double((idx + 1) * (100.0 / nav.length)).intValue();
            previousPage = page;

        }
    }

    public Page getCurrentPage(String action) {

        if (action == null || action.isEmpty()) {
            return pages.get(0);
        }

        Page currentPage = null;
        for (int idx = 0; idx < pages.size(); idx++) {
            if (pages.get(idx).getAction().equals(action)) {
                currentPage = pages.get(idx);
                break;
            }
        }
        return currentPage;
    }

    public List<Page> getPages() {
        return pages;
    }

}
