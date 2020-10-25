/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("openUrl")
public class OpenUrlDescriptor {

    @XNode("@name")
    protected String name;

    protected String grantPattern;

    protected Pattern compiledGrantPattern;

    @XNode("denyPattern")
    protected String denyPattern;

    protected Pattern compiledDenyPattern;

    @XNode("method")
    protected String method;

    public String getName() {
        return name;
    }

    @XNode("grantPattern")
    public void setGrantPattern(String grantPattern) {
        this.grantPattern = Framework.expandVars(grantPattern);
    }

    public String getGrantPattern() {
        return grantPattern;
    }

    public Pattern getCompiledGrantPattern() {
        if (compiledGrantPattern == null && (grantPattern != null && grantPattern.length() > 0)) {
            compiledGrantPattern = Pattern.compile(grantPattern);
        }
        return compiledGrantPattern;
    }

    public Pattern getCompiledDenyPattern() {
        if (compiledDenyPattern == null && denyPattern != null && denyPattern.length() > 0) {
            compiledDenyPattern = Pattern.compile(denyPattern);
        }
        return compiledDenyPattern;
    }

    public String getDenyPattern() {
        return denyPattern;
    }

    public String getMethod() {
        return method;
    }

    public boolean allowByPassAuth(HttpServletRequest httpRequest) {
        String uri = httpRequest.getRequestURI();
        String requestMethod = httpRequest.getMethod();

        if (method != null && !requestMethod.equals(method)) {
            return false;
        }

        Pattern deny = getCompiledDenyPattern();
        if (deny != null) {
            Matcher denyMatcher = deny.matcher(uri);
            if (denyMatcher.matches()) {
                return false;
            }
        }

        Pattern grant = getCompiledGrantPattern();
        if (grant != null) {
            Matcher grantMatcher = grant.matcher(uri);
            if (grantMatcher.matches()) {
                return true;
            }
        }
        return false;
    }

}
