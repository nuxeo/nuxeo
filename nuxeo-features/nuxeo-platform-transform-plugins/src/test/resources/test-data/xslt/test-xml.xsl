<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml"
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
     doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>
  <!--****************************************************************
      ** "/" template
      *************************************************************-->
  <xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml">
      <head>
        <title>Schedule</title>
      </head>
      <body>
        <h2 align="center">
          <xsl:value-of select="schedule/owner/name/first"/>
          <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
          <xsl:value-of select="schedule/owner/name/last"/>'s Schedule</h2>
        <xsl:apply-templates select="schedule/appointment"/>
      </body>
    </html>
  </xsl:template>
  <!--***************************************************************
      ** "appointment" template
      ************************************************************-->
  <xsl:template match="appointment">
    <hr/>
    <h3>Appointment</h3>
    <xsl:apply-templates select="when"/>
    <table>
      <tr>
        <td>Subject:</td>
        <td>
          <xsl:value-of select="subject"/>
        </td>
      </tr>
      <tr>
        <td>Location:</td>
        <td>
          <xsl:value-of select="location"/>
        </td>
      </tr>
      <tr>
        <td>Note:</td>
        <td>
          <xsl:value-of select="note"/>
        </td>
      </tr>
    </table>
  </xsl:template>
  <!--****************************************************************
       ** "when" template
       *************************************************************-->
  <xsl:template match="when">
    <p>
      <xsl:value-of select="date/@month"/>
      <xsl:text>/</xsl:text>
      <xsl:value-of select="date/@day"/>
      <xsl:text>/</xsl:text>
      <xsl:value-of select="date/@year"/>
      from
      <xsl:value-of select="startTime/@hour"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="startTime/@minute"/>
      until
      <xsl:value-of select="endTime/@hour"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="endTime/@minute"/>
    </p>
  </xsl:template>
</xsl:stylesheet>
