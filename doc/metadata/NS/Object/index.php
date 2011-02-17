<html>
	<head>
		<title>CameraObscura - Metadata Schema (version 1.0) - 2/8/11</title>
		<link rel="stylesheet" style="text/css" href="../docstyle.css" />
	</head>
<body>
<h1>Introduction</h1>
<p>This is the first draft of the SSC/CameraObscura metadata schematic.  <b>NB:</b> The Data objects are at this point blank-- this is where this namespace will employ pre-existing schemas (RDF, Exif, and others) which are still being studied for consideration.</p>
<p>The proposed schema aims to be as flexible as possible in regards to different media types.  Admittedly, its language does seem to favor video, but the object's definitions and prescription for usage is definitely applicable to still images as well.  As a general rule, any object that would rationally only apply to video can receive a null value in the case of a still image.</p>
<p>Here is an <a href="../References/ssc-md_v1.xml">XML representation</a> of this schema.  The root of our namespace is "http://witness.org" but of course this is subject to change.</p>

<h1>Declarations within the Object object</h1>
<ul>
	<li><a href="../Intent/">Intent</a>
		<ul>
			<li><a href="../Intent/#Ownership">Ownership</a></li>
			<li><a href="../Intent/#OwnershipType">OwnershipType</a></li>
			<li><a href="../Intent/#SecurityLevel">SecurityLevel</a></li>
			<li><a href="../Intent/#PublicKey">PublicKey</a></li>
			<li><a href="../Intent/#SociallySharable">SociallySharable</a></li>
		</ul>
	</li>
	<li><a href="../Consent/">Consent</a>
		<ul>
			<li><a href="../Consent/#NumTags">NumTags</a></li>
			<li><a href="../Subject/">Subject</a>
				<ul>
					<li><a href="../Subject/#EntityName">EntityName</a></li>
					<li><a href="../Subject/#InformedConsentGiven">InformedConsentGiven</a></li>
					<li><a href="../Subject/#ConsentTimecode">ConsentTimecode</a></li>
					<li><a href="../Subject/#ObfuscationType">ObfuscationType</a></li>
					<li><a href="../ObfuscationTimeblock">ObfuscationTimeblock</a>
						<ul>
							<li><a href="../ObfuscationTimeblock/#TimecodeStart">TimecodeStart</a></li>
							<li><a href="../ObfuscationTimeblock/#TImecodeEnd">TimecodeEnd</a></li>
							<li><a href="../ObfuscationTimeblock/#InitialCoordinates">InitialCoordinates</a></li>
							<li><a href="../ObjectPath">ObjectPath</a>
								<ul>
									<li><a href="../ObjectPath/#Relevance">Relevance</a></li>
								</ul>
							</li>
						</ul>
					</li>
				</ul>
			</li>
		</ul>
	</li>
	<li><a href="../Data/">Data</a></li>
	<li><a href="../Geneaology/">Geneaology</a>
		<ul>
			<li><a href="../Geneaology/#Origin">Origin</a></li>
			<li><a href="../Geneaology/#ParentEntity">ParentEntity</a></li>
			<li><a href="../Geneaology/#DateAcquired">DateAcquired</a></li>
			<li><a href="../Geneaology/#SourceService">SourceService</a></li>
	</li>
</ul>

</body>