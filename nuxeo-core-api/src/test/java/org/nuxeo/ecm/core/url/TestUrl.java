package org.nuxeo.ecm.core.url;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

import junit.framework.TestCase;

public class TestUrl extends TestCase {

    public void test() throws IOException {
        URL url1 = URLFactory.getURL("http://toto.com/");
        assertEquals("http://toto.com/", url1.toString());

        URL url2 = URLFactory.getURL("nxdoc://toto/titi/");
        assertEquals("nxdoc://toto/titi/", url2.toString());
        url2.openConnection();

        URL url3 = URLFactory.getURL("nxobj://toto/titi/");
        assertEquals("nxobj://toto/titi/", url3.toString());
        url3.openConnection();
    }

}
