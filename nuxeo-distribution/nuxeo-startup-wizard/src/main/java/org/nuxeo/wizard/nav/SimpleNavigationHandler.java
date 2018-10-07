/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.wizard.nav;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Very basic Navigation handler
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class SimpleNavigationHandler {

    public static final String SKIP_PAGES_KEY = "nuxeo.wizard.skippedpages";

    // I am too lazy to load a file
    // navCode / jsp Page / active flag / hidden flag
    protected static final String[] nav = { "Home|welcome.jsp|1|0", "NetworkBlocked|networkBlocked.jsp|0|0",
            "General|generalSettings.jsp|1|0", "Proxy|proxySettings.jsp|1|0", "DB|dbSettings.jsp|1|0",
            "User|userSettings.jsp|1|0", "Smtp|smtpSettings.jsp|1|0", "Connect|connectForm.jsp|1|0",
            "ConnectCallback|connectCallback.jsp|0|1", "ConnectFinish|connectFinish.jsp|0|0",
            "PackagesSelection|packagesSelection.jsp|1|0", "PackagesDownload|packagesDownload.jsp|1|0",
            "PackagesSelectionDone|packagesSelectionDone.jsp|0|0", "Recap|recapScreen.jsp|1|0",
            "Restart|reStarting.jsp|1|1", "Reset|Welcome.jsp|1|1", "PackageOptionsResource||1|1" };

    protected List<Page> pages = new ArrayList<>();

    protected static SimpleNavigationHandler instance;

    protected static Log log = LogFactory.getLog(SimpleNavigationHandler.class);

    public static SimpleNavigationHandler instance() {
        if (instance == null) {
            instance = new SimpleNavigationHandler();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    protected SimpleNavigationHandler() {

        Page previousPage = null;
        for (int idx = 0; idx < nav.length; idx++) {
            String token = nav[idx];

            Page page = new Page(token);
            pages.add(page);

            if (previousPage != null) {
                previousPage.next = page;
                page.prev = previousPage;
            }

            // XXX false !
            page.progress = (int) ((idx + 1) * (100.0 / nav.length));
            previousPage = page;

        }

        ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();
        configurationGenerator.init();
        String skipPages = configurationGenerator.getUserConfig().getProperty(SKIP_PAGES_KEY, null);
        if (skipPages != null) {
            String[] pages2Skip = skipPages.split(",");
            for (String pageKey : pages2Skip) {
                deactivatePage(pageKey);
            }
        }
    }

    public Page getDefaultPage() {
        return getCurrentPage(pages.get(0).action);
    }

    public int getProgress(String action) {

        int activePageIdx = 0;
        int totalActivePages = 0;

        for (int idx = 0; idx < pages.size(); idx++) {

            if (pages.get(idx).isVisibleInNavigationMenu()) {
                totalActivePages += 1;
            }
            if (pages.get(idx).getAction().equals(action)) {
                activePageIdx = totalActivePages;
            }
        }
        if (totalActivePages == 0) {
            return 0;
        }
        return (int) ((activePageIdx) * (100.0 / totalActivePages));
    }

    public Page getCurrentPage(String action) {

        Page currentPage = null;

        if (action == null || action.isEmpty()) {
            currentPage = pages.get(0);
        } else {
            currentPage = findPageByAction(action);
        }

        if (currentPage == null) {
            log.warn("No Page found for action " + action);
            return null;
        }

        // mark as navigated
        currentPage.navigated = true;

        return currentPage;
    }

    public Page findPageByAction(String action) {
        for (int idx = 0; idx < pages.size(); idx++) {
            if (pages.get(idx).getAction().equals(action)) {
                return pages.get(idx);
            }
        }
        return null;
    }

    public void activatePage(String action) {
        Page page = findPageByAction(action);
        if (page != null) {
            page.active = true;
        }
    }

    public void deactivatePage(String action) {
        Page page = findPageByAction(action);
        if (page != null) {
            page.active = false;
        }
    }

    public List<Page> getPages() {
        return pages;
    }

}
