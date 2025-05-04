<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" indent="yes"/>

    <xsl:template match="/">
        <html>
            <head>
                <title>Book</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background: #f9f9f9; }
                    h1 { color: #2E86C1; font-size: 28px; }
                    h2 { color: #117A65; margin-top: 30px; }
                    h3 { color: #D35400; margin-top: 20px; }
                    p { color: #333333; font-size: 14px; margin-top: 10px; }
                    img { margin-top: 10px; max-width: 300px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 30px; }
                    th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                    th { background-color: #2E86C1; color: white; }
                    td { background-color: #ffffff; }
                </style>
            </head>

            <body>

                <!-- Loop through chapters -->
                <xsl:for-each select="book/chapter">
                    <h2><xsl:value-of select="@title"/></h2>

                    <!-- Loop through sections inside chapter -->
                    <xsl:for-each select="section">
                        <h3><xsl:value-of select="@title"/></h3>

                        <!-- Loop through paragraph/image -->
                        <xsl:for-each select="*">
                            <xsl:choose>
                                <xsl:when test="self::paragraph">
                                    <p><xsl:value-of select="."/></p>
                                </xsl:when>

                                <xsl:when test="self::image[@src]">
                                    <img>
                                        <xsl:attribute name="src">
                                            <xsl:value-of select="@src"/>
                                        </xsl:attribute>
                                    </img>
                                </xsl:when>

                                <xsl:when test="self::image[not(@src)]">
                                    <p style="color:#888;"><em><xsl:value-of select="."/></em></p>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>

                    </xsl:for-each>
                </xsl:for-each>
        <h2>Notes</h2>
        <table>
            <tr>
                <th style="width:10%;">Annotation No.</th>
                <th>Content</th>
            </tr>
            <xsl:for-each select="book/annotation">
                <tr>
                    <td><xsl:value-of select="position()"/></td>
                    <td><xsl:value-of select="."/></td>
                </tr>
            </xsl:for-each>
        </table>


            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
