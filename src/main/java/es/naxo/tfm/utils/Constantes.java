package es.naxo.tfm.utils;

import java.util.Properties;

public class Constantes {

	// Obtengo la ruta del certificado, a partir de una variable de entorno del servidor JBoss, y en función del entorno Apple destino (desarrollo o producción)
	public static final Properties p = System.getProperties(); 
	public static final String rutaKeyStore = p.getProperty("jboss.home.dir") + "/standalone/ks/";
	
	public static final String certificateCAFile = rutaKeyStore + "rootCA_TFM_Ciberseguridad.pem";
	public static final String privateKeyCAFile = rutaKeyStore + "rootCA_TFM_Ciberseguridad.key"; 
	
	public static final String listaSerialNumberValidos = rutaKeyStore + "listaSerialNumberValidos";
	public static final String listaSerialNumberUsados = rutaKeyStore + "listaSerialNumberUsados";

	
	public static final String privateKeyCertificadoDevice = null; 
}
