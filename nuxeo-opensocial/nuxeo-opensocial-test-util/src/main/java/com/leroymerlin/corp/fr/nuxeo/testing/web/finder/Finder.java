package com.leroymerlin.corp.fr.nuxeo.testing.web.finder;

import java.util.NoSuchElementException;

public interface Finder<T> {

  T find() throws NoSuchElementException;

}
