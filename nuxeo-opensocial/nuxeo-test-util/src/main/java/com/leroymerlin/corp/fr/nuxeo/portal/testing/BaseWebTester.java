package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.nuxeo.ecm.core.api.DocumentModel;

public class BaseWebTester {

  private TestWebEngine engine;
  private TestRuntimeHarness runtime;
  private TestRepositoryHandler repository;
  private String[] bundles;

  public BaseWebTester(String... bundles) {
    this.bundles = bundles;
  }

  /**
   * Starts the tester. This methods start the OSGi runtime, a "demo" repository
   * and a web engine. All the bundles given at construction time for this
   * tester are deployed.
   * 
   * @throws Exception
   */
  public void start() throws Exception {
    engine = new TestWebEngine();
    runtime = engine.getRuntime();
    runtime.start();
    repository = engine.getRepository();
    repository.openRepository();
    engine.start();
    for (String bundle : bundles)
      runtime.deployBundle(bundle);
    init();
  }

  /**
   * Hook method for subclasses, called at end of start process, it can be used
   * for eg. initialize root of a repository.
   * 
   * @throws Exception
   */
  protected void init() throws Exception {
  }

  /**
   * Hook method for subclasses, called before stop.
   * 
   * @throws Exception
   */
  protected void uninit() throws Exception {
  }

  public void stop() throws Exception {
    uninit();
    engine.stop();
    engine.getRepository()
        .releaseRepository();
    engine.getRuntime()
        .stop();
  }

  public WithUser withUser(String user)  {
    return new WithUser(user, this);
  }

  public void clearRepository() throws Exception {
    withUser("Administrator").call(new SessionCall<Void>() {

      public Void call() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel defaultDomain = session.createDocumentModel(
            root.getPathAsString(), "default-domain", "Domain");
        defaultDomain = session.createDocument(defaultDomain);
        session.saveDocument(defaultDomain);
        DocumentModel book = session.createDocumentModel(
            defaultDomain.getPathAsString(), "book", "Picturebook");
        book.setProperty("dublincore", "title", "Test Picturebook");
        book = session.createDocument(book);
        session.saveDocument(book);
        return null;
      }

    });
  }

  public TestWebEngine getEngine() {
    return engine;
  }

  public TestRuntimeHarness getRuntime() {
    return runtime;
  }

  public TestRepositoryHandler getRepository() {
    return repository;
  }

}
