<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- copy with attributes, and keep comments -->
  <xsl:template match="@*|node()|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()" />
    </xsl:copy>
  </xsl:template>

  <!-- add allowMultipleLastResource for jta -->

  <xsl:template
    match="properties[@name='jta']/property[@name='com.arjuna.ats.jta.allowMultipleLastResources']">
  </xsl:template>

  <xsl:template match="properties[@name='jta']/property[@name='com.arjuna.ats.jta.supportSubtransactions']">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()" />
    </xsl:copy>
    <xsl:text>
    </xsl:text>
    <property name="com.arjuna.ats.jta.allowMultipleLastResources" value="true" />
    <xsl:text>
    </xsl:text>
  </xsl:template>

</xsl:stylesheet>
