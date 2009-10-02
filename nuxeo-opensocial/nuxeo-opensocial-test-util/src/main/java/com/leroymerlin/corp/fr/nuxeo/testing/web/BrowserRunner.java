package com.leroymerlin.corp.fr.nuxeo.testing.web;

import java.util.Arrays;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoModule;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;

public class BrowserRunner extends NuxeoRunner {

  public BrowserRunner(Class<?> classToRun) throws InitializationError {
    super(classToRun, new BrowserModule(), new NuxeoModule());
    try {
      BrowserConfig config = getInjector().getInstance(BrowserConfig.class);
      final String browserType = config.getBrowser();
      filter(new Filter() {

        @Override
        public boolean shouldRun(Description description) {
          SkipBrowser skip = description.getAnnotation(SkipBrowser.class);
          if (skip != null && Arrays.asList(skip.value())
              .contains(browserType)) {

            return false;
          }
          return true;
        }

        @Override
        public String describe() {
          return "Filtering tests according to current browser settings";
        }
      });
    } catch (ClassCastException e) {
      // OK - just skip
    } catch (NoTestsRemainException e) {
      e.printStackTrace();
    }
  }
}
