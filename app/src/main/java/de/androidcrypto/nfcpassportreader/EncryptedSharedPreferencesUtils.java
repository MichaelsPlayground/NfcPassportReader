package de.androidcrypto.nfcpassportreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptedSharedPreferencesUtils {

    /**
     * This class bundles all methods to work with EncryptedSharedPreferences
     */

    private static Context context;
    private static String masterKeyAlias;
    private static SharedPreferences sharedPreferences;
    private static final String encryptedSharedPreferencesFilename = "esp.dat";
    private static final String encryptedSharedPreferencesDefaultValue = "no data stored";
    private static final boolean encryptedSharedPreferencesDefaultValueBoolean = false;
    private static final String PASSPORT_NUMBER = "passport_number";
    private static final String PASSPORT_BIRTH_DATE = "passport_birth_date";
    private static final String PASSPORT_EXPIRATION_DATE = "passport_expiration_date";

    public static boolean setupEncryptedSharedPreferences(Context myContext) {
        try {
            context = myContext;
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
            sharedPreferences = EncryptedSharedPreferences.create(
                    encryptedSharedPreferencesFilename,
                    masterKeyAlias,
                    myContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void savePassportData(String passportNumber, String passportBirthDate, String passportExpirationDate) {
        saveEncryptedSharedPreferences(PASSPORT_NUMBER, passportNumber);
        saveEncryptedSharedPreferences(PASSPORT_BIRTH_DATE, passportBirthDate);
        saveEncryptedSharedPreferences(PASSPORT_EXPIRATION_DATE, passportExpirationDate);
    }

    public static String loadPassportNumber() {
        return getEncryptedSharedPreferences(PASSPORT_NUMBER);
    }

    public static String loadPassportBirthDate() {
        return getEncryptedSharedPreferences(PASSPORT_BIRTH_DATE);
    }

    public static String loadPassportExpirationDate() {
        return getEncryptedSharedPreferences(PASSPORT_EXPIRATION_DATE);
    }

    /**
     * private methods follow
     */

    private static boolean checkEncryptedSharedPreferencesStored(String key) {
        String decryptedData = sharedPreferences
                .getString(key, encryptedSharedPreferencesDefaultValue);
        if (decryptedData.equals(encryptedSharedPreferencesDefaultValue)) {
            return false;
        } else {
            return true;
        }
    }

    private static String getEncryptedSharedPreferences(String key) {
        return sharedPreferences
                .getString(key, encryptedSharedPreferencesDefaultValue);
    }

    private static void saveEncryptedSharedPreferences(String key, String value) {
        sharedPreferences
                .edit()
                .putString(key, value)
                .apply();
    }

    private static boolean getEncryptedSharedPreferencesBoolean(String key) {
        return sharedPreferences
                .getBoolean(key, encryptedSharedPreferencesDefaultValueBoolean);
    }

    private static void saveEncryptedSharedPreferencesBoolean(String key, boolean value) {
        sharedPreferences
                .edit()
                .putBoolean(key, value)
                .apply();
    }
}