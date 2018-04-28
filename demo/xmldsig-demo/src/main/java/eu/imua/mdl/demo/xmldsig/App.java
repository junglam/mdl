package eu.imua.mdl.demo.xmldsig;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Paths;
import java.security.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * xmldsig-demo
 * @author mdl@imua.eu
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        Security.addProvider(new BouncyCastleProvider());
        final KeyPair keyPair = generateKeyPair();

        String cwd = Paths.get("").toAbsolutePath().toString();
        FileInputStream fileInputStream = new FileInputStream(new File(cwd + File.separator + "demo/pom.xml"));

        Document document = initializeSigningDocument(fileInputStream);
        System.out.println(xml2String(document));
        xmldsigCreate(document, keyPair);
        System.out.println(xml2String(document));

        // validating
        boolean result = xmldsigValidate(document, new SimpleKeySelectorResult(keyPair.getPublic()));
        System.out.println("XMLDSig validated " + result);
    }

    static String xml2String(Document document) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    static boolean xmldsigValidate(Document document, KeySelectorResult keySelectorResult) throws MarshalException, XMLSignatureException {
        NodeList nl = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0)
            throw new RuntimeException("Cannot find Signature element");

        DOMValidateContext valContext = new DOMValidateContext
                (new KeySelector() {
                    @Override
                    public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {
                        if (keyInfo == null) {
                            throw new KeySelectorException("Null KeyInfo object!");
                        }
                        SignatureMethod sm = (SignatureMethod) method;
                        List list = keyInfo.getContent();

                        for (int i = 0; i < list.size(); i++) {
                            XMLStructure xmlStructure = (XMLStructure) list.get(i);
                            if (xmlStructure instanceof KeyValue) {
                                PublicKey pk = null;
                                try {
                                    pk = ((KeyValue)xmlStructure).getPublicKey();
                                } catch (KeyException ke) {
                                    throw new KeySelectorException(ke);
                                }
                                // make sure algorithm is compatible with method
                                if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                                    return keySelectorResult;
                                }
                            }
                        }
                        throw new KeySelectorException("No KeyValue element found!");
                    }
                }, nl.item(0));

        XMLSignature xmlSignature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(valContext);
        // The validate method returns "true" if the signature validates successfully according to
        // the core validation rules in the W3C XML Signature Recommendation, and false otherwise.
        boolean coreValidity = xmlSignature.validate(valContext);
        System.out.println("Core validation: " + coreValidity);

        // signature validation
        boolean sv = xmlSignature.getSignatureValue().validate(valContext);
        System.out.println("signature validation status: " + sv);

        // iterate over the references and check the validation status of each one
        Iterator i = xmlSignature.getSignedInfo().getReferences().iterator();
        boolean refValid = false;
        for (int j = 0; i.hasNext(); j++) {
            refValid = ((Reference) i.next()).validate(valContext);
            System.out.println("ref["+j+"] validity status: " + refValid);
            if (refValid) break;
        }

        if (coreValidity || sv || refValid)
            return true;
        return false;
    }

    static boolean algEquals(String algURI, String algName) {
        if (algName.equalsIgnoreCase("DSA") &&
                algURI.equalsIgnoreCase("http://www.w3.org/2009/xmldsig11#dsa-sha256")) {
            return true;
        } else if (algName.equalsIgnoreCase("RSA") &&
                algURI.equalsIgnoreCase("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")) {
            return true;
        } else {
            return false;
        }
    }

    static void xmldsigCreate(Document document, KeyPair keyPair) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, KeyException, MarshalException, XMLSignatureException {
        XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");

        // creating reference
        DigestMethod digestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA256, null);
        Transform transform = xmlSignatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        List transforms = Collections.singletonList(transform);
        Reference reference = xmlSignatureFactory.newReference("", digestMethod, transforms, null, null);

        // creating signInfo
        CanonicalizationMethod canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null);
        SignatureMethod signatureMethod = xmlSignatureFactory.newSignatureMethod("http://www.w3.org/2009/xmldsig11#dsa-sha256", null);
        List references = Collections.singletonList(reference);
        SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod, references);

        // creating keyInfo
        KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
        KeyValue keyValue = keyInfoFactory.newKeyValue(keyPair.getPublic());
        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));

        // creating signature
        DOMSignContext signContext = new DOMSignContext(keyPair.getPrivate(), document.getDocumentElement());
        XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
        xmlSignature.sign(signContext);
    }

    static Document initializeSigningDocument(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); // must make namespace-aware

        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document document = builder.parse(inputStream);
        return document;
    }

    static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        // create keypair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }

}
