<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!--<xsl:output type="xml" indent="yes" /> -->
  <xsl:variable name="WSSFilterExists" select="count(//*[text()='WSS Filter'])"/>

  <xsl:template match="@*|node()|comment()" >
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|comment()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Adding WSS Filter -->
  <xsl:template match="web-app/servlet-mapping[1]" >
   <xsl:if test="$WSSFilterExists=1">
    <xsl:copy>      
      <xsl:apply-templates select="@*|node()|comment()"/>
    </xsl:copy>
   </xsl:if>
   <xsl:if test="$WSSFilterExists=0">
      <servlet-mapping><xsl:text>
         </xsl:text><servlet-name>Status Servlet</servlet-name><xsl:text>
         </xsl:text><url-pattern>/status</url-pattern><xsl:text>
  </xsl:text></servlet-mapping><xsl:text>
      </xsl:text>
      <xsl:comment>Nuxeo patch: Adding WSS Filter declaration and mapping </xsl:comment><xsl:text>
      </xsl:text><filter><xsl:text>
            </xsl:text><display-name>WSS Filter</display-name><xsl:text>
            </xsl:text><filter-name>WSSFilter</filter-name><xsl:text>
            </xsl:text><filter-class>org.nuxeo.wss.servlet.WSSFilter</filter-class><xsl:text>
            </xsl:text><init-param><xsl:text>
                </xsl:text><param-name>org.nuxeo.wss.rootFilter</param-name><xsl:text>
                </xsl:text><param-value>/nuxeo</param-value><xsl:text>
            </xsl:text></init-param><xsl:text>
        </xsl:text></filter><xsl:text>
        </xsl:text><filter-mapping><xsl:text>
            </xsl:text><filter-name>WSSFilter</filter-name><xsl:text>
            </xsl:text><url-pattern>/*</url-pattern><xsl:text>
        </xsl:text></filter-mapping><xsl:text>
        </xsl:text><filter-mapping><xsl:text>
            </xsl:text><filter-name>WSSFilter</filter-name><xsl:text>
            </xsl:text><url-pattern>/</url-pattern><xsl:text>
        </xsl:text></filter-mapping>
   </xsl:if>      
  </xsl:template>
</xsl:stylesheet>

