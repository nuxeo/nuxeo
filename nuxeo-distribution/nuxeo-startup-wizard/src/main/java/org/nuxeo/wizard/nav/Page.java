/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 *
 */

package org.nuxeo.wizard.nav;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.wizard.context.Context;

/**
 * Represents a Page
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class Page {

    public static final String NUXEO_COM_URL = "http://www.nuxeo.com/embedded/wizard/";

    protected final String action;

    protected final String jsp;

    protected int progress = -1;

    protected Page prev;

    protected Page next;

    protected boolean active = true;

    protected boolean hidden = false;

    protected boolean navigated = false;

    public Page(String pageConfigString) {

        String[] parts = pageConfigString.split("\\|");

        action = parts[0];
        String jspPage = parts[1];
        if (!jspPage.startsWith("/")) {
            jspPage = "/" + jspPage;
        }
        jsp = jspPage;
        if ("0".equals(parts[2])) {
            active = false;
        }
        if ("1".equals(parts[3])) {
            hidden = true;
        }
    }

    public String getAction() {
        return action;
    }

    public String getJsp() {
        return jsp;
    }

    public Page prev() {
        if (prev.isActive()) {
            return prev;
        } else {
            return prev.prev();
        }
    }

    public Page next() {
        if (next.isActive()) {
            return next;
        } else {
            return next.next();
        }
    }

    public void dispatchToJSP(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatchToJSP(req, resp, false);
    }

    public void dispatchToJSP(HttpServletRequest req, HttpServletResponse resp, boolean postBack)
            throws ServletException, IOException {

        // be sure to bind context
        Context.instance(req);

        // make currentPage object available
        req.setAttribute("currentPage", this);

        // render JSP
        if (postBack && "POST".equals(req.getMethod())) {
            // handle POST-Back
            // fixes URLs and remove refresh warnings
            String target = "/" + req.getContextPath() + "/" + action;
            if (target.startsWith("//")) {
                target = target.substring(1);
            }
            resp.sendRedirect(target);
        } else {
            req.getRequestDispatcher(jsp).forward(req, resp);
        }

    }

    @Override
    public String toString() {
        return action + ":" + jsp;
    }

    public int getProgress() {
        return progress;
    }

    public String getAssociatedIFrameUrl() {
        return NUXEO_COM_URL + action;
    }

    public String getLabelKey() {
        return "label.short." + jsp.replace(".jsp", "").substring(1);
    }

    public boolean isVisibleInNavigationMenu() {
        return active && (!hidden);
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasBeenNavigatedBefore() {
        return navigated;
    }
}
