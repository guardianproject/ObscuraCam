<html>
	<head>
		<title>CameraObscura - Metadata Schema (version 1.0) - 2/8/11</title>
		<link rel="stylesheet" style="text/css" href="../docstyle.css" />
	</head>
<body>
<h1>Geneaology</h1>
<p>Child of <a href="../Object">the Object</a>.</p>

<table>
	<tr>
		<td class="thead" width="100">Name</td>
		<td class="thead" width="500">Description</td>
		<td class="thead" width="60">Datatype</td>
	</tr>
	<tr valign="top">
		<td><a name="Origin">Origin</a></td>
		<td>Declares whether media object was taked with the app, pulled in from the gallery, or from another source</td>
		<td>integer</td>
	</tr>
	<tr valign="top">
		<td><a name="ParentEntity">ParentEntity</a></td>
		<td>If media object's origin is not the app itself, declare the Intent:Ownership definition from the object's metadata</td>
		<td>varchar</td>
	</tr>
	<tr valign="top">
		<td><a name="DateAcquired">DateAcquired</a></td>
		<td>If not at time of creation, declare the Data:Tag_Datetime definition from object's metadata</td>
		<td>datetime</td>
	</tr>
	<tr valign="top">
		<td><a name="SourceService">SourceService</a></td>
		<td>If not the app itself, declare the Data:Software definition from the object's metadata</td>
		<td>varchar</td>
	</tr>
</table>

</body>
</html>