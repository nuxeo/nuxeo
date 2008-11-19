<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output doctype-system="log4j.dtd"/>

  <!-- copy with attributes, and keep comments -->
  <xsl:template match="@*|node()|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()"/>
    </xsl:copy>
  </xsl:template>



  <!-- remove previous declarations (if there) so that they don't end up
       twice in the file
  -->
  <xsl:template match="category[@name='org.jboss.seam']">
  </xsl:template>
  <xsl:template match="category[@name='org.jboss.ejb3']">
  </xsl:template>
  <xsl:template match="category[@name='org.jboss.ejb3.cache.simple.StatefulSessionFilePersistenceManager']">
  </xsl:template>
  <xsl:template match="category[@name='org.jboss.seam.contexts.Lifecycle']">
  </xsl:template>
  <xsl:template match="category[@name='org.jboss.mx.loading']">
  </xsl:template>
  <xsl:template match="category[@name='org.ajax4jsf']">
  </xsl:template>
  <xsl:template match="category[@name='org.hibernate']">
  </xsl:template>
  <xsl:template match="category[@name='org.hibernate.engine.StatefulPersistenceContext.ProxyWarnLog']">
  </xsl:template>
  <xsl:template match="category[@name='org.hibernate.cache.EhCacheProvider']">
  </xsl:template>
  <xsl:template match="category[@name='org.hibernate.hql.ast.tree.FromElementType']">
  </xsl:template>
  <xsl:template match="category[@name='org.jbpm']">
  </xsl:template>
  <xsl:template match="category[@name='org.jbpm.jpdl.xml.JpdlXmlReader']">
  </xsl:template>
  <xsl:template match="category[@name='org.compass.core.transaction']">
  </xsl:template>
  <xsl:template match="category[@name='org.compass.core.lucene.engine.optimizer.ScheduledLuceneSearchEngineOptimizer']">
  </xsl:template>
  <xsl:template match="category[@name='org.apache.jackrabbit.core.query.lucene.IndexMerger']">
  </xsl:template>
  <xsl:template match="category[@name='org.nuxeo.ecm.platform.ui.web.auth']">
  </xsl:template>
  <xsl:template match="category[@name='org.apache.myfaces.renderkit.html.util.DefaultAddResource']">
  </xsl:template>
  <xsl:template match="category[@name='javax.enterprise.resource.webcontainer.jsf.renderkit']">
  </xsl:template>
  <xsl:template match="category[@name='javax.enterprise.resource.webcontainer.jsf.application']">
  </xsl:template>
  <xsl:template match="category[@name='javax.enterprise.resource.webcontainer.jsf.lifecycle']">
  </xsl:template>

  <!-- reinsert after org.apache category -->
  <xsl:template match="category[@name='org.apache']">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
    <xsl:text>
   </xsl:text>
    <category name="org.jboss.seam">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.jboss.ejb3">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.jboss.ejb3.cache.simple.StatefulSessionFilePersistenceManager">
      <priority value="DEBUG"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.jboss.seam.contexts.Lifecycle">
      <priority value="WARN"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.jboss.mx.loading">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.ajax4jsf">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.hibernate">
      <priority value="WARN"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.hibernate.engine.StatefulPersistenceContext.ProxyWarnLog">
      <priority value="ERROR"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.hibernate.cache.EhCacheProvider">
      <priority value="ERROR"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.hibernate.hql.ast.tree.FromElementType">
      <priority value="ERROR"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.jbpm">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.jbpm.jpdl.xml.JpdlXmlReader">
      <priority value="ERROR"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.compass.core.transaction">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.compass.core.lucene.engine.optimizer.ScheduledLuceneSearchEngineOptimizer">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.apache.jackrabbit.core.query.lucene.IndexMerger">
      <priority value="WARN"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.nuxeo.ecm.platform.ui.web.auth">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="org.apache.myfaces.renderkit.html.util.DefaultAddResource">
      <priority value="ERROR"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="javax.enterprise.resource.webcontainer.jsf.renderkit">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="javax.enterprise.resource.webcontainer.jsf.application">
      <priority value="INFO"/>
    </category>
    <xsl:text>
   </xsl:text>
    <category name="javax.enterprise.resource.webcontainer.jsf.lifecycle">
      <priority value="INFO"/>
    </category>
  </xsl:template>

</xsl:stylesheet>
