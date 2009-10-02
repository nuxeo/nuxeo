package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpSimpleServer {

  private ServerSocket serverSocket;
  private int port = 0;
  private StringBuffer request = new StringBuffer();
  private String expectResponse;
  private Thread t;

  public void start() throws IOException {
    serverSocket = new ServerSocket(0);
    port = serverSocket.getLocalPort();
    t = new Thread() {
      public void run() {
        Socket rep = null;
        try {
          rep = serverSocket.accept();
          BufferedReader rd = new BufferedReader(new InputStreamReader(
              rep.getInputStream()));
          String line = null;
          while (true) {
            line = rd.readLine();
            request.append(line)
                .append("\n");
            if (line == null || "".equals(line))
              break;
          }
          PrintStream ps = new PrintStream(rep.getOutputStream());
          if (expectResponse != null){
            ps.println(expectResponse);
          }
          else{
            ps.println("HTTP/1.1 200 OK");
            ps.println("");
          }
          ps.flush();
          ps.close();
          rep.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          if (rep != null)
            try {
              rep.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }


  public int getPort() {
    return port;
  }

  public void setResponse(String response) {
    this.expectResponse = response;
  }

}
