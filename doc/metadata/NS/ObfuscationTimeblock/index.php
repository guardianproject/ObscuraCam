<html>
	<head>
		<title>CameraObscura - Metadata Schema (version 1.0) - 2/8/11</title>
		<link rel="stylesheet" style="text/css" href="../docstyle.css" />
	</head>
<body>
<h1>ObfuscationTimeblock</h1>
<p>Child of the <a href="../Subject">Subject</a> object.</p>
<table>
	<tr>
		<td class="thead" width="100">Name</td>
		<td class="thead" width="500">Description</td>
		<td class="thead" width="60">Datatype</td>
	</tr>
	<tr valign="top">
		<td><a name="TimecodeStart">TimecodeStart</a></td>
		<td>When on the video's timeline this subject first appears (in order to apply obfuscation) if media type is video.  If media type is image, this is null.</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="TimecodeEnd">TimecodeEnd</a></td>
		<td>When on the video's timeline this subject is no longer visible (in order to get rid of obfuscation) if media type is video.  If media type is image, this is null.</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="InitialCoordinates">InitialCoordinates</a></td>
		<td>Where on the image space should the app apply obfuscation?  String contains 4 coordinates, comma-separated, for top-left x and y position and bottom-right x and y position.</td>
		<td>JSON object</td>
	</tr>
</table>

</body>
</html>