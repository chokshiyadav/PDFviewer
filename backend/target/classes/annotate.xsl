<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:template match="/">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="A4" page-height="29.7cm" page-width="21cm" margin="2cm">
                    <fo:region-body/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="A4">
                <fo:flow flow-name="xsl-region-body">

                    <!-- Book Title -->
                    <fo:block font-size="20pt" font-weight="bold" color="#2E86C1" text-align="center" space-after="15pt">
                        Book
                    </fo:block>

                    <!-- Loop through all chapters -->
                    <xsl:for-each select="book/chapter">
                        <fo:block font-size="16pt" font-weight="bold" color="#117A65" space-before="20pt" space-after="8pt">
                            <xsl:value-of select="@title"/>
                        </fo:block>

                        <!-- Loop through all sections -->
                        <xsl:for-each select="section">
                            <fo:block font-size="13pt" font-weight="bold" color="#D35400" space-before="15pt" space-after="6pt">
                                <xsl:value-of select="@title"/>
                            </fo:block>

                            <!-- Maintain order of paragraphs and images -->
                            <xsl:for-each select="*">
                                <xsl:choose>
                                    <!-- Paragraph -->
                                    <xsl:when test="self::paragraph">
                                        <fo:block font-size="10pt" color="#555555" space-before="5pt" linefeed-treatment="ignore"
                                                  white-space-collapse="true" white-space-treatment="ignore-if-surrounding">
                                            <xsl:value-of select="normalize-space(.)"/>
                                        </fo:block>
                                    </xsl:when>

                                    <!-- Image -->
                                    <xsl:when test="self::image">
                                        <fo:block text-align="center" space-before="10pt" space-after="10pt">
                                            <fo:external-graphic src="{@src}" content-width="25%" content-height="auto" scaling="uniform"/>
                                        </fo:block>
                                    </xsl:when>
                                </xsl:choose>
                            </xsl:for-each>
                        </xsl:for-each>
                    </xsl:for-each>

                    <!-- Notes Section -->
                    <fo:block font-size="15pt" font-weight="bold" color="#8E44AD" space-before="30pt" space-after="15pt" text-align="center">
                        Notes
                    </fo:block>

                    <!-- Notes Table -->
                    <fo:table table-layout="fixed" width="100%" font-size="10pt" border="solid 1pt #D5D8DC">
                        <fo:table-column column-width="5%"/>
                        <fo:table-column column-width="95%"/>
                        <fo:table-body>
                            <xsl:for-each select="book/annotation">
                                <fo:table-row>
                                    <!-- Annotation Number -->
                                    <fo:table-cell border="solid 0.5pt #AAB7B8" padding="5pt">
                                        <fo:block font-weight="bold" color="#1F618D">
                                            <xsl:value-of select="position()"/>.
                                        </fo:block>
                                    </fo:table-cell>

                                    <!-- Annotation Text -->
                                    <fo:table-cell border="solid 0.5pt #AAB7B8" padding="5pt">
                                        <fo:block color="#34495E">
                                            <xsl:value-of select="normalize-space(.)"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </xsl:for-each>
                        </fo:table-body>
                    </fo:table>

                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

</xsl:stylesheet>
