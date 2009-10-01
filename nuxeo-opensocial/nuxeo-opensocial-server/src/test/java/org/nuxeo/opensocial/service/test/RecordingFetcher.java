package org.nuxeo.opensocial.service.test;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;


public class RecordingFetcher implements HttpFetcher {

  private static HttpRequest lastRequest;
  private static HttpResponse response;

  public static void setReponse(HttpResponse response) {
    RecordingFetcher.response = response;
  }

  public HttpResponse fetch(HttpRequest req) throws GadgetException {
    lastRequest = req;
    return response;
  }

  public static HttpRequest getLastRequest() {
    return lastRequest;
  }

}