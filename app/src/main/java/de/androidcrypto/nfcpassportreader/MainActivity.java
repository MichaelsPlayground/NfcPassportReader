package de.androidcrypto.nfcpassportreader;

import static org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE;
import static org.jmrtd.PassportService.NORMAL_MAX_TRANCEIVE_LENGTH;

import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG12File;
import org.jmrtd.lds.icao.DG15File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG4File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG6File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import org.jmrtd.lds.iso19794.FingerImageInfo;
import org.jmrtd.lds.iso19794.FingerInfo;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    EditText passportNumber, birthDate, expirationDate;
    Button savePassportData, loadPassportData, clearPassportData;
    TextView nfcaContent;
    private ImageView ivPhoto;
    ProgressBar pbNfcReading;
    TextView tvPbNfcReading;
    ScrollView scrollView;

    private NfcAdapter mNfcAdapter;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = findViewById(R.id.scrollViewLayout);
        passportNumber = findViewById(R.id.etPassportNumber);
        birthDate = findViewById(R.id.etPassportBirth);
        expirationDate = findViewById(R.id.etPassportExpiration);
        savePassportData = findViewById(R.id.btnSavePassportData);
        loadPassportData = findViewById(R.id.btnLoadPassportData);
        clearPassportData = findViewById(R.id.btnClearPassportData);
        // init encrypted shared preferences
        EncryptedSharedPreferencesUtils.setupEncryptedSharedPreferences(getApplicationContext());

        nfcaContent = findViewById(R.id.tvNfcaContent);
        ivPhoto = findViewById(R.id.view_photo);

        // progressbar for reading
        pbNfcReading = findViewById(R.id.pbLoadNfc);
        tvPbNfcReading = findViewById(R.id.tvPbLoadNfc);
        pbNfcReading.setVisibility(View.GONE);
        tvPbNfcReading.setVisibility(View.GONE);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        savePassportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String passNumber = passportNumber.getText().toString().toUpperCase();
                String passBirthDate = birthDate.getText().toString();
                String passExpirationDate = expirationDate.getText().toString();
                EncryptedSharedPreferencesUtils.savePassportData(
                        passNumber,
                        passBirthDate,
                        passExpirationDate
                );
                Snackbar snackbar = Snackbar.make(view, "The passport data are saved.", Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), R.color.green));
                snackbar.show();
            }
        });

        loadPassportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passportNumber.setText(EncryptedSharedPreferencesUtils.loadPassportNumber());
                birthDate.setText(EncryptedSharedPreferencesUtils.loadPassportBirthDate());
                expirationDate.setText(EncryptedSharedPreferencesUtils.loadPassportExpirationDate());
            }
        });

        clearPassportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passportNumber.setText("");
                birthDate.setText("");
                expirationDate.setText("");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag after reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {

        IsoDep isoDep = null;
        BACKeySpec bacKey = null;
        EDocument eDocument = new EDocument();
        DocType docType = DocType.OTHER;
        PersonDetails personDetails = new PersonDetails();
        AdditionalPersonDetails additionalPersonDetails = new AdditionalPersonDetails();

        // Whole process is put into a big try-catch trying to catch the transceive's IOException
        try {
            isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText("");
                    pbNfcReading.setVisibility(View.VISIBLE);
                    tvPbNfcReading.setVisibility(View.VISIBLE);
                    scrollView.smoothScrollBy(0, 500);
                }
            });
            isoDep.connect();
            byte[] response;
            String result = "Content of ISO-DEP tag\n";

            try {
                bacKey = new BACKey(
                        passportNumber.getText().toString(),
                        birthDate.getText().toString(),
                        expirationDate.getText().toString()
                );
            } catch (IllegalArgumentException e) {
                Log.w(TAG, e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //UI related things, not important for NFC
                        nfcaContent.setText("*** ERROR *** wrong data in fields above");
                        scrollView.smoothScrollBy(0, 200);
                        pbNfcReading.setVisibility(View.GONE);
                        tvPbNfcReading.setVisibility(View.GONE);
                    }
                });
            }

            try {
                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                System.out.println("** cardService: " + cardService.toString());

                PassportService service = new PassportService(cardService, NORMAL_MAX_TRANCEIVE_LENGTH, DEFAULT_MAX_BLOCKSIZE, true, false);
                service.open();

                System.out.println("** passport service isOpen?: " + service.isOpen());

                boolean paceSucceeded = false;
                try {
                    //CardSecurityFile cardSecurityFile = new CardSecurityFile(service.getInputStream(PassportService.EF_CARD_SECURITY));
                    //System.out.println("cardSecurityFile: " + cardSecurityFile.toString() + " #: " + cardSecurityFile.getSecurityInfos().toArray().toString());
                    //Collection<SecurityInfo> securityInfoCollection = cardSecurityFile.getSecurityInfos();

                    CardAccessFile cardAccessFile = new CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS, DEFAULT_MAX_BLOCKSIZE));
                    Collection<SecurityInfo> securityInfoCollection = cardAccessFile.getSecurityInfos();

                    for (SecurityInfo securityInfo : securityInfoCollection) {
                        if (securityInfo instanceof PACEInfo) {
                            PACEInfo paceInfo = (PACEInfo) securityInfo;
                            service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), null);
                            paceSucceeded = true;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //UI related things, not important for NFC
                            nfcaContent.setText("*** ERROR *** wrong data in fields above");
                            scrollView.smoothScrollBy(0, 200);
                            pbNfcReading.setVisibility(View.GONE);
                            tvPbNfcReading.setVisibility(View.GONE);
                        }
                    });
                }

                service.sendSelectApplet(paceSucceeded);

                if (!paceSucceeded) {
                    try {
                        service.getInputStream(PassportService.EF_COM, DEFAULT_MAX_BLOCKSIZE).read();
                    } catch (Exception e) {
                        service.doBAC(bacKey);
                    }
                }

                // -- Personal Details -- //
                CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1, DEFAULT_MAX_BLOCKSIZE);
                DG1File dg1File = new DG1File(dg1In);

                MRZInfo mrzInfo = dg1File.getMRZInfo();
                personDetails.setName(mrzInfo.getSecondaryIdentifier().replace("<", " ").trim());
                personDetails.setSurname(mrzInfo.getPrimaryIdentifier().replace("<", " ").trim());
                personDetails.setPersonalNumber(mrzInfo.getPersonalNumber());
                personDetails.setGender(mrzInfo.getGender().toString());
                personDetails.setBirthDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfBirth()));
                personDetails.setExpiryDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfExpiry()));
                personDetails.setSerialNumber(mrzInfo.getDocumentNumber());
                personDetails.setNationality(mrzInfo.getNationality());
                personDetails.setIssuerAuthority(mrzInfo.getIssuingState());

                if ("I".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.ID_CARD;
                } else if ("P".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.PASSPORT;
                }

                // -- Face Image -- //
                CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2, DEFAULT_MAX_BLOCKSIZE);
                DG2File dg2File = new DG2File(dg2In);
                List<FaceInfo> faceInfos = dg2File.getFaceInfos();
                List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
                for (FaceInfo faceInfo : faceInfos) {
                    allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
                }
                if (!allFaceImageInfos.isEmpty()) {
                    FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();
                    Image image = ImageUtil.getImage(MainActivity.this, faceImageInfo);
                    personDetails.setFaceImage(image.getBitmapImage());
                    personDetails.setFaceImageBase64(image.getBase64Image());
                }

                // -- Fingerprint (if exist)-- //
                try {
                    CardFileInputStream dg3In = service.getInputStream(PassportService.EF_DG3, DEFAULT_MAX_BLOCKSIZE);
                    DG3File dg3File = new DG3File(dg3In);
                    List<FingerInfo> fingerInfos = dg3File.getFingerInfos();
                    List<FingerImageInfo> allFingerImageInfos = new ArrayList<>();
                    for (FingerInfo fingerInfo : fingerInfos) {
                        allFingerImageInfos.addAll(fingerInfo.getFingerImageInfos());
                    }
                    List<Bitmap> fingerprintsImage = new ArrayList<>();
                    if (!allFingerImageInfos.isEmpty()) {
                        for (FingerImageInfo fingerImageInfo : allFingerImageInfos) {
                            Image image = ImageUtil.getImage(MainActivity.this, fingerImageInfo);
                            fingerprintsImage.add(image.getBitmapImage());
                        }
                        personDetails.setFingerprints(fingerprintsImage);
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                    result += "DG3File not available (Fingerprint)" + "\n";
                }

                // -- Portrait Picture -- //
                try {
                    CardFileInputStream dg5In = service.getInputStream(PassportService.EF_DG5, DEFAULT_MAX_BLOCKSIZE);
                    DG5File dg5File = new DG5File(dg5In);

                    List<DisplayedImageInfo> displayedImageInfos = dg5File.getImages();
                    if (!displayedImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = displayedImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                    result += "DG5File not available (Portrait Picture)" + "\n";
                }

                // -- Signature (if exist) -- //
                try {
                    CardFileInputStream dg7In = service.getInputStream(PassportService.EF_DG7, DEFAULT_MAX_BLOCKSIZE);
                    DG7File dg7File = new DG7File(dg7In);

                    List<DisplayedImageInfo> signatureImageInfos = dg7File.getImages();
                    if (!signatureImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = signatureImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }

                } catch (Exception e) {
                    Log.w(TAG, e);
                    result += "DG7File not available (Signature)" + "\n";
                }

                // -- Additional Details (if exist) -- //
                try {
                    CardFileInputStream dg11In = service.getInputStream(PassportService.EF_DG11, DEFAULT_MAX_BLOCKSIZE);
                    DG11File dg11File = new DG11File(dg11In);

                    if (dg11File.getLength() > 0) {
                        additionalPersonDetails.setCustodyInformation(dg11File.getCustodyInformation());
                        additionalPersonDetails.setNameOfHolder(dg11File.getNameOfHolder());
                        additionalPersonDetails.setFullDateOfBirth(dg11File.getFullDateOfBirth());
                        additionalPersonDetails.setOtherNames(dg11File.getOtherNames());
                        additionalPersonDetails.setOtherValidTDNumbers(dg11File.getOtherValidTDNumbers());
                        additionalPersonDetails.setPermanentAddress(dg11File.getPermanentAddress());
                        additionalPersonDetails.setPersonalNumber(dg11File.getPersonalNumber());
                        additionalPersonDetails.setPersonalSummary(dg11File.getPersonalSummary());
                        additionalPersonDetails.setPlaceOfBirth(dg11File.getPlaceOfBirth());
                        additionalPersonDetails.setProfession(dg11File.getProfession());
                        additionalPersonDetails.setProofOfCitizenship(dg11File.getProofOfCitizenship());
                        additionalPersonDetails.setTag(dg11File.getTag());
                        additionalPersonDetails.setTagPresenceList(dg11File.getTagPresenceList());
                        additionalPersonDetails.setTelephone(dg11File.getTelephone());
                        additionalPersonDetails.setTitle(dg11File.getTitle());
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                    result += "DG11File not available (Additional Details)" + "\n";
                }

                // -- Additional Details (if exist) like issuing authority -- //
                String issuingAuthority = "";
                String dateOfIssue = "";
                String dateOfPersonalization = "";
                try {
                    CardFileInputStream dg12In = service.getInputStream(PassportService.EF_DG12, DEFAULT_MAX_BLOCKSIZE);
                    DG12File dg12File = new DG12File(dg12In);
                    issuingAuthority = dg12File.getIssuingAuthority();
                    dateOfIssue = dg12File.getDateOfIssue();
                    dateOfPersonalization = dg12File.getDateAndTimeOfPersonalization();
                } catch (Exception e) {
                    Log.w(TAG, e);
                    result += "DG12File not available (Additional Data)" + "\n";
                }

                // -- Document Public Key -- //
                try {
                    CardFileInputStream dg15In = service.getInputStream(PassportService.EF_DG15, DEFAULT_MAX_BLOCKSIZE);
                    DG15File dg15File = new DG15File(dg15In);
                    PublicKey publicKey = dg15File.getPublicKey();
                    eDocument.setDocPublicKey(publicKey);
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                eDocument.setDocType(docType);
                eDocument.setPersonDetails(personDetails);
                eDocument.setAdditionalPersonDetails(additionalPersonDetails);

            } catch (Exception e) {
                Log.d(TAG, String.valueOf(e));
                result += "DG15File not available (Document Public Key)" + "\n";
            }

            result += "NAME: " + eDocument.getPersonDetails().getName() + "\n";
            result += "SURNAME: " + eDocument.getPersonDetails().getSurname() + "\n";
            result += "PERSONAL NUMBER: " + eDocument.getPersonDetails().getPersonalNumber() + "\n";
            result += "GENDER: " + eDocument.getPersonDetails().getGender() + "\n";
            result += "BIRTH DATE: " + eDocument.getPersonDetails().getBirthDate() + "\n";
            result += "EXPIRY DATE: " + eDocument.getPersonDetails().getExpiryDate() + "\n";
            result += "SERIAL NUMBER: " + eDocument.getPersonDetails().getSerialNumber() + "\n";
            result += "NATIONALITY: " + eDocument.getPersonDetails().getNationality().replace("<", " ").trim() + "\n";
            result += "DOC TYPE: " + eDocument.getDocType().name() + "\n";
            result += "ISSUER AUTHORITY: " + eDocument.getPersonDetails().getIssuerAuthority().replace("<", " ").trim() + "\n";

            // here we are using the real image and not the scaled one
            Bitmap fullSizeImage = eDocument.getPersonDetails().getFaceImage();
            //result += "IMAGE FULL SIZE WIDTH: " + fullSizeImage.getWidth() + " HEIGHT: " + fullSizeImage.getHeight() + "\n";
            //Bitmap image = ImageUtil.scaleImage(eDocument.getPersonDetails().getFaceImage());

            String finalIdContentString = result;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText(finalIdContentString);
                    //ivPhoto.setImageBitmap(image);
                    ivPhoto.setImageBitmap(fullSizeImage);
                    pbNfcReading.setVisibility(View.GONE);
                    tvPbNfcReading.setVisibility(View.GONE);
                }
            });
            try {
                isoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            //Trying to catch any ioexception that may be thrown
            e.printStackTrace();
        } catch (Exception e) {
            //Trying to catch any exception that may be thrown
            e.printStackTrace();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}