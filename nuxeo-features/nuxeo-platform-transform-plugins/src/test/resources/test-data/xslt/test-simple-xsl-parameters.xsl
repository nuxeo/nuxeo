<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:param name="param1" />
  <xsl:param name="param2" />
  <xsl:param name="param3" />

  <xsl:output method="xml"
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
     doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>
  <!--****************************************************************
      ** "/" template
      *************************************************************-->
  <xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml">
      <head>
        <title>Test Parameters</title>
      </head>
      <body>
<h2>Hello</h2>
<table>
  <tbody>
    <tr>
      <td>Parameter 1:</td>
      <td><xsl:value-of select="$param1" /></td>
    </tr>
    <tr>
      <td>Parameter 2:</td>
      <td><xsl:value-of select="$param2" /></td>
    </tr>
    <tr>
      <td>Parameter 3:</td>
      <td><xsl:value-of select="$param3" /></td>
    </tr>
  </tbody>
</table>
</body>
</html>
</xsl:template>

</xsl:stylesheet>
