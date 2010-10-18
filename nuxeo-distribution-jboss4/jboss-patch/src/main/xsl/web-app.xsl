<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:param name="protocol" select="jrmp"/>
    <xsl:param name="locator" select="servlet"/>
    <xsl:param name="hostname" select="www.nuxeo.org"/>
    <xsl:param name="port" select="8080"/>

  <!-- copy with attributes, and keep comments -->
  <xsl:template match="@*|node()|comment()">
	<xsl:copy>
		<xsl:choose>
			<xsl:when test="$protocol='http'"><xsl:apply-templates select="@*|node()|comment()" mode="http"/></xsl:when>
			<xsl:when test="$protocol='https'"><xsl:apply-templates select="@*|node()|comment()" mode="http"/></xsl:when>
			<xsl:otherwise><xsl:apply-templates select="@*|node()|comment()" mode="jrmp"/></xsl:otherwise>
		</xsl:choose>
	</xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()|comment()" mode="jrmp">
  <!-- This template leaves source XML intact -->
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()" mode="jrmp"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()|comment()" mode="http">
  <!-- This template leaves source XML intact with exceptions below -->
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()" mode="http"/>
    </xsl:copy>
  </xsl:template>

  <!-- Removing old EJB3 connector servlets -->
  <xsl:template match="web-app/servlet[servlet-name/text()='ServerInvokerServlet']" mode="jrmp">
      <xsl:comment>Nuxeo patch: removing servlet[ServerInvokerServlet]</xsl:comment>   
  </xsl:template>
  <xsl:template match="web-app/servlet[servlet-name/text()='Ejb3InvokerServlet']" mode="jrmp">
      <xsl:comment>Nuxeo patch: removing servlet[Ejb3InvokerServlet]</xsl:comment>   
  </xsl:template>
  <xsl:template match="web-app/servlet-mapping[servlet-name/text()='ServerInvokerServlet']" mode="jrmp">
      <xsl:comment>Nuxeo patch: removing servlet-mapping[ServerInvokerServlet]</xsl:comment>   
  </xsl:template>
  <xsl:template match="web-app/servlet-mapping[servlet-name/text()='Ejb3InvokerServlet']" mode="jrmp">
      <xsl:comment>Nuxeo patch: removing servlet-mapping[Ejb3InvokerServlet]</xsl:comment>   
  </xsl:template>
  <!-- Removing old EJB3 connector servlets -->
  <xsl:template match="web-app/servlet[servlet-name/text()='ServerInvokerServlet']" mode="http">
      <xsl:comment>Nuxeo patch: removing servlet[ServerInvokerServlet]</xsl:comment>   
  </xsl:template>
  <xsl:template match="web-app/servlet[servlet-name/text()='Ejb3InvokerServlet']" mode="http">
      <xsl:comment>Nuxeo patch: removing servlet[Ejb3InvokerServlet]</xsl:comment>   
  </xsl:template>
  <xsl:template match="web-app/servlet-mapping[servlet-name/text()='ServerInvokerServlet']" mode="http">
      <xsl:comment>Nuxeo patch: removing servlet-mapping[ServerInvokerServlet]</xsl:comment>   
  </xsl:template>
  <xsl:template match="web-app/servlet-mapping[servlet-name/text()='Ejb3InvokerServlet']" mode="http">
      <xsl:comment>Nuxeo patch: removing servlet-mapping[Ejb3InvokerServlet]</xsl:comment>   
  </xsl:template>

  <!-- Updating EJB3 connector servlets -->
  <xsl:template match="web-app/servlet[1]" mode="http">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()"/>
    </xsl:copy><xsl:text>
      </xsl:text><xsl:comment>Nuxeo patch: EJB3, unified connectors servlets added </xsl:comment><xsl:text>
      </xsl:text><servlet><xsl:text>
         </xsl:text><servlet-name>ServerInvokerServlet</servlet-name><xsl:text>
         </xsl:text><description><xsl:text>The ServerInvokerServlet receives requests via HTTP
            protocol from within a web container and passes it onto the
            ServletServerInvoker for processing.
         </xsl:text></description><xsl:text>
         </xsl:text><servlet-class>org.jboss.remoting.transport.servlet.web.ServerInvokerServlet</servlet-class><xsl:text>
         </xsl:text><init-param><xsl:text>
            </xsl:text><param-name>locatorUrl</param-name><xsl:text>
            </xsl:text><param-value><xsl:value-of select="concat($locator,'://',$hostname,':',$port)"/>/invoker/ServerInvokerServlet</param-value><xsl:text>
            </xsl:text><description>The servlet server invoker locator url</description><xsl:text>
         </xsl:text></init-param><xsl:text>
         </xsl:text><load-on-startup>1</load-on-startup><xsl:text>
      </xsl:text></servlet><xsl:text>
      </xsl:text><servlet><xsl:text>
         </xsl:text><servlet-name>Ejb3InvokerServlet</servlet-name><xsl:text>
         </xsl:text><description><xsl:text>The ServerInvokerServlet receives requests via HTTP
            protocol from within a web container and passes it onto the
            ServletServerInvoker for processing.
         </xsl:text></description><xsl:text>
         </xsl:text><servlet-class>org.jboss.remoting.transport.servlet.web.ServerInvokerServlet</servlet-class><xsl:text>
         </xsl:text><init-param><xsl:text>
            </xsl:text><param-name>invokerName</param-name><xsl:text>
            </xsl:text><param-value>jboss.remoting:service=invoker,transport=servlet</param-value><xsl:text>
            </xsl:text><description>The name of the connector mbean to forward connections - jbos-remoting 4.0.4.GA  is not supporting locatorUrl</description><xsl:text>
         </xsl:text></init-param><xsl:text>
         </xsl:text><init-param><xsl:text>
            </xsl:text><param-name>locatorUrl</param-name><xsl:text>
            </xsl:text><param-value><xsl:value-of select="concat($locator,'://',$hostname,':',$port)"/>/invoker/Ejb3InvokerServlet</param-value><xsl:text>
            </xsl:text><description>The servlet server invoker locator url</description><xsl:text>
         </xsl:text></init-param><xsl:text>
         </xsl:text><load-on-startup>1</load-on-startup><xsl:text>
      </xsl:text></servlet>
  </xsl:template>

  <!-- Updating EJB3 connector servlets -->
  <xsl:template match="web-app/servlet-mapping[1]" mode="http">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()"/>
    </xsl:copy><xsl:text>
      </xsl:text><xsl:comment>Nuxeo patch: EJB3, unified connectors servlets mappings added</xsl:comment><xsl:text>
      </xsl:text><servlet-mapping><xsl:text>
         </xsl:text><servlet-name>ServerInvokerServlet</servlet-name><xsl:text>
         </xsl:text><url-pattern>/ServerInvokerServlet/*</url-pattern><xsl:text>
      </xsl:text></servlet-mapping><xsl:text>

      </xsl:text><servlet-mapping><xsl:text>
         </xsl:text><servlet-name>Ejb3InvokerServlet</servlet-name><xsl:text>
         </xsl:text><url-pattern>/Ejb3InvokerServlet/*</url-pattern><xsl:text>
      </xsl:text></servlet-mapping>
  </xsl:template>

</xsl:stylesheet>
