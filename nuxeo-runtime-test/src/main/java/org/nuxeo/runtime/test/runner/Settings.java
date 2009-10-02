package org.nuxeo.runtime.test.runner;

import org.junit.runner.Description;

public class Settings {

    private final Description description;

    public Settings(Description description) {
        this.description = description;
    }


    public String[] getBundles() {
        Bundles annotation = description.getAnnotation(Bundles.class);
        if(annotation != null) {
            return annotation.value();
        } else {
            return new String[0];
        }
    }
}
