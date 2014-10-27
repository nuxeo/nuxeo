/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mhilaire
 */
package org.nuxeo.ecm.platform.login.test;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;

@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.platform.login",
        "org.nuxeo.ecm.platform.login.test:dummy-client-login-config.xml" })
public class ClientLoginFeature extends SimpleFeature {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = { ElementType.TYPE, ElementType.METHOD })
    public @interface User {

        String name() default LoginComponent.SYSTEM_USERNAME;

        boolean anonymous() default true;

        boolean administrator() default false;

        String[] groups() default {};
    }

    protected interface UserCallback extends Callback {
        void setUser(User value);
    }

    public static class Module implements LoginModule {

        protected Subject subject;

        protected CallbackHandler handler;

        protected NuxeoPrincipalImpl identity;

        @Override
        public void initialize(Subject subject, CallbackHandler handler,
                Map<String, ?> sharedState, Map<String, ?> options) {
            this.subject = subject;
            this.handler = handler;
        }

        @Override
        public boolean login() throws LoginException {
            try {
                handler.handle(new Callback[] { new UserCallback() {

                    @Override
                    public void setUser(User user) {
                        identity = new NuxeoPrincipalImpl(user.name(),
                                user.anonymous(), user
                                    .administrator());
                        identity.setGroups(Arrays.asList(user.groups()));
                    }

                } });
            } catch (IOException | UnsupportedCallbackException cause) {
                LoginException error = new LoginException(
                        "Cannot login in test");
                error.initCause(cause);
                throw error;
            }
            if (identity == null) {
                throw new LoginException("Cannot retrieve user");
            }
            return true;
        }

        @Override
        public boolean commit() throws LoginException {
            subject.getPrincipals().add(identity);
            return true;
        }

        @Override
        public boolean abort() throws LoginException {
            return true;
        }

        @Override
        public boolean logout() throws LoginException {
            subject.getPrincipals().remove(identity);
            return true;
        }

    }

    public ClientLoginFeature() {
        super();
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(ClientLoginFeature.class).toInstance(this);
    }

    public LoginContext logContext;

    public Principal principal() {
        Subject subject = logContext.getSubject();
        Set<Principal> principals = subject.getPrincipals();
        return principals.iterator().next();
    }

    public void logout() throws LoginException {
        logContext.logout();
    }

    @Override
    public void beforeMethodRun(final FeaturesRunner runner,
            final FrameworkMethod method, final Object test) throws Exception {
        final User credential = runner
            .getConfig(method, User.class);
        logContext = Framework.login(new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException,
                    UnsupportedCallbackException {
                for (Callback each : callbacks) {
                    if (each instanceof NameCallback) {
                        ((NameCallback) each).setName(credential.name());
                    } else if (each instanceof UserCallback) {
                        ((UserCallback) each).setUser(credential);
                    }
                }
            }
        });
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        logContext.logout();
    }
}
