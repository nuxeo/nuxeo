/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.LoginModuleWrapper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("domain")
public class SecurityDomain {

    @XNode("@name")
    private String name;

    private AppConfigurationEntry[] entries;

    public SecurityDomain() {
    }

    public SecurityDomain(String name) {
        this.name = name;
    }

    public SecurityDomain(String name, AppConfigurationEntry[] entries) {
        this.name = name;
        this.entries = entries;
    }

    public String getName() {
        return name;
    }

    public AppConfigurationEntry[] getAppConfigurationEntries() {
        return entries;
    }

    public void setAppConfigurationEntries(AppConfigurationEntry[] entries) {
        this.entries = entries;
    }

    @XNodeList(value = "login-module", type = ArrayList.class, componentType = LoginModuleDescriptor.class)
    public void setEntries(List<LoginModuleDescriptor> descriptors) {
        entries = new AppConfigurationEntry[descriptors.size()];
        int i = 0;
        for (LoginModuleDescriptor descriptor : descriptors) {
            LoginModuleControlFlag flag = null;
            if (descriptor.flag == null) {
                flag = LoginModuleControlFlag.OPTIONAL;
            } else if ("optional".equals(descriptor.flag)) {
                flag = LoginModuleControlFlag.OPTIONAL;
            } else if ("sufficient".equals(descriptor.flag)) {
                flag = LoginModuleControlFlag.SUFFICIENT;
            } else if ("required".equals(descriptor.flag)) {
                flag = LoginModuleControlFlag.REQUIRED;
            } else if ("requisite".equals(descriptor.flag)) {
                flag = LoginModuleControlFlag.REQUISITE;
            }
            descriptor.options.put(LoginModuleWrapper.DELEGATE_CLASS_KEY, descriptor.code);
            entries[i++] = new AppConfigurationEntry(LoginModuleWrapper.class.getName(), flag, descriptor.options);
        }
    }

    public LoginContext login(Subject subject) throws LoginException {
        LoginContext ctx = new LoginContext(name, subject);
        ctx.login();
        return ctx;
    }

    public LoginContext login(CallbackHandler handler) throws LoginException {
        LoginContext ctx = new LoginContext(name, handler);
        ctx.login();
        return ctx;
    }

    public LoginContext login(Subject subject, CallbackHandler handler) throws LoginException {
        LoginContext ctx = new LoginContext(name, subject, handler);
        ctx.login();
        return ctx;
    }

    public LoginContext login(String username, Object credentials) throws LoginException {
        CredentialsCallbackHandler handler = new CredentialsCallbackHandler(username, credentials);
        LoginContext ctx = new LoginContext(name, handler);
        ctx.login();
        return ctx;
    }

    public static String controlFlagToString(LoginModuleControlFlag flag) {
        if (flag == LoginModuleControlFlag.OPTIONAL) {
            return "optional";
        } else if (flag == LoginModuleControlFlag.REQUIRED) {
            return "required";
        } else if (flag == LoginModuleControlFlag.REQUISITE) {
            return "requisite";
        } else if (flag == LoginModuleControlFlag.SUFFICIENT) {
            return "sufficient";
        }
        throw new IllegalArgumentException("Not a supported LoginModuleControlFlag: " + flag);
    }

    public static LoginModuleControlFlag controlFlagFromString(String flag) {
        if (flag == null) {
            return LoginModuleControlFlag.OPTIONAL;
        } else if ("optional".equals(flag)) {
            return LoginModuleControlFlag.OPTIONAL;
        } else if ("sufficient".equals(flag)) {
            return LoginModuleControlFlag.SUFFICIENT;
        } else if ("required".equals(flag)) {
            return LoginModuleControlFlag.REQUIRED;
        } else if ("requisite".equals(flag)) {
            return LoginModuleControlFlag.REQUISITE;
        }
        throw new IllegalArgumentException("Not a supported LoginModuleControlFlag: " + flag);
    }

}
