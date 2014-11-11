package org.apache.shindig.gadgets.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.servlet.InjectedServlet;

import com.google.inject.Inject;

public class NXProxyServlet extends InjectedServlet {
    private NXProxyHandler proxyHandler;

    @Inject
    public void setProxyHandler(NXProxyHandler proxyHandler) {
      this.proxyHandler = proxyHandler;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
      proxyHandler.fetch(new ProxyServletRequest(request), response);
    }
  }
