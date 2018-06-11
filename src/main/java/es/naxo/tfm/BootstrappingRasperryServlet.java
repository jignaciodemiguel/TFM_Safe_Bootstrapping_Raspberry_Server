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
import es.naxo.tfm.utils.Trazas;

public class BootstrappingRasperryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
       
    /**
     * @see MiDispatcherServlet#PadreServlet()
     */
    public BootstrappingRasperryServlet() {
        super();
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Trazas.inicializar();
		ServletOutputStream sout = response.getOutputStream();
        response.setContentType("text/plain;charset=UTF-8");

		// Obtengo los parametros que me llegan. 
		String csr = (String) request.getParameter("csr");
		String identificadorUnico = (String) request.getParameter("identificador");

		Trazas.getLogger().info ("Recibida petición Bootstrapping. Identificador: " + identificadorUnico + " CSR: " + csr);
		
		// Validamos que ambos están en Base64
		Pattern patron = Pattern.compile("[a-zA-Z0-9/\\-\\+\\=\\s\\t]{1,10000}");
	    
		// Validamos el CSR. 
		Matcher busqueda = patron.matcher(csr);
		if (csr == null || csr.equals("") || busqueda.matches() == false)   {
			Trazas.getLogger().warn("Error en la validación de usuario. Está vacio o no coincide la expresion regular: " + csr);
	        sout.print("KO_Error_CSR");
	        return; 
		}

		// Validamos el identificadorUnico. 
	    busqueda = patron.matcher(identificadorUnico);

	    if (identificadorUnico == null || identificadorUnico.equals("") || busqueda.matches() == false)   {
			Trazas.getLogger().warn("Error en la validación de identificadorUnico. Está vacio o no coincide la expresion regular: " + identificadorUnico);
	        sout.print("KO_Error_IdentificadorUnico");
	        return; 
		}

	    String resultado = null; 
	    
	    try    {
	    	resultado = FirmarCertificado.firmarCertificadoConCA(csr, identificadorUnico);
	    	
	    	if (resultado == null || "".equals(resultado))    {
		    	Trazas.getLogger().error ("Error, el certificado firmado viene vacio: " + resultado);
		    	sout.print("KO_Error_Firma");
		        return;
	    	}

			Trazas.getLogger().info ("Firma de certificado realizada con éxito. Identificador: " + identificadorUnico);

	    	// Todo ha ido bien, procedemos a crear el Thing en AWS. 
			boolean resultado2 = CrearThingAWS.crearDevice(FirmarCertificado.obtenerIdDevice(identificadorUnico));
			
			if (resultado2 == false)    {
		    	sout.print("KO_Error_Crear_Device");
				return; 
			}

			Trazas.getLogger().info ("Thing creado en AWS con éxito. Identificador: " + identificadorUnico);

	    	// Todo ha ido bien, devolvemos el certificado firmado;
	    	sout.print(resultado);
	    	return;
	    }
	    catch (Exception ex)    {
	    	Trazas.getLogger().error ("Excepcion en la firma del CSR", ex);
	    	sout.print("KO_Error_Firma");
	        return;
	    }
	}
}
