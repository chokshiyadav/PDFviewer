<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="A4"
                                       page-height="29.7cm"
                                       page-width="21cm"
                                       margin="2cm">
                    <fo:region-body/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="A4">
                <fo:flow flow-name="xsl-region-body">
                    <xsl:apply-templates select="//book"/>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <!-- Book -->
    <xsl:template match="book">
        <xsl:apply-templates select="chapter"/>
    </xsl:template>

    <!-- Chapter -->
    <xsl:template match="chapter">
        <!-- English chapter title -->
        <fo:block font-size="16pt" font-weight="bold" space-before="20pt" space-after="4pt">
            <xsl:value-of select="title_en"/>
        </fo:block>

        <!-- Hindi chapter title -->
        <fo:block font-size="14pt" font-family="Mangal, Arial" space-before="4pt" space-after="10pt">
            <xsl:value-of select="title"/>
        </fo:block>

        <xsl:apply-templates select="section"/>
    </xsl:template>

    <!-- Section -->
    <xsl:template match="section">
        <!-- English section title -->
        <fo:block font-size="14pt" font-weight="bold" space-before="16pt" space-after="4pt">
            <xsl:value-of select="title_en"/>
        </fo:block>

        <!-- Hindi section title -->
        <fo:block font-size="12pt" font-family="Mangal, Arial" space-before="4pt" space-after="8pt">
            <xsl:value-of select="title"/>
        </fo:block>

        <xsl:apply-templates select="paragraph | image"/>
    </xsl:template>

    <!-- Paragraph -->
    <xsl:template match="paragraph">
        <!-- English paragraph -->
        <fo:block font-size="11pt" space-before="4pt" space-after="2pt">
            <xsl:value-of select="text_en"/>
        </fo:block>

        <!-- Hindi translation paragraph -->
        <fo:block font-size="11pt" font-family="Mangal, Arial" space-before="15pt" space-after="15pt">
            <xsl:value-of select="text"/>
        </fo:block>
    </xsl:template>

    <!-- Image -->
    <xsl:template match="image">
        <xsl:choose>
            <xsl:when test="@src">
                <fo:block text-align="center" space-before="8pt" space-after="8pt">
                    <fo:external-graphic src="{@src}" content-width="20%" scaling="uniform"/>
                </fo:block>
            </xsl:when>
            <xsl:otherwise>
                <fo:block font-style="italic" font-size="10pt" color="gray">
                    <xsl:value-of select="."/>
                </fo:block>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
