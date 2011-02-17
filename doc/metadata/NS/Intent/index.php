<html>
	<head>
		<title>CameraObscura - Metadata Schema (version 1.0) - 2/8/11</title>
		<link rel="stylesheet" style="text/css" href="../docstyle.css" />
	</head>
<body>
<h1>Intent</h1>
<p>Child of the <a href="../Object">Object</a> object.</p>
<table>
	<tr>
		<td class="thead" width="100">Name</td>
		<td class="thead" width="500">Description</td>
		<td class="thead" width="60">Datatype</td>
	</tr>
	<tr valign="top">
		<td><a name="OwnershipType">OwnershipType</a></td>
		<td>Attribute of Intent.<br />Is content owner individual or with an organization?</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="Ownership">Ownership</a></td>
		<td>Attribute of Intent.<br />Name of person/org app is registered to.<br />Can be null for anonymity</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="SecurityLevel">SecurityLevel</a></td>
		<td>Element of Intent.<br />Indicates how locked-down media is, according to predefinied options.  (TBD)</td>
		<td>integer</td>
	</tr>
	<tr valign="top">
		<td><a name="PublicKey">PublicKey</a></td>
		<td>Element of Intent.<br />If not null, this is the key used to restore media to unadultured state.</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="SociallySharable">SociallySharable</a></td>
		<td>Element of Intent.<br />If not false, this image will be able to be shared according to Android gallery intents (Email, Flickr, Bump, etc..)</td>
		<td>boolean</td>
	</tr>
</table>

</body>
</html>