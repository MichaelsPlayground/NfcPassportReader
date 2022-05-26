# NFC Passport Reader

This app is based on the work from this repository https://github.com/alimertozdemir/EPassportNFCReader.

All dependencies are up to date on May 26th 2022, the app runs on SDK >= 23 and is developed 
using Android SDK 32.

To read data from passports you need to provide three data fields:

- the passport number
- the birth date in format YYMMDD, e.g. 651023 for a birth day October 23rd 1965
- the expiration date in format YYMMDD, e.g. 251017 for an expiration day October 17th 2025

Mostly available are the data from data groud ("DG") 1 and 2, the other data groups require a more 
complex authorization that is e.g. available only for authorities like border patrol. 

As the reading of the data takes a lot of time (can be up to a minute) it is important to LAY 
the smartphone on the passport and DO NOT MOVE the device to avoid a tag lost exception.

I could only test the app with a German passport - kindly note that a German ID card ("Personalausweis") 
is not supported.

Dependencies to load:

```plaintext
// save and load passport data to Encrypted Shared Preferences
implementation 'androidx.security:security-crypto:1.0.0'
// https://mvnrepository.com/artifact/org.jmrtd/jmrtd
implementation group: 'org.jmrtd', name: 'jmrtd', version: '0.7.32'
// https://mvnrepository.com/artifact/edu.ucar/jj2000
implementation group: 'edu.ucar', name: 'jj2000', version: '5.2'
implementation 'com.github.mhshams:jnbis:1.1.0'
// https://mvnrepository.com/artifact/net.sf.scuba/scuba-sc-android
implementation group: 'net.sf.scuba', name: 'scuba-sc-android', version: '0.0.23'
```

AndroidManifest.xml - add these permissions:

```plaintext
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.VIBRATE" />
```

The app allows to store or load one passport data set for convenience. The data are stored 
in **Encrypted Shared Preferences** in internal app storage.

```plaintext

```

