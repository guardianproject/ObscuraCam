<html>
	<head>
		<title>CameraObscura - Metadata Schema (version 1.0) - 2/8/11</title>
		<link rel="stylesheet" style="text/css" href="../docstyle.css" />
	</head>
<body>
<h1>ObjectPath</h1>
<p>Child of <a href="../Subject">Subject</a> group.</p>
<table>
	<tr>
		<td class="thead" width="100">Name</td>
		<td class="thead" width="500">Description</td>
		<td class="thead" width="60">Datatype</td>
	</tr>
	<tr valign="top">
		<td><a name="ObjectPath">ObjectPath</a></td>
		<td>For video only, traces the subject's coordinates along the timeline.  (TBD) If media type is image, this is null.</td>
		<td>JSON object</td>
	</tr>
	<tr valign="top">
		<td><a name="ObjectPath">Relevance</a></td>
		<td>Attribute of ObjectPath.<br />True if media type is video, false if still image.</td>
		<td>boolean</td>
	</tr>
</table>

</body>
</html>