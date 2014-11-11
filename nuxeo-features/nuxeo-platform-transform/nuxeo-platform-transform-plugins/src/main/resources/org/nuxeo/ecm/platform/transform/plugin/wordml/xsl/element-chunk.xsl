	<xsl:template match="${field_name}">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:text>${field_value}</xsl:text>
		</xsl:copy>
	</xsl:template>
