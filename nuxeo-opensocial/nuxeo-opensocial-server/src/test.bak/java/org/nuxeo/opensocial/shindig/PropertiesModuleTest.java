package org.nuxeo.opensocial.shindig;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

public class PropertiesModuleTest {

  public static class Dummy {

    @Inject
    @Named("shindig.containers.default")
    String prop1;

    @Inject
    @Named("shindig.signing.key-file")
    String prop2;
  }

  public static class TestPropertiesModule extends ResourcePropertiesModule {
    public TestPropertiesModule() {
      super("test.properties");
    }
  }

  @Test
  public void canLoadModuleFromPropertiesPath() throws Exception {
    Injector injector = Guice.createInjector(new OpensocialPropertiesModule());
    assertEquals("res://containers/default/container.js",
        injector.getInstance(Dummy.class).prop1);
  }

  @Test(expected = CreationException.class)
  public void throwsCreationExceptionWhenPropertiesDoesNotExist()
      throws Exception {
    Injector injector = Guice.createInjector(new TestPropertiesModule());
  }

  /**
   * @Test public void propertiesAreOverriddenIfRedefined() throws Exception {
   *       Injector injector = Guice.createInjector(new
   *       OpensocialPropertiesModule(), new TestPropertiesModule()); Dummy
   *       dummy = injector.getInstance(Dummy.class);
   *       assertEquals("res://containers/default/container.js", dummy.prop1);
   *       assertEquals("res://dummy.pem", dummy.prop2); }
   */
}
