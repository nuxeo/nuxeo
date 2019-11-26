/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.scroll;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

/**
 * Scrolls document identifiers using the repository search.
 *
 * @since 11.1
 */
public class RepositoryScroll implements Scroll {

    protected DocumentScrollRequest request;

    protected NuxeoLoginContext loginContext;

    protected CloseableCoreSession session;

    protected ScrollResult<String> repoScroller;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        if (!(request instanceof DocumentScrollRequest)) {
            throw new IllegalArgumentException("Requires a DocumentScrollRequest");
        }
        this.request = (DocumentScrollRequest) request;
        login();
        openSession();
    }

    protected void login() {
        String username = request.getUsername();
        try {
            loginContext = SYSTEM_USERNAME.equals(username) ? Framework.loginSystem() : Framework.loginUser(username);
        } catch (LoginException e) {
            throw new IllegalArgumentException("Cannot login as user: " + username, e);
        }
    }

    protected void openSession() {
        session = CoreInstance.openCoreSession(request.getRepository());
    }

    @Override
    public boolean fetch() {
        if (repoScroller == null) {
            repoScroller = session.scroll(request.getQuery(), request.getSize(),
                    (int) request.getTimeout().toSeconds());
        } else {
            repoScroller = session.scroll(repoScroller.getScrollId());
        }
        return repoScroller.hasResults();
    }

    @Override
    public List<String> getIds() {
        if (repoScroller == null) {
            throw new IllegalStateException("fetch must be called first");
        }
        if (repoScroller.hasResults()) {
            return repoScroller.getResults();
        }
        return Collections.emptyList();
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
        if (loginContext != null) {
            loginContext.close();
            loginContext = null;
        }
    }

    @Override
    public String toString() {
        return "RepositoryScroll{" + "request=" + request + '}';
    }

}
