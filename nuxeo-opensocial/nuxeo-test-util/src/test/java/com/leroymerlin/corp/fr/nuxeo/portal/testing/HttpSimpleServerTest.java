package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

public class HttpSimpleServerTest {

  @Test
  public void canServerCanHandleARequest() throws Exception {
    HttpSimpleServer server = new HttpSimpleServer();
    server.start();
    URL url = new URL("http://localhost:" + server.getPort() + "/toto");
    HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
    assertEquals(200, openConnection.getResponseCode());

  }

  @Test
  public void canServerCanSendAGivenResponse() throws Exception {
    HttpSimpleServer server = new HttpSimpleServer();
    server.start();
    server.setResponse("HTTP/1.1 404 NOT FOUND");
    URL url = new URL("http://localhost:" + server.getPort() + "/toto");
    HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
    assertEquals(404, openConnection.getResponseCode());
    assertEquals("NOT FOUND", openConnection.getResponseMessage());
  }
}
