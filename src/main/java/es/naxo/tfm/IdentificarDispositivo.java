package es.naxo.tfm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.naxo.tfm.utils.Cifrado;
import es.naxo.tfm.utils.Constantes;
import es.naxo.tfm.utils.Trazas;

public class IdentificarDispositivo {

	// Clave usada compartida para generar el certificado unico. 
	private static final String claveSecretoCompartido1 = "6849576002387456";
	private static final String claveSecretoCompartido2 = "9472652849608709";

	public static String obtenerSerialNumber (String identificadorUnico)  {
			
		// Desciframos el identificadorUnico
		byte[] descifrado = Base64.getDecoder().decode(identificadorUnico);
		
		//Lo desciframos desde AES, con la clave secreta también compartida. 
		String cadena = Cifrado.descifra(descifrado);
		
		// Eliminamos los secretos compartidos de inicio y final. Lo que nos queda al final es el Serial Number. 
		cadena = cadena.replaceAll(claveSecretoCompartido1, "");
		String serialNumber = cadena.replaceAll(claveSecretoCompartido2, "");
		
		return serialNumber;
	}
	
	/* Ejecuto la validación de un campo SerialNumber de Raspberry. 
	 *  - Tiene que existir. 
	 *  - Tener 16 caracteres exactos de tamaño, de tipo Hexadecimal.
	 *  - Tiene que estar en la lista de SerialNumber autorizadas
	 *  - No tiene que estar en la lista de SerialNumber ya utilizados anteriormente. 
	 *  - Devuelve el tipo de error al devolver al cliente, o 0 si es OK.
	 *      -1 si es error de validacion del SerialNumber
	 *      -2 si es error de que no está en la lista de SerialNumbers validos
	 *      -3 si es error de que ya fue utilizado anteriormente.  
	 */
	public static int validarSerialNumber (String serialNumber)    {
		
		if (serialNumber == null || serialNumber.equals(""))   {
			Trazas.getLogger().warn("Error en la validación del SerialNumber. Está vacio: " + serialNumber);
			return -1;
		}

	    if (serialNumber.length() != 16)   {
	    	Trazas.getLogger().warn("Error en la longitud (" + serialNumber.length() + ") del SerialNumber: " + serialNumber);
			return -1;
		}
	    
		Pattern patron = Pattern.compile("^[A-Fa-f\\d]{16}$");
	    Matcher busqueda = patron.matcher(serialNumber);
		
	    if (busqueda.matches() == false)   {
	    	Trazas.getLogger().warn("Error en la validación de la expresion regular del SerialNumber: " + serialNumber);
			return -1;
		}
	    
	    // Cargo la lista de serialNumbers validos. 
	    HashMap <String, String> valoresSerialNumberValidos = cargarLista(Constantes.listaSerialNumberValidos);
	    
	    // Valido si el SerialNumber está en la lista de valores correctos. 
	    if (valoresSerialNumberValidos.containsKey(serialNumber) == false)    {
	    	Trazas.getLogger().warn("Error, el SerialNumber no está en la lista de valores correctos: " + serialNumber);
	    	return -2;
		}
	
	    // Cargo la lista de serialNumbers ya usados anteriormente. 
	    HashMap <String, String> valoresSerialNumberUsados = cargarLista(Constantes.listaSerialNumberUsados);

	    // Valido si el SerialNumber ya se ha usado previamente algún dispositivo.
	    if (valoresSerialNumberUsados.containsKey(serialNumber) == true)    {
	    	Trazas.getLogger().warn("Error, el SerialNumber ya ha sido utilizado anteriormente: " + serialNumber);
	    	return -3;
		}
	    
	    // Si todo fue bien, devuelvo null; 
	    return 0;
	}

	private static HashMap<String, String> cargarLista (String file)    {
		
		HashMap<String, String> lista = new HashMap<String, String>();
		FileReader fr = null;
		BufferedReader br = null;
		
		try   {
			fr = new FileReader(file);
			br = new BufferedReader(fr);
	
	        String line = br.readLine();
	        while (line != null)    {
	        	lista.put(line, null);
	        	line = br.readLine();
	        }
		}
		catch (Exception ex)    {
			Trazas.getLogger().error("Error al cargar el fichero: " + file, ex);
		}
		finally    {
			try   {
				if (br != null)   {
					br.close();
				}
			}
			catch (Exception ex2)   {
				Trazas.getLogger().error("Error al cerrar el fichero: " + file, ex2);
			}
		}
	
		return lista;
	}
	
	public static void grabarLista (String serialNumber, String file)    {
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		
		try   {
			
			// Valido si el fichero existe para ver si lo abro con true o no. 
			File fichero = new File (file); 
			if (fichero.exists())   {
				fw = new FileWriter(file, true);
			}
			else   {
				fw = new FileWriter(file);
			}
			
			bw = new BufferedWriter(fw);
	
			if (serialNumber != null)    {
				bw.write(serialNumber);
				bw.newLine();
			}
		}
		catch (Exception ex)    {
			Trazas.getLogger().error("Error al grabar el fichero: " + file, ex);
		}
		finally    {
			try   {
				if (bw != null)   {
					bw.close();
				}
			}
			catch (Exception ex2)   {
				Trazas.getLogger().error("Error al cerrar el fichero: " + file, ex2);
			}
		}
	}
}
