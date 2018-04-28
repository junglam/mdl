package eu.imua.mdl.demo.xmldsig;

import javax.xml.crypto.KeySelectorResult;
import java.security.Key;
import java.security.PublicKey;

/**
 * @author mdl@imua.eu
 */
public class SimpleKeySelectorResult implements KeySelectorResult {

    private PublicKey publicKey;

    @Override
    public Key getKey() {
        return publicKey;
    }


    public SimpleKeySelectorResult(PublicKey pk) {
        publicKey = pk;
    }
}
