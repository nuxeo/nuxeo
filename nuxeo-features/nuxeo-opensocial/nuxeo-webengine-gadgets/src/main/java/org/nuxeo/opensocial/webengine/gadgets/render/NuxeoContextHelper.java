package org.nuxeo.opensocial.webengine.gadgets.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public class NuxeoContextHelper {

    protected static NuxeoContextHelper instance;

    protected Map<String, String> repoNames;

    protected String defaultRepoName;

    public static NuxeoContextHelper getInstance() {
        if (instance == null) {
            instance = new NuxeoContextHelper();
        }
        return instance;
    }

    public boolean isMultiRepository() {
        return getRepos().size() > 1;
    }

    public Map<String, String> getRepos() {
        if (repoNames == null) {
            fetchContextInfo();
        }
        return repoNames;
    }

    public Set<String> getRepoNames() {
        return getRepos().keySet();
    }

    public String getRepoLabel(String name) {
        return getRepos().get(name);
    }

    public String getDefaultRepoName() {
        if (defaultRepoName == null) {
            fetchContextInfo();
        }
        return defaultRepoName;
    }

    protected synchronized void fetchContextInfo() {

        LoginContext loginContext = null;

        try {
            loginContext = Framework.login();
            RepositoryManager rm = Framework.getLocalService(RepositoryManager.class);
            repoNames = new HashMap<String, String>();
            for (Repository repo : rm.getRepositories()) {
                repoNames.put(repo.getName(), repo.getLabel());
                if (repo.isDefault() || "default".equals(repo.getName())) {
                    defaultRepoName = repo.getName();
                }
            }
        } catch (Exception e) {
        } finally {
            if (loginContext != null) {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                    // NOP
                }
            }
        }

    }

}
