package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.ecm.webengine.session.StatelessUserSession;
import org.nuxeo.runtime.api.Framework;

public class TestWebEngine {

  public static class SessionCreatorFilter implements Filter {

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
      StatelessUserSession userSession = new StatelessUserSession(
          new FakeNuxeoPrincipal("toto", "François", "Dupont"));
      ((HttpServletRequest) request).setAttribute(
          "nuxeo.webengine.user_session", userSession);
      HttpSession session = ((HttpServletRequest) request).getSession();
      session.setAttribute("nuxeo.webengine.user_session", userSession);
      chain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

  }

  private static final int DEFAULT_PORT = 9999;

  private WebEngine engine;
  private TestRuntimeHarness runtime;

  public WebEngine getEngine() {
    return engine;
  }

  public void setEngine(WebEngine engine) {
    this.engine = engine;
  }

  public TestRuntimeHarness getRuntime() {
    return runtime;
  }

  public void setRuntime(TestRuntimeHarness runtime) {
    this.runtime = runtime;
  }

  public TestRepositoryHandler getRepository() {
    return repository;
  }

  public void setRepository(TestRepositoryHandler repository) {
    this.repository = repository;
  }

  private TestRepositoryHandler repository;
  private int port;

  public TestWebEngine(TestRuntimeHarness runtime,
      TestRepositoryHandler repository) {
    this(runtime, repository, DEFAULT_PORT);
  }

  public TestWebEngine() {
    this(new TestRuntimeHarness(), new TestRepositoryHandler("demo"),
        DEFAULT_PORT);
  }

  public TestWebEngine(TestRuntimeHarness runtime, TestRepositoryHandler repository,
      int port) {
    this.port = port;
    this.runtime = runtime;
    this.repository = repository;
  }

  public void start() throws Exception {
    runtime.deployBundle("org.nuxeo.ecm.core.query");
    runtime.deployBundle("org.nuxeo.ecm.platform.api");
    runtime.deployBundle("org.nuxeo.ecm.platform.web.common");
    runtime.deployBundle("org.nuxeo.ecm.platform.rendering");
    runtime.deployBundle("org.nuxeo.ecm.platform.usermanager.api");
    runtime.deployBundle("org.nuxeo.ecm.platform.usermanager");
    runtime.deployBundle("org.nuxeo.ecm.platform.usermanager.facade");
    runtime.deployBundle("org.nuxeo.ecm.webengine.core");
    runtime.deployBundle("org.nuxeo.ecm.webengine.base");
    runtime.deployBundle("org.nuxeo.ecm.webengine.ui");

    engine = Framework.getLocalService(WebEngine.class);
  }

  public int getPort() {
    return port;
  }

  public void stop() throws Exception {

  }

  public void deployTestModule() {
		// TODO: include that procedure in the WE Harness
		URL currentDir = Thread.currentThread().getContextClassLoader()
				.getResource(".");

		ModuleManager moduleManager = Framework
				.getLocalService(WebEngine.class).getModuleManager();
		moduleManager.loadModuleFromDir(new File(currentDir.getFile()));
  }
}
