package org.nuxeo.ecm.webengine.test.web.finder;

import java.util.NoSuchElementException;

public interface Finder<T> {

  T find() throws NoSuchElementException;

}
