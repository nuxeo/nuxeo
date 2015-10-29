/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry handling hot-reload and merge of {@link LoginScreenConfig} contributions.
 *
 * @since 7.10
 */
public class LoginScreenConfigRegistry extends SimpleContributionRegistry<LoginScreenConfig> {

    LoginScreenConfig config;

    @Override
    public String getContributionId(LoginScreenConfig contrib) {
        // only one contrib
        return "static";
    }

    @Override
    public void contributionUpdated(String id, LoginScreenConfig contrib, LoginScreenConfig newOrigContrib) {
        config = contrib;
    }

    @Override
    public void contributionRemoved(String id, LoginScreenConfig origContrib) {
        config = currentContribs.get("static");
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    @Override
    public LoginScreenConfig clone(LoginScreenConfig orig) {
        return orig.clone();
    }

    @Override
    public void merge(LoginScreenConfig src, LoginScreenConfig dst) {
        dst.merge(src);
    }

    public LoginScreenConfig getConfig() {
        return config;
    }

}
