package de.androidcrypto.nfcpassportreader;

import android.graphics.Bitmap;

public class Image {
    // https://github.com/alimertozdemir/EPassportNFCReader/blob/master/app/src/main/java/com/alimert/passportreader/util/Image.java
    private Bitmap bitmapImage;
    private String base64Image;

    public Bitmap getBitmapImage() {
        return bitmapImage;
    }

    public void setBitmapImage(Bitmap bitmapImage) {
        this.bitmapImage = bitmapImage;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }
}
