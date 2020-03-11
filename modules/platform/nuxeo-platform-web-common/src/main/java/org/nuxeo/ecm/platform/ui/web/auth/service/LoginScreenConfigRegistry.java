/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
