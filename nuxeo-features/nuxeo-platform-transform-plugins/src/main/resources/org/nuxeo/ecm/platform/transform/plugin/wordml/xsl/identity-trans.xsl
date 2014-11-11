<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
	version="1.0" 
	xmlns:w="http://schemas.microsoft.com/office/word/2003/wordml" 
	xmlns:o="urn:schemas-microsoft-com:office:office">
	
	<xsl:output method="xml" indent="yes" encoding="iso-8859-1"/>
	
	<xsl:preserve-space elements="*" />

${fields_value_inject_templates}

	<!-- This is a simple identity function -->

	<xsl:template match="@*|*|processing-instruction()|comment()"
		priority="-2">
		<xsl:copy>
			<xsl:apply-templates
				select="*|@*|text()|processing-instruction()|comment()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>
