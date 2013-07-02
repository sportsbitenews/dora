<?xml version="1.0" encoding="UTF-8"?>
<!--

Copyright 2013 Cameron Gregor
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xp="http://www.ibm.com/xsp/core">

  <xsl:output indent="yes"/>
  <xsl:strip-space elements="*"/>

	<!-- Remove the Version and Branch from the version custom control --> 
	<xsl:template match="//xp:text[@id='sourceVersion']/@value"/>
	<xsl:template match="//xp:text[@id='sourceBranch']/@value"/>
	
  <xsl:template match="node() | @*" name="identity">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
    </xsl:copy>
  </xsl:template> 

</xsl:stylesheet>