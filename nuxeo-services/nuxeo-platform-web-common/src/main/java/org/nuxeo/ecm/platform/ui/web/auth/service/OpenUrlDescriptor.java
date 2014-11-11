/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject(value = "openUrl")
public class OpenUrlDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

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
        if (compiledGrantPattern == null && (grantPattern!=null && grantPattern.length()>0)) {
            compiledGrantPattern = Pattern.compile(grantPattern);
        }
        return compiledGrantPattern;
    }

    public Pattern getCompiledDenyPattern() {
        if (compiledDenyPattern == null && (denyPattern!=null && denyPattern.length()>0)) {
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
        String method = httpRequest.getMethod();

        if (getMethod() != null) {
            if (!getMethod().equals(method)) {
                return false;
            }
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
