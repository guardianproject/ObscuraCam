<html>
	<head>
		<title>CameraObscura - Metadata Schema (version 1.0) - 2/8/11</title>
		<link rel="stylesheet" style="text/css" href="../docstyle.css" />
	</head>
<body>
<h1>Subject</h1>
<p>Child of <a href="../Consent">Consent</a> object</p>
<table>
	<tr>
		<td class="thead" width="100">Name</td>
		<td class="thead" width="500">Description</td>
		<td class="thead" width="60">Datatype</td>
	</tr>
	<tr valign="top">
		<td><a name="EntityName">EntityName</a></td>
		<td>Name of person ID'ed as a subject. Can be null for anonymity.</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="InformedConsentGiven">InformedConsentGiven</a></td>
		<td>Declares whether the subject has given consent to appear in media object.</td>
		<td>boolean</td>
	</tr>
	<tr valign="top">
		<td><a name="ConsentTimecode">ConsentTimecode</a></td>
		<td>If media type is video, this will direct viewer to exact moment in video where the subject registers his/her consent.  Null if media type is image.</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="ObfuscationType">ObfuscationType</a></td>
		<td>Indicates the app-generated method by which subject is obfuscated (blur/pixelate/blocked-out/etc.)</td>
		<td>integer</td>
	</tr>
</table>

</body>
</html>