package org.witness.sscphase1.secure;

public class GPGExportActivity {

}

/*
 * 
Updated Jul 09, 2010 by thialfi...@gmail.com
Labels:	Development
   UsingApgForDevelopment  
Some info on how to use APG in other apps.

As an example one can look at the implementation in K9, currently here: http://code.google.com/p/k9mail/source/browse/k9mail/branches/apg-integration/src/com/fsck/k9/crypto/Apg.java

A lot of this may still change. It's all under development. Suggestions are welcome.

Intents
Apg.Intent.ENCRYPT_AND_RETURN
Encrypt data and return the result.

Intent.getData() (Uri), Apg.EXTRA_DATA (byte), Apg.EXTRA_TEXT (String) - data source
Apg.EXTRA_ASCII_ARMOUR (boolean) - use ASCII armour
true: if EXTRA_DATA or EXTRA_TEXT were used, then the data is returned with Apg.EXTRA_ENCRYPTED_TEXT
false: EXTRA_DATA or EXTRA_TEXT were used, then the data is returned with Apg.EXTRA_ENCRYPTED_DATA
Apg.EXTRA_ENCRYPTION_KEY_IDS (long) - the list of encryption keys
Apg.EXTRA_SIGNATURE_KEY_ID (long) - the signature key
returns Apg.EXTRA_ENCRYPTED_DATA (byte) or Apg.EXTRA_ENCRYPTED_TEXT (String)
Apg.Intent.DECRYPT_AND_RETURN
Decrypt data and return the result.

Intent.getData() (Uri), Apg.EXTRA_DATA (byte), Apg.EXTRA_TEXT (String) - data source
Apg.EXTRA_BINARY (boolean) - return the decrypted data as byte array
true: the data is returned with Apg.EXTRA_DECRYPTED_DATA
false: the data is returned with Apg.EXTRA_DECRYPTED_TEXT
returns Apg.EXTRA_DECRYPTED_DATA (byte) or Apg.EXTRA_DECRYPTED_TEXT (String)
if signed:
returns Apg.EXTRA_SIGNATURE_KEY_ID (long) - the signature key ID
returns Apg.EXTRA_SIGNATURE_USER_ID (String) - the user ID of the signature
returns Apg.EXTRA_SIGNATURE_SUCCESS (boolean) - signature is correct
returns Apg.EXTRA_SIGNATURE_UNKNOWN (boolean) - signature is unknown
Apg.Intent.SELECT_SECRET_KEY
Select a secret key from the list and return its ID.

result returned in Apg.EXTRA_SECRET_KEY (long)
Apg.Intent.SELECT_PUBLIC_KEYS
Select some public keys from the list and return their IDs.

Apg.EXTRA_SELECTION (long) - pass a list of IDs for keys that are selected already
result returned in Apg.EXTRA_SELECTION (long)
*/
