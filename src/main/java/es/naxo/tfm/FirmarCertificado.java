package es.naxo.tfm;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

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

import es.naxo.tfm.utils.Constantes;
import es.naxo.tfm.utils.CryptoUtils;
import es.naxo.tfm.utils.Trazas;

public class FirmarCertificado {
	

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
            
            X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(cacert, serial, issuedDate, 
            		expiryDate, jcaRequest.getSubject(), jcaRequest.getPublicKey());
            
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
			System.err.println("Excepcion al generar el base64 del certificado firmado");
		    e.printStackTrace();
		    return null; 
		}
	    
	    return certificadoFirmadoString;
	}
	
	/*
	 * Una vez que está el certificado firmado, le concatena después la clave publica de nuestra CA, para que después funcione 
	 * el autoregistro y activación de certificados en AWS. 
	 * Además prepara ya la cadena completa en Base64 para devolverlo como respuesta HTTPS.
	 * Recibe el certificado ya firmado. 
	 * Devuelve el certificado en Base64, y con la clave publica de la CA en base64 también concatenada. 
	 */
	public static String agregarCAYPrepararBase64 (String certificadoFirmado)    {
		
	    // Cargo el contenido de la clave publica de la CA, que lo concatenaré junto con el certificado firmado.
		String clavePublicaCAEnTexto = null;
		FileInputStream fileInput = null;
		ByteArrayOutputStream baos = null;
	    
	    try		{
			
			fileInput = new FileInputStream(Constantes.certificateCAFile);
			
			byte [] array = new byte[10000];
			int leidos = fileInput.read(array);

			if (leidos >=10000 || leidos <= 0)    {
				Trazas.getLogger().error("Error al leer el fichero de la CA. Leidos: " + leidos);
		    	fileInput.close();	
				return null; 
			}

			clavePublicaCAEnTexto = new String (array, 0, leidos, StandardCharsets.UTF_8);
	    	fileInput.close();	
		}

	    catch (Exception ex)		{
	    	Trazas.getLogger().error("Excepcion al cargar la clave publica de la CA de su fichero", ex);
			return null; 
		}
		
	    // Ahora grabamos en el fichero tanto el certificado firmado que nos han devuelto, como la clave publica de la CA, para que luego sea compatible con 
	    // el autoregistro de certificados en AWS. 
	    try    {

	    	byte[] cert = certificadoFirmado.getBytes();
	    	baos = new ByteArrayOutputStream();

	    	baos.write("-----BEGIN CERTIFICATE-----\n".getBytes("UTF-8"));

	    	int lineas = cert.length / 64;
		    for (int i = 0; i < lineas; i++)    {
			    baos.write(cert, i*64, 64);
	    		baos.write ("\n".getBytes());
		    }

	    	if (cert.length % 64 > 0)    {
	    		baos.write(cert, lineas*64, cert.length % 64);
	    	}
		    
		    baos.write("\n-----END CERTIFICATE-----\n".getBytes("UTF-8"));
		    
		    // Después le concateno la clave publica de la CA, ya que será necesaria para que en AWS podamos autoregistrar el certificado. 
		    baos.write(clavePublicaCAEnTexto.getBytes());
		    
		    baos.close();
	    }
		catch (Exception e)		{
			System.err.println("Excepcion al grabar el certificado firmado en el fichero de salida");
		    e.printStackTrace();
		    return null;
		}
	    
	    return baos.toString(); 
	}
	
}
