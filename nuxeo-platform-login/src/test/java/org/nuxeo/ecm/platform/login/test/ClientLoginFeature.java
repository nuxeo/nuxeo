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
import java.util.Arrays;
import java.util.Map;

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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;

@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.platform.login",
        "org.nuxeo.ecm.platform.login.test:trusted-client-login-config.xml" })
public class ClientLoginFeature extends SimpleFeature {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = { ElementType.TYPE, ElementType.METHOD })
    public @interface Identity {

        String name() default "ClientLoginFeature";

        boolean anonymous() default true;

        boolean administrator() default false;

        String[] groups() default {};
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Opener {

        Class<? extends Listener> value();
    }

    public interface Listener {
        void onLogin(FeaturesRunner runner, FrameworkMethod method,
                LoginContext context);

        void onLogout(FeaturesRunner runner, FrameworkMethod method,
                LoginContext context);
    }

    protected interface IdentityCallback extends Callback {
        void identity(Identity value);
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
                handler.handle(new Callback[] { new IdentityCallback() {

                    @Override
                    public void identity(Identity user) {
                        identity = new NuxeoPrincipalImpl(user.name(), user
                            .anonymous(), user.administrator());
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

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(ClientLoginFeature.class).toInstance(this);
    }

    @SuppressWarnings("rawtypes")
    protected Listener listener;

    protected LoginContext context;

    @Override
    public void beforeMethodRun(final FeaturesRunner runner,
            final FrameworkMethod method, final Object test) throws Exception {
        Identity identity = runner.getConfig(method, Identity.class);
        context = login(identity);
        final Class<? extends Listener> type = runner.getConfig(Opener.class).value();
        if (type == null) {
            return;
        }
        listener = type
            .getConstructor(test.getClass()).newInstance(test);
        listener.onLogin(runner, method, context);
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        try {
            if (listener != null) {
                listener.onLogout(runner, method, context);
            }
        } finally {
            listener = null;
            context.logout();
        }
    }

    protected LoginContext login(final Identity credential)
            throws LoginException {
        return Framework.login(new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException,
                    UnsupportedCallbackException {
                for (Callback each : callbacks) {
                    if (each instanceof NameCallback) {
                        ((NameCallback) each).setName(credential.name());
                    } else if (each instanceof IdentityCallback) {
                        ((IdentityCallback) each).identity(credential);
                    }
                }
            }
        });
    }
}
