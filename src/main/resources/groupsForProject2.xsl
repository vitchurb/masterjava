<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" omit-xml-declaration="yes" indent="no"/>
    <xsl:param name="projectId" />
    <xsl:strip-space elements="*"/>

    <xsl:template match="/">
        <html>
            <body>
                <table>
                    <tr>
                        <td>Name</td>
                        <td>State</td>
                    </tr>
                    <xsl:apply-templates/>
                </table>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="/*[name()='Payload']/*[name()='Groups']/*[name()='Group' and @project = $projectId]">
                    <tr>
                        <td>
                            <xsl:value-of select="."/>
                        </td>
                        <td>
                            <xsl:value-of select="@state"/>
                        </td>
                    </tr>

        <xsl:text>&#xa;</xsl:text><!-- put in the newline -->

    </xsl:template>
    <xsl:template match="text()"/>
</xsl:stylesheet>