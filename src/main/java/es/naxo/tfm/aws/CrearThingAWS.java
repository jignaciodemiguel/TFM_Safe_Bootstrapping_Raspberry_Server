package es.naxo.tfm.aws;


import java.io.FileReader;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.CreateThingRequest;
import com.amazonaws.services.iot.model.CreateThingResult;

import es.naxo.tfm.utils.Constantes;
import es.naxo.tfm.utils.Trazas;

public class CrearThingAWS {

	public static String accessKeyId = null;
	public static String secretAccessKey = null;
	
	public static boolean crearDevice (String idDevice)    {
		
		try    {

			// Cargamos las variables de identificacion. 
			Properties p = new Properties();
			p.load(new FileReader(Constantes.rutaKeyStore + "properties.conf"));

			accessKeyId = p.getProperty("accessKeyId");
			secretAccessKey = p.getProperty("secretAccessKey");
			
			if (accessKeyId == null || "".equals(accessKeyId) || secretAccessKey == null || "".equals(secretAccessKey))   {
				Trazas.getLogger().error ("Error al cargar las credenciales: " + accessKeyId + " " + secretAccessKey);
			}
			
			BasicAWSCredentials credential = new BasicAWSCredentials(accessKeyId, secretAccessKey);
			AWSIotClient client = new AWSIotClient(credential);
			client.setRegion (Region.getRegion (Regions.EU_CENTRAL_1));
			CreateThingRequest ct = new CreateThingRequest().withThingName(idDevice);
			CreateThingResult cresult = client.createThing(ct);
			
			String id = cresult.getThingId();
			
			if (id == null || "".equals(id))    {
				Trazas.getLogger().error ("Error al crear el Thing en AWS: " + idDevice + ". El Id llega vacio");
				return false; 
			}
		}
		
		catch (Exception ex)    {
			Trazas.getLogger().error ("Excepcion al crear el Thing en AWS: " + idDevice, ex);
			return false;
		}
		
		return true; 
	}
}
