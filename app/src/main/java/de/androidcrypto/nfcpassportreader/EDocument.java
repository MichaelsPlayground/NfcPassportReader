package de.androidcrypto.nfcpassportreader;

import java.security.PublicKey;

public class EDocument {
    // https://github.com/alimertozdemir/EPassportNFCReader/blob/master/app/src/main/java/com/alimert/passportreader/model/EDocument.java
    private DocType docType;
    private PersonDetails personDetails;
    private AdditionalPersonDetails additionalPersonDetails;
    private PublicKey docPublicKey;

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public PersonDetails getPersonDetails() {
        return personDetails;
    }

    public void setPersonDetails(PersonDetails personDetails) {
        this.personDetails = personDetails;
    }

    public AdditionalPersonDetails getAdditionalPersonDetails() {
        return additionalPersonDetails;
    }

    public void setAdditionalPersonDetails(AdditionalPersonDetails additionalPersonDetails) {
        this.additionalPersonDetails = additionalPersonDetails;
    }

    public PublicKey getDocPublicKey() {
        return docPublicKey;
    }

    public void setDocPublicKey(PublicKey docPublicKey) {
        this.docPublicKey = docPublicKey;
    }
}
