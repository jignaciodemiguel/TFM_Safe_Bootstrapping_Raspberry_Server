package es.naxo.tfm;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;

import es.naxo.tfm.utils.Cifrado;
import es.naxo.tfm.utils.Constantes;
import es.naxo.tfm.utils.CryptoUtils;

public class FirmarCertificado {
	
	// Clave usada compartida para generar el certificado unico. 
	private static final String claveSecretoCompartido1 = "6849576002387456";
	private static final String claveSecretoCompartido2 = "9472652849608709";

	public static String obtenerIdDevice (String identificadorUnico) 
			throws Exception {
			
			// Desciframos el identificadorUnico
			byte[] descifrado = Base64.getDecoder().decode(identificadorUnico);
			
			//Lo desciframos desde AES, con la clave secreta también compartida. 
			String cadena = Cifrado.descifra(descifrado);
			
			// Eliminamos los secretos compartidos de inicio y final. Lo que nos queda al final es el Serial Number. 
			cadena = cadena.replaceAll(claveSecretoCompartido1, "");
			String serialNumber = cadena.replaceAll(claveSecretoCompartido2, "");
			
			if (validarSerialNumber(serialNumber) == false)    {
				return null; 
			}

			return "Rasperry_" + serialNumber;
		}

	public static String firmarCertificadoConCA (String csr, String identificadorUnico)   {
		
		// Desciframos el identificadorUnico
		byte[] descifrado = Base64.getDecoder().decode(identificadorUnico);
		
		//Lo desciframos desde AES, con la clave secreta también compartida. 
		String cadena = Cifrado.descifra(descifrado);
		
		// Eliminamos los secretos compartidos de inicio y final. Lo que nos queda al final es el Serial Number. 
		cadena = cadena.replaceAll(claveSecretoCompartido1, "");
		String serialNumber = cadena.replaceAll(claveSecretoCompartido2, "");
		
		if (validarSerialNumber(serialNumber) == false)    {
			return null; 
		}

		String certificado = firmarCSR(csr);
		

		return certificado;
	}
	
	/**
	 * A partir de un CSR generado, y de la clave privada de nuestra CA, realiza la firma del certificado. 
	 * @param csrString El CSR, codificado en base64, para poder transmitirlo por HTTP. 
	 * Ayuda para generar el codigo de la firma: http://blog.pulasthi.org/2014/08/signing-certificate-signing-request-csr.html
	 *
	 * @return Devuelve a su vez un String en base64, para poder devolverlo también por HTTP. 
	 */
	public static String firmarCSR (String csrString)   {
	    
		// Cargo clave publica y privada de la CA. Y la variable que almacenará finalmente el certificado firmado.  
    	PrivateKey cakey = null;
    	X509Certificate cacert = null;
    	X509Certificate certificadoFirmado = null;
    	String certificadoFirmadoString = null;

		try		{
	    	cakey = CryptoUtils.loadPrivateKeyFromFile (Constantes.privateKeyCAFile, "RSA");
	    	cacert = (X509Certificate)CryptoUtils.loadCertificateFromFile(Constantes.certificateCAFile);
		}
		catch (Exception e)		{
			System.err.println("Excepcion al cargar la clave privada de la CA o el CSR");
			e.printStackTrace();
			return null; 
		}

	    // Ahora realizamos en si mismo la firma de certificado. 
	    try    {

		    // Cargo el CSR y lo decodifico, que viene en base64 para poder transportarlo. 
		    byte[] decodeado = Base64.getDecoder().decode(csrString); 
		    PKCS10CertificationRequest request = new PKCS10CertificationRequest(decodeado); 

		    // Fecha de inicio y fin de caducidad del certificado. 
		    Date issuedDate = new Date();
		    Date expiryDate = new Date(System.currentTimeMillis() + (730L * 86400000L));   // 2 años desde hoy. 
	
		    // Serial Number aleatorio. 
		    BigInteger serial = new BigInteger(32, new SecureRandom());
	    	
            JcaPKCS10CertificationRequest jcaRequest = new JcaPKCS10CertificationRequest(request);
            
            X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(cacert, serial, issuedDate, expiryDate, jcaRequest.getSubject(), jcaRequest.getPublicKey());
            
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                    extUtils.createAuthorityKeyIdentifier(cacert))
                    .addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(jcaRequest
                            .getPublicKey()))
                    .addExtension(Extension.basicConstraints, true, new BasicConstraints(0))
                    .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))
                    .addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
            
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(cakey);
            certificadoFirmado = new JcaX509CertificateConverter().getCertificate (certificateBuilder.build(signer));
	    }
	    
	    catch (Exception e)		{
	    	System.err.println("Excepcion al firmar el CSR");
	    	e.printStackTrace();
	    	return null; 
	    }
	    
	    // Devolvemos el certificado en base64, para que se pueda transmitir. 
	    try    {
		    byte[] certBase64 = certificadoFirmado.getEncoded();
		    certificadoFirmadoString = Base64.getEncoder().encodeToString(certBase64);
	    }

	    catch (Exception e)		{
			System.err.println("Excepcion al grabar el certificado firmado en el fichero de salida");
		    e.printStackTrace();
		    return null; 
		}
	    
	    return certificadoFirmadoString;
	}
	
	/* Ejecuto la validación sintactica de un campo SerialNumber de Rasperry. 
	 *  - Tiene que existir. 
	 *  - Tener 16 caracteres exactos de tamaño.
	 *  - Cumplir la expresion regular (numeros)
	 */
	public static boolean validarSerialNumber (String serialNumber)    {
		
		if (serialNumber == null || serialNumber.equals(""))   {
			System.err.println ("Error en la validación del SerialNumber. Está vacio: " + serialNumber);
			return false;
		}

	    if (serialNumber.length() != 16)   {
			System.err.println ("Error en la longitud (" + serialNumber.length() + ") del SerialNumber: " + serialNumber);
			return false;	
		}
	    
		Pattern patron = Pattern.compile("^[A-Za-z\\d]{16}$");
	    Matcher busqueda = patron.matcher(serialNumber);
		
	    if (busqueda.matches() == false)   {
			System.err.println("Error en la validación de la expresion regular del SerialNumber: " + serialNumber);
			return false;	
		}
	    
	    // Si he llegado hasta aquí, es que todo fue bien.
	    return true;
	}
}
