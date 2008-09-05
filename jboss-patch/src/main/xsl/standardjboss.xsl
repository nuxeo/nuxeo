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


  <!-- Dirty patch -->
  <xsl:template match="invoker-proxy-binding/name[contains(text(),'-rmi-')]" mode="http">
      <xsl:comment>Nuxeo patch: invoker proxy binding renamed</xsl:comment><xsl:text>
      </xsl:text><name><xsl:value-of select="concat(substring-before(text(),'-rmi-'), '-http-',substring-after(text(),'-rmi-'))"/></name>
  </xsl:template>
  <xsl:template match="invoker-proxy-binding/invoker-mbean[contains(text(),'jrmp')]" mode="http">
      <xsl:comment>Nuxeo patch: invoker proxy binding renamed</xsl:comment><xsl:text>
      </xsl:text><invoker-mbean><xsl:value-of select="concat(substring-before(text(),'jrmp'), 'http',substring-after(text(),'jrmp'))"/></invoker-mbean>
  </xsl:template>

  <xsl:template match="invoker-proxy-binding-name[contains(text(),'-rmi-')]" mode="http">
      <xsl:comment>Nuxeo patch: invoker proxy binding renamed</xsl:comment><xsl:text>
      </xsl:text><invoker-proxy-binding-name><xsl:value-of select="concat(substring-before(text(),'-rmi-'), '-http-',substring-after(text(),'-rmi-'))"/></invoker-proxy-binding-name>
  </xsl:template>

</xsl:stylesheet>
