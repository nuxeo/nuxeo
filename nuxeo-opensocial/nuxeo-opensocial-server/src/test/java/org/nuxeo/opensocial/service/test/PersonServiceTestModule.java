package org.nuxeo.opensocial.service.test;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.shindig.auth.AnonymousAuthenticationHandler;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.social.core.oauth.AuthenticationHandlerProvider;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.core.util.BeanXStreamAtomConverter;
import org.apache.shindig.social.core.util.BeanXStreamConverter;
import org.apache.shindig.social.opensocial.service.BeanConverter;
import org.apache.shindig.social.opensocial.service.DataServiceServletFetcher;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class PersonServiceTestModule extends AbstractModule {

  @Override
  protected void configure() {
    HttpFetcher server = mock(HttpFetcher.class);
    String result = "{userName:'toto'," + "firstName:'Jean',"
        + "lastName:'Dupont'," + "email:'dupont@leroymerlin.fr',"
        + "company:'leroy-merlin'," + "codeMagasin:'123',"
        + "listeCodesRayons:['001']," + "listeLibelleRayons: ['elect'],"
        + "\"mailUser\": [], \"mailHost\": \"zzz\"," + "libelleMagasin:'',"
        + "region:''}";
    HttpResponse response = new HttpResponseBuilder().setHttpStatusCode(200)
        .setResponseString(result)
        .setHeader("Content-type", "application/json")
        .create();
    try {
      when(server.fetch((HttpRequest) anyObject())).thenReturn(response);
    } catch (GadgetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    ;
    bind(HttpFetcher.class).toInstance(server);
    bind(ParameterFetcher.class).annotatedWith(
        Names.named("DataServiceServlet"))
        .to(DataServiceServletFetcher.class);
    bind(BeanConverter.class).annotatedWith(
        Names.named("shindig.bean.converter.xml"))
        .to(BeanXStreamConverter.class);
    bind(BeanConverter.class).annotatedWith(
        Names.named("shindig.bean.converter.json"))
        .to(BeanJsonConverter.class);
    bind(BeanConverter.class).annotatedWith(
        Names.named("shindig.bean.converter.atom"))
        .to(BeanXStreamAtomConverter.class);

    bind(Boolean.class).annotatedWith(
        Names.named(AnonymousAuthenticationHandler.ALLOW_UNAUTHENTICATED))
        .toInstance(Boolean.TRUE);

    bind(new TypeLiteral<List<AuthenticationHandler>>() {
    }).toProvider(AuthenticationHandlerProvider.class);

  }

}
