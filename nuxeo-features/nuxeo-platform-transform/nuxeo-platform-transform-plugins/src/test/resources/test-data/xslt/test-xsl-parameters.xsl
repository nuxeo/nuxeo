<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


  <xsl:output method="xml"/>
  <xsl:output doctype-system="http://www.w3.org/TR/xhtml1/dtd/xhtml1-transitional.dtd" />
  <xsl:output doctype-public="-//W3C//dtd xhtml 1.0 transitional//EN"/>

  <xsl:param name="param1" />
  <xsl:param name="param2" />
  <xsl:param name="param3" />

  <!--****************************************************************
    ** "/" template
    *************************************************************-->
  <xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml">
      <head>
        <title>Publimap: XSL de test</title>
        <xsl:call-template name="htmlMeta"/>
        <xsl:call-template name="styleCss" />
        <xsl:call-template name="script" />
      </head>
      <body>
        <h1>Publimap: XSL de test</h1>
        <table cellspacing="0" cellpadding="0">
          <thead>
            <tr>
              <th>Paramètre</th>
              <th>valeur</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>$param1</td>
              <td>
                <xsl:value-of select="$param1" />
              </td>
            </tr>
            <tr>
              <td>$param2</td>
              <td>
                <xsl:value-of select="$param2" />
              </td>
            </tr>
            <tr>
              <td>$param3</td>
              <td>
                <xsl:value-of select="$param3" />
              </td>
            </tr>
          </tbody>
        </table>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="styleCss">
    <style type="text/css" media="screen,print">
<![CDATA[
*       {font-size: 10pt; font-family: tahoma;}
h1      {font-size: 11pt;}
table, td, th
        {border: solid #888 1px;}
th      {background-color: #ccc;}
td, th  {padding: 0px 8px;}
.ok, .ko
        {text-align: center;}
.ok     {color: green;}
.ko     {color: red;}
.debug  {color: red; font-family: "lucida console";}
.dossier
        {font-weight: bold;}
]]>
    </style>
  </xsl:template>

  <xsl:template name="htmlMeta">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  </xsl:template>

  <xsl:template name="script"></xsl:template>

</xsl:stylesheet>
