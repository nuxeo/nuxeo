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
  <!-- This template leaves source XML almost intact -->
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()" mode="jrmp"/>
    </xsl:copy>
  </xsl:template>
  <!-- Making sure EAR deployer switches to NuxeoDeployer, the rest is left intact -->
  <xsl:template match="mbean[@name='jboss.j2ee:service=EARDeployer']" mode="jrmp">
     <xsl:comment>Nuxeo patch: mbean[jboss.j2ee:service=EARDeployer]: changing 'code' attribute to 'org.nuxeo.runtime.jboss.deployment.NuxeoDeployer'</xsl:comment><xsl:text>
   </xsl:text><mbean code="org.nuxeo.runtime.jboss.deployment.NuxeoDeployer" name="jboss.j2ee:service=EARDeployer"><xsl:text>
      </xsl:text><xsl:comment><xsl:text> A flag indicating if ear deployments should have their own scoped
      class loader to isolate their classes from other deployments.
      </xsl:text></xsl:comment><xsl:text>
      </xsl:text><attribute name="Isolated">false</attribute><xsl:text>
      </xsl:text><xsl:comment><xsl:text>A flag indicating if the ear components should have in VM call
      optimization disabled.
      </xsl:text></xsl:comment><xsl:text>
      </xsl:text><attribute name="CallByValue">false</attribute><xsl:text>
      </xsl:text><xsl:comment><xsl:text> NXJBossExtensions: A flag indicating that the preprocessor
           must always run before deploying the EAR.
           On live systems you may want to avoid running the preprocessor
           each time, and run it manually after modifying the EAR
           either from the JMX console or by deleting the file
           ".predeploy" and restarting JBoss.
      </xsl:text></xsl:comment><xsl:text>
      </xsl:text><attribute name="Debug">true</attribute><xsl:text>
   </xsl:text></mbean>
  </xsl:template>


  <xsl:template match="@*|node()|comment()" mode="http">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()" mode="http"/>
    </xsl:copy>
  </xsl:template>


  <!-- Removing JRMP invoker -->
  <xsl:template match="mbean[@name='jboss:service=invoker,type=jrmp']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss:service=invoker,type=jrmp] removed</xsl:comment>
  </xsl:template>

  <!-- Removing Pooled invoker -->
  <xsl:template match="mbean[@name='jboss:service=invoker,type=pooled']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss:service=invoker,type=pooled] removed</xsl:comment>
  </xsl:template>

  <!-- Removing UIL2 invoker -->
  <xsl:template match="mbean[@name='jboss.mq:service=InvocationLayer,type=UIL2']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss.mq:service=InvocationLayer,type=UIL2] removed</xsl:comment>
  </xsl:template>
  <!-- HTTP UIL factories get rid of HTTP prefix -->
  <xsl:template match="mbean[@name='jboss.mq:service=InvocationLayer,type=HTTP']/attribute[@name='ConnectionFactoryJNDIRef']" mode="http">
      <xsl:comment>Nuxeo patch: mbean[jboss.mq:service=InvocationLayer,type=HTTP] change connection factory</xsl:comment><xsl:text>
         </xsl:text><attribute name='ConnectionFactoryJNDIRef'>ConnectionFactory</attribute>
  </xsl:template>
  <xsl:template match="mbean[@name='jboss.mq:service=InvocationLayer,type=HTTP']/attribute[@name='XAConnectionFactoryJNDIRef']" mode="http">
      <xsl:comment>Nuxeo patch: mbean[jboss.mq:service=InvocationLayer,type=HTTP] change connection factory</xsl:comment><xsl:text>
         </xsl:text><attribute name='XAConnectionFactoryJNDIRef'>XAConnectionFactory</attribute>
  </xsl:template>

  <!-- Replacing socket locator dependency for unified invoker with SSLServlet transport (JBoss 4.2) -->
  <xsl:template match="mbean[@name='jboss:service=invoker,type=unified']/depends[text()='jboss.remoting:service=Connector,transport=socket']" mode="http">
     <xsl:comment>Nuxeo patch: Replacing socket locator dependency for unified invoker with Servlet transport</xsl:comment><xsl:text>
     </xsl:text><depends>jboss.remoting:service=Connector,transport=Servlet</depends>
  </xsl:template>
  <!-- Replacing socket locator for unified invoker with SSLServlet transport (JBoss 4.2) -->
  <xsl:template match="mbean[@name='jboss.remoting:service=Connector,transport=socket']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss.remoting:service=Connector,transport=socket] replaced</xsl:comment><xsl:text>
   </xsl:text><mbean code="org.jboss.remoting.transport.Connector" name="jboss.remoting:service=Connector,transport=Servlet" display-name="SSL Servlet transport Connector"><xsl:text>
      </xsl:text><attribute name="InvokerLocator"><xsl:value-of select="concat($locator,'://',$hostname,':',$port)"/>/invoker/ServerInvokerServlet</attribute><xsl:text>
      </xsl:text><attribute name="Configuration"><xsl:text>
         </xsl:text><config><xsl:text>
            </xsl:text><handlers><xsl:text>
               </xsl:text><handler subsystem="invoker">jboss:service=invoker,type=unified</handler><xsl:text>
            </xsl:text></handlers><xsl:text>
         </xsl:text></config><xsl:text>
      </xsl:text></attribute><xsl:text>
      </xsl:text><depends>jboss.remoting:service=NetworkRegistry</depends><xsl:text>
   </xsl:text></mbean>
  </xsl:template>


  <!-- Getting rid of WebService mbean and its dependencies
  <xsl:template match="mbean[@name='jboss:service=WebService']">
  </xsl:template>

  <xsl:template match="mbean/depends[text()='jboss:service=WebService']">
  </xsl:template>
  -->
  
  <!-- Replacing JRMP JMX invoker with HTTP invoker -->
  <xsl:template match="mbean[@name='jboss.jmx:type=adaptor,name=Invoker,protocol=jrmp,service=proxyFactory']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss.jmx:type=adaptor,name=Invoker,protocol=jrmp,service=proxyFactory] replaced</xsl:comment><xsl:text>
      </xsl:text><mbean code="org.jboss.invocation.http.server.HttpProxyFactory" name="jboss.jmx:type=adaptor,name=Invoker,protocol=http,service=proxyFactory"><xsl:text>
         </xsl:text><attribute name="InvokerURL"><xsl:value-of select="concat($protocol,'://',$hostname,':',$port)"/>/invoker/JMXInvokerServlet</attribute><xsl:text>
         </xsl:text><depends optional-attribute-name="InvokerName">jboss.jmx:type=adaptor,name=Invoker</depends><xsl:text>
         </xsl:text><depends>jboss:service=invoker,type=http</depends><xsl:text>
         </xsl:text><attribute name="ExportedInterface">org.jboss.jmx.adaptor.rmi.RMIAdaptor</attribute><xsl:text>
         </xsl:text><attribute name="JndiName">jmx/invoker/HttpAdaptor</attribute><xsl:text>
         </xsl:text><attribute name="ClientInterceptors"><xsl:text>
            </xsl:text><interceptors><xsl:text>
               </xsl:text><interceptor>org.jboss.proxy.ClientMethodInterceptor</interceptor><xsl:text>
               </xsl:text><interceptor>org.jboss.proxy.SecurityInterceptor</interceptor><xsl:text>
               </xsl:text><interceptor>org.jboss.jmx.connector.invoker.client.InvokerAdaptorClientInterceptor</interceptor><xsl:text>
               </xsl:text><interceptor>org.jboss.invocation.InvokerInterceptor</interceptor><xsl:text>
            </xsl:text></interceptors><xsl:text>
         </xsl:text></attribute><xsl:text>
      </xsl:text></mbean><xsl:text>
      </xsl:text><mbean code="org.jboss.jmx.connector.invoker.MBeanProxyRemote" name="jboss.jmx:type=adaptor,name=MBeanProxyRemote,protocol=http"><xsl:text>
         </xsl:text><depends optional-attribute-name="MBeanServerConnection">jboss.jmx:type=adaptor,name=Invoker,protocol=http,service=proxyFactory</depends><xsl:text>
      </xsl:text></mbean>
   </xsl:template>

  <!-- Replacing proxy dependency for Plugin manager -->
  <xsl:template match="mbean[@name='jboss.admin:service=PluginManager']/depends[text()='jboss.jmx:type=adaptor,name=Invoker,protocol=jrmp,service=proxyFactory']" mode="http">
     <xsl:comment>Nuxeo patch: Replacing proxy dependency for mbean[jboss.admin:service=PluginManager]</xsl:comment><xsl:text>
     </xsl:text><depends>jboss.jmx:type=adaptor,name=Invoker,protocol=http,service=proxyFactory</depends><xsl:text>
     </xsl:text><depends>jboss.jmx:alias=jmx/rmi/RMIAdaptor</depends>
  </xsl:template>

  <xsl:template match="mbean[(@name!='jboss.admin:service=PluginManager')and(count(depends[text()='jboss.jmx:type=adaptor,name=Invoker,protocol=jrmp,service=proxyFactory'])&gt;0)]" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[<xsl:value-of select="@name"/>] removed as dependent on mbean[jboss.jmx:type=adaptor,name=Invoker,protocol=jrmp,service=proxyFactory]</xsl:comment>
  </xsl:template>

  <xsl:template match="mbean[@name='jboss.jmx:type=adaptor,name=MBeanProxyRemote,protocol=http']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss.jmx:type=adaptor,name=MBeanProxyRemote,protocol=http] removed</xsl:comment>
  </xsl:template>
  <xsl:template match="mbean[@name='jboss.jmx:type=adaptor,name=MBeanProxyRemote,protocol=jrmp']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss.jmx:type=adaptor,name=MBeanProxyRemote,protocol=jrmp] removed</xsl:comment>
  </xsl:template>
  <xsl:template match="mbean[@name='jboss.jmx:alias=jmx/rmi/RMIAdaptor']" mode="http">
      <xsl:comment>Nuxeo patch: added naming aliases to the Http Proxy</xsl:comment><xsl:text>
      </xsl:text><mbean code="org.jboss.naming.NamingAlias" name="jboss.jmx:alias=jmx/invoker/RMIAdaptor"><xsl:text>
         </xsl:text><attribute name="FromName">jmx/invoker/RMIAdaptor</attribute><xsl:text>
         </xsl:text><attribute name="ToName">jmx/invoker/HttpAdaptor</attribute><xsl:text>
         </xsl:text><depends>jboss:service=Naming</depends><xsl:text>
      </xsl:text></mbean><xsl:text>
      </xsl:text><xsl:comment>Nuxeo patch: mbean[jboss.jmx:alias=jmx/rmi/RMIAdaptor] replaced</xsl:comment><xsl:text>
      </xsl:text><mbean code="org.jboss.naming.NamingAlias" name="jboss.jmx:alias=jmx/rmi/RMIAdaptor"><xsl:text>
         </xsl:text><attribute name="FromName">jmx/rmi/RMIAdaptor</attribute><xsl:text>
         </xsl:text><attribute name="ToName">jmx/invoker/HttpAdaptor</attribute><xsl:text>
         </xsl:text><depends>jboss:service=Naming</depends><xsl:text>
      </xsl:text></mbean>
  </xsl:template>
  <xsl:template match="mbean[@name='jboss.jmx:alias=jmx/invoker/RMIAdaptor']" mode="http">
  	<xsl:comment>Nuxeo patch: mbean[jboss.jmx:alias=jmx/invoker/RMIAdaptor] removed</xsl:comment>
  </xsl:template>

  <!-- Replacing ClientUserTransaction as depending on JRMP invoker -->
  <xsl:template match="mbean[@name='jboss:service=ClientUserTransaction']" mode="http">
   <xsl:comment>Nuxeo patch: mbean[jboss:service=ClientUserTransaction] now depends on HTTP Proxy</xsl:comment><xsl:text>
   </xsl:text><mbean code="org.jboss.tm.usertx.server.ClientUserTransactionService" name="jboss:service=ClientUserTransaction" xmbean-dd="resource:xmdesc/ClientUserTransaction-xmbean.xml"><xsl:text>
      </xsl:text><depends><xsl:text>
         </xsl:text><mbean code="org.jboss.invocation.http.server.HttpProxyFactory" name="jboss:service=proxyFactory,target=ClientUserTransactionFactory"><xsl:text>
       </xsl:text><attribute name="InvokerName">jboss:service=invoker,type=http</attribute><xsl:text>
       </xsl:text><attribute name="JndiName">UserTransactionSessionFactory</attribute><xsl:text>
       </xsl:text><attribute name="ExportedInterface">org.jboss.tm.usertx.interfaces.UserTransactionSessionFactory</attribute><xsl:text>
       </xsl:text><attribute name="ClientInterceptors"><xsl:text>
          </xsl:text><interceptors><xsl:text>
             </xsl:text><interceptor>org.jboss.proxy.ClientMethodInterceptor</interceptor><xsl:text>
             </xsl:text><interceptor>org.jboss.invocation.InvokerInterceptor</interceptor><xsl:text>
          </xsl:text></interceptors><xsl:text>
       </xsl:text></attribute><xsl:text>
       </xsl:text><depends>jboss:service=invoker,type=http</depends><xsl:text>
         </xsl:text></mbean><xsl:text>
      </xsl:text></depends><xsl:text>
      </xsl:text><depends optional-attribute-name="TxProxyName"><xsl:text>
         </xsl:text><mbean code="org.jboss.invocation.http.server.HttpProxyFactory" name="jboss:service=proxyFactory,target=ClientUserTransaction"><xsl:text>
       </xsl:text><attribute name="InvokerName">jboss:service=invoker,type=http</attribute><xsl:text>
       </xsl:text><attribute name="JndiName"></attribute><xsl:text>
       </xsl:text><attribute name="ExportedInterface">org.jboss.tm.usertx.interfaces.UserTransactionSession</attribute><xsl:text>
       </xsl:text><attribute name="ClientInterceptors"><xsl:text>
          </xsl:text><interceptors><xsl:text>
             </xsl:text><interceptor>org.jboss.proxy.ClientMethodInterceptor</interceptor><xsl:text>
             </xsl:text><interceptor>org.jboss.invocation.InvokerInterceptor</interceptor><xsl:text>
          </xsl:text></interceptors><xsl:text>
       </xsl:text></attribute><xsl:text>
       </xsl:text><depends>jboss:service=invoker,type=http</depends><xsl:text>
         </xsl:text></mbean><xsl:text>
      </xsl:text></depends><xsl:text>
   </xsl:text></mbean>
  </xsl:template>

  <!-- Disabling listening Port for NamingService -->
  <xsl:template match="mbean[@name='jboss:service=Naming']/attribute[@name='Port']" mode="http">
      <xsl:comment>Nuxeo patch: mbean[jboss:service=Naming] now does not listen to JNP port</xsl:comment><xsl:text>
         </xsl:text><attribute name='Port'>-1</attribute>
  </xsl:template>
  <!-- The RMI for NamingService should be bound to localhost for security reasons -->
  <xsl:template match="mbean[@name='jboss:service=Naming']/attribute[@name='RmiBindAddress']" mode="http">
      <xsl:comment>Nuxeo patch: mbean[jboss:service=Naming] now does not bind to network</xsl:comment><xsl:text>
         </xsl:text><attribute name='RmiBindAddress'>localhost</attribute>
  </xsl:template>

  <!-- We remove attribute[InvokerURLPrefix] as redundant -->
  <xsl:template match="mbean/attribute[@name='InvokerURLPrefix']" mode="http">
		<xsl:comment>Nuxeo patch: removing attribute[InvokerURLPrefix]</xsl:comment>
  </xsl:template>

  <!-- We remove attribute[InvokerURLPrefix] as evil -->
  <xsl:template match="mbean/attribute[@name='UseHostName']" mode="http">
		<xsl:comment>Nuxeo patch: removing attribute[UseHostName]</xsl:comment>
  </xsl:template>

  <!-- We replace attribute[InvokerURLSuffix] and attribute[InvokerURL] with proper attribute[InvokerURL] -->
  <xsl:template match="mbean/attribute[@name='InvokerURLSuffix']" mode="http">
		<xsl:comment>Nuxeo patch: replacing attribute[InvokerURLSuffix]</xsl:comment><xsl:text>
         </xsl:text><attribute name="InvokerURL"><xsl:value-of select="concat($protocol,'://',$hostname,':',$port,'/invoker/',substring-after(text(),'/invoker/'))"/></attribute>
  </xsl:template>

  <!-- We replace attribute[InvokerURLSuffix] and attribute[InvokerURL] with proper attribute[InvokerURL] -->
  <xsl:template match="mbean/attribute[@name='InvokerURL']" mode="http">
		<xsl:comment>Nuxeo patch: replacing attribute[InvokerURL]</xsl:comment><xsl:text>
         </xsl:text><attribute name="InvokerURL"><xsl:value-of select="concat($protocol,'://',$hostname,':',$port,'/invoker/',substring-after(text(),'/invoker/'))"/></attribute>
  </xsl:template>


  <!-- Making sure EAR deployer depends on HTTPS invoker -->
  <xsl:template match="mbean[@name='jboss.j2ee:service=EARDeployer']" mode="http">
     <xsl:comment>Nuxeo patch: mbean[jboss.j2ee:service=EARDeployer]: removing redundant attribute</xsl:comment><xsl:text>
   </xsl:text><mbean code="org.nuxeo.runtime.jboss.deployment.NuxeoDeployer" name="jboss.j2ee:service=EARDeployer"><xsl:text>
      </xsl:text><xsl:comment><xsl:text> A flag indicating if ear deployments should have their own scoped
      class loader to isolate their classes from other deployments.
      </xsl:text></xsl:comment><xsl:text>
      </xsl:text><attribute name="Isolated">false</attribute><xsl:text>
      </xsl:text><xsl:comment><xsl:text>A flag indicating if the ear components should have in VM call
      optimization disabled.
      </xsl:text></xsl:comment><xsl:text>
      </xsl:text><attribute name="CallByValue">false</attribute><xsl:text>
      </xsl:text><xsl:comment><xsl:text> NXJBossExtensions: A flag indicating that the preprocessor
           must always run before deploying the EAR.
           On live systems you may want to avoid running the preprocessor
           each time, and run it manually after modifying the EAR
           either from the JMX console or by deleting the file
           ".predeploy" and restarting JBoss.
      </xsl:text></xsl:comment><xsl:text>
      </xsl:text><attribute name="Debug">true</attribute><xsl:text>
      </xsl:text><depends>jboss:service=invoker,type=http</depends><xsl:text>
   </xsl:text></mbean>
  </xsl:template>

  <!-- Updating EJB3 connector -->
  <xsl:template match="mbean[@name='jboss.remoting:type=Connector,name=DefaultEjb3Connector,handler=ejb3']/attribute[@name='InvokerLocator']" mode="http">
      <xsl:comment>Nuxeo patch: mbean[jboss.j2ee:service=EARDeployer] now depends on HTTP invoker</xsl:comment><xsl:text>
      </xsl:text><depends>jboss:service=invoker,type=http</depends><xsl:text>
      </xsl:text><attribute name="InvokerLocator"><xsl:value-of select="concat($locator,'://',$hostname,':',$port)"/>/invoker/Ejb3InvokerServlet</attribute>
  </xsl:template>

</xsl:stylesheet>
