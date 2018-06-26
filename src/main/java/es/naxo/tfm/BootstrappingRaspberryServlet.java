package es.naxo.tfm;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import es.naxo.tfm.aws.CrearThingAWS;
import es.naxo.tfm.utils.Constantes;
import es.naxo.tfm.utils.Trazas;

public class BootstrappingRaspberryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
       
    /**
     * @see MiDispatcherServlet#PadreServlet()
     */
    public BootstrappingRaspberryServlet() {
        super();
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {

		Trazas.inicializar();
		ServletOutputStream sout = response.getOutputStream();
        response.setContentType("text/plain;charset=UTF-8");
        String certificado = null;

		// Obtengo los parametros que me llegan. 
		String csr = (String) request.getParameter("csr");
		String identificadorUnico = (String) request.getParameter("identificador");

		Trazas.getLogger().info ("Recibida petición Bootstrapping. Identificador: " + identificadorUnico + " CSR: " + csr);
		
		// Validamos que ambos están en Base64
		Pattern patron = Pattern.compile("[a-zA-Z0-9/\\-\\+\\=\\s\\t]{1,10000}");
	    
		// Validamos el CSR. 
		if (csr == null || csr.equals(""))   {
			Trazas.getLogger().warn("Error en la validación del CSR. Está vacio: " + csr);
	        response.sendError(400, "Error en la validación de parametros");
	        return; 
		}

		Matcher busqueda = patron.matcher(csr);
		if (busqueda.matches() == false)   {
			Trazas.getLogger().warn("Error en la validación del CSR. No coincide la expresion regular: " + csr);
	        response.sendError(400, "Error en la validación de parametros");
	        return; 
		}

		// Validamos el identificadorUnico. 
	    if (identificadorUnico == null || identificadorUnico.equals(""))   {
			Trazas.getLogger().warn("Error en la validación de identificadorUnico. Está vacio: " + identificadorUnico);
	        response.sendError(400, "Error en la validación de parametros");
	        return; 
		}

	    busqueda = patron.matcher(identificadorUnico);
	    if (busqueda.matches() == false)   {
			Trazas.getLogger().warn("Error en la validación de identificadorUnico. No coincide la expresion regular: " + identificadorUnico);
	        response.sendError(400, "Error en la validación de parametros");
	        return; 
		}

	    // Obtengo el SerialNumber a partir del identificadorUnico. 
	    String serialNumber = IdentificarDispositivo.obtenerSerialNumber(identificadorUnico); 
	    
	    // Valido si el Serial Number es correcto a nivel de formato, y también si está en la lista de Serial Numbers
	    // válidos y además que no se haya utilizado anteriormente por ningún dispositivo. 
	    int validacion = IdentificarDispositivo.validarSerialNumber(serialNumber);

	    // Si validación <> 0, es que falló la validación y devolvemos el error al cliente. 
	    if (validacion == -1)   {
	        response.sendError(400, "Error en la validación de parametros");
	    	return; 
	    }
	    else if (validacion == -2)    {
	    	response.sendError(403, "Serial Number no permitido");
	    	return; 
	    }
	    else if (validacion == -3)    {
	    	response.sendError(403, "Serial Number ya registrado anteriormente");
	    	return; 
	    }
	    
	    try    {
	    	
	    	// Firmamos el CSR.
	    	certificado = FirmarCertificado.firmarCSR(csr);
	    	
	    	if (certificado == null || "".equals(certificado))    {
		    	Trazas.getLogger().error ("Error, el certificado firmado viene vacio: " + certificado);
		    	response.sendError(500, "Error interno al firmar el certificado");
		        return;
	    	}

			Trazas.getLogger().info ("Firma de certificado realizada con éxito. Identificador: " + identificadorUnico);

			// Lo convertimos a Base64 (formato para grabar en fichero) y le agregamos la clave publica de la CA. 
			String certificadoMasCA = FirmarCertificado.agregarCAYPrepararBase64 (certificado);

			if (certificadoMasCA == null)    {
		    	response.sendError(500, "Error interno al firmar el certificado");
				return; 
			}

	    	// Todo ha ido bien, procedemos a crear el Thing en AWS. 
			boolean resultado2 = CrearThingAWS.crearDevice("Rasperry_" + serialNumber);
			
			if (resultado2 == false)    {
		    	response.sendError(500, "Error interno al crear el Thing en AWS");
				return; 
			}

			Trazas.getLogger().info ("Thing creado en AWS con éxito. Identificador: " + identificadorUnico);

	    	// Todo ha ido bien, marcamos el Serial Number como ya utilizado, para que no se permita usarlo más veces. 
			IdentificarDispositivo.grabarLista(serialNumber, Constantes.listaSerialNumberUsados);
			
			// Devolvemos el certificado firmado;
	    	sout.print(certificadoMasCA);
	    	return;
	    }
	    catch (Exception ex)    {
	    	Trazas.getLogger().error ("Excepcion en la firma del CSR", ex);
	    	response.sendError(500, "Error interno al firmar el certificado");
	        return;
	    }
	}
}
