package org.nuxeo.ecm.webengine.gwt;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

public interface GwtAppResolver {

    URI source();

    File resolve(String path) throws FileNotFoundException;
}
