<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="protocol" select="jrmp"/>

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

  <xsl:template match="@*|node()|comment()" mode="http">
  <!-- This template leaves source XML almost intact -->
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()" mode="http"/>
    </xsl:copy>
  </xsl:template>

  <!-- Removing previous invoker specification -->
  <xsl:template match="local-tx-datasource/jmx-invoker-name" mode="http">
  	<xsl:comment>Nuxeo patch: previous invoker specification removed</xsl:comment>
  </xsl:template>
  <xsl:template match="local-tx-datasource/jmx-invoker-name" mode="jrmp">
  	<xsl:comment>Nuxeo patch: previous invoker specification removed</xsl:comment>
  </xsl:template>

  <!-- Adding JMX invoker specification -->
  <xsl:template match="local-tx-datasource/jndi-name" mode="http">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy><xsl:text>
    </xsl:text><xsl:comment>Nuxeo patch: mandatory element jndi-name is appended with jmx-invoker-name</xsl:comment><xsl:text>
    </xsl:text><jmx-invoker-name>jboss:service=invoker,type=http</jmx-invoker-name>
  </xsl:template>

  <!-- Removing previous invoker specification -->
  <xsl:template match="xa-datasource/jmx-invoker-name" mode="jrmp">
  	<xsl:comment>Nuxeo patch: previous invoker specification removed</xsl:comment>
  </xsl:template>

  <xsl:template match="xa-datasource/jmx-invoker-name" mode="http">
  	<xsl:comment>Nuxeo patch: previous invoker specification removed</xsl:comment>
  </xsl:template>

  <!-- Adding JMX invoker specification -->
  <xsl:template match="xa-datasource/jndi-name" mode="http">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy><xsl:text>
    </xsl:text><xsl:comment>Nuxeo patch: mandatory element jndi-name is appended with jmx-invoker-name</xsl:comment><xsl:text>
    </xsl:text><jmx-invoker-name>jboss:service=invoker,type=http</jmx-invoker-name>
  </xsl:template>

</xsl:stylesheet>
