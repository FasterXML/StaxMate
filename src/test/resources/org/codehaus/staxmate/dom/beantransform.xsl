<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>

<!-- identity transform -->
<!--https://stackoverflow.com/questions/2228004/how-do-i-pretty-print-an-xslt-result-document-with-removed-source-elements-->
    <xsl:template match="node() | @*">
        <xsl:copy>
            <!-- select everything except blank text nodes -->
            <xsl:apply-templates select="
      node()[not(self::text())] | text()[normalize-space() != ''] | @*
    " />
        </xsl:copy>
    </xsl:template>
    <!-- change the class name of an object -->
    <xsl:template match="//object[@class='de.elbosso.scratch.xml.Person']">
        <xsl:choose>
            <xsl:when test="number(./void[@property='age']/double) &lt; 90">
                <object>
                    <xsl:attribute name="class">de.elbosso.scratch.xml.Human</xsl:attribute>
                    <xsl:apply-templates/>
                </object>
            </xsl:when>
            <xsl:otherwise>
                <object>
                    <xsl:attribute name="class">de.elbosso.scratch.xml.Senior</xsl:attribute>
                    <xsl:copy-of select="node()">
                    </xsl:copy-of>
                </object>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- change the class name of an object and add a default constructor parameter -->
    <xsl:template match="//object[@class='de.elbosso.scratch.xml.TraditionalPCouple']">
        <object>
            <xsl:attribute name="class">de.elbosso.scratch.xml.TraditionalHCouple</xsl:attribute>
            <xsl:apply-templates/>
            <object>
                <xsl:attribute name="class">java.util.LinkedList</xsl:attribute>
            </object>
        </object>
    </xsl:template>
    <!-- build a property value out of two other property values -->
    <xsl:template match="//object[@class='de.elbosso.scratch.xml.Person']/void[@property='firstName']">
        <xsl:choose>
            <xsl:when test="number(../void[@property='age']/double) &lt; 90">
                <void>
                    <xsl:attribute name="property">name</xsl:attribute>
                    <string>
                        <xsl:value-of select="../void[@property='lastName']/string"/><xsl:text>, </xsl:text><xsl:value-of
                            select="./string"/>
                    </string>
                </void>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select=".">
                </xsl:copy-of>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--remove a property-->
    <xsl:template match="//object[@class='de.elbosso.scratch.xml.Person']/void[@property='lastName']"/>

</xsl:stylesheet>
