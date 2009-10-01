package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.junit.runner.Description;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation.Repository;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation.RepositoryFactory;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation.Session;

@Session
@Repository
public class Settings {

    private final Description description;

    public Settings(Description description) {
        this.description = description;
    }

    public RepoType getRepoType() {
        Repository repo = description.getAnnotation(Repository.class);
        if (repo == null) {
            return this.getClass().getAnnotation(Repository.class).value();
        }
        return repo.value();
    }


    public String getRepoUsername() {
        Session sessionFactory= description.getAnnotation(Session.class);
        if(sessionFactory == null) {
            return  this.getClass().getAnnotation(Session.class).user();
        }
        return sessionFactory.user();
    }

    public RepoFactory getRepoFactory() {

        RepositoryFactory annotation = description.getAnnotation(RepositoryFactory.class);
        if(annotation != null) {
            try {
                RepoFactory instance = annotation.value().newInstance();
                return instance;
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

}
