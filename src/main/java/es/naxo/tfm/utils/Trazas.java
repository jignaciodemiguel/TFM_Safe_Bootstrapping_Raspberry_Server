package es.naxo.tfm.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Clase que implementa el control de las trazas. Internamente utiliza el log4j, aunque puede utilizar otro compatible.
 * 
 *  El uso general de esta clase sera: 
 *  
 *  	Trazas.getLogger().metodo(String texto) -> Si queremos usar la configuracion por defecto (caso normal)
 *    	Trazas.getLogger(String categoria)).metodo(String texto) -> Si queremos usar la configuracion asignada a una categoria especifica, 
 *    																por ejemplo para una clase, o grupo de clases.
 *    
 *  En cada aplicacion se debera inicializar antes de cualquier uso a partir del fichero correspondiente o una Properties, de la forma:
 *     	
 *      Trazas.inicializar ();
 *  	Trazas.inicializar (String fichero);
 *  	Trazas.inicializar (Properties propiedades);
 *
 * @author Naxete
 * 
 * Normalmente lo tendremos en modo INFO solo, actualizando directamente la constante 'nivelLogActual = "INFO";'
 * Si queremos habilitar DEBUG lo cambiamos a nivelLogActual = "DEBUG";
 * Si además queremos que salgan las queries de Hibernate, habilitamos a true en el fichero hibernate.cfg.xml la propiedad <property name="hibernate.show_sql">false</property>
 * 
 */

public class Trazas   {

	private final static Logger rootLogger = Logger.getRootLogger();
	public final static boolean isDebugEnabled = rootLogger.isDebugEnabled();
	private static String nivelLogActual = "INFO";
	
	
	public static String getNivelLogActual() {
		if (nivelLogActual == null)   {
			cargarVariables();
		}
		return nivelLogActual;
	}

	public static void setNivelLogActual(String nivelLogActual) {
		Trazas.nivelLogActual = nivelLogActual;
	}

	private static Properties cargarVariables()    {
		
		Properties props = new Properties();
		
		// Log global
		//props.setProperty("log4j.rootLogger", nivelLogActual + ", consola, fichero");
		props.setProperty("log4j.rootLogger", "INFO, consola, fichero");
		props.setProperty("log4j.logger.java", "INFO, consola, fichero");
		props.setProperty("log4j.logger.org", "INFO, consola, fichero");
		props.setProperty("log4j.logger.es.naxo.look4family", nivelLogActual + ", consola, fichero");
		
		// Configuración del log CONSOLE
		props.setProperty("log4j.appender.consola", "org.apache.log4j.ConsoleAppender");
		props.setProperty("log4j.appender.consola.layout", "org.apache.log4j.PatternLayout");
		props.setProperty("log4j.appender.consola.layout.ConversionPattern", "%d [%-5p] (%F:%L).%M - %m%n");

		// Configuracion del log de DEBUG a un fichero especifico.
		props.setProperty("log4j.appender.fichero", "org.apache.log4j.FileAppender");
		props.setProperty("log4j.appender.fichero.File", "${jboss.server.log.dir}/errores.log");
		//props.setProperty("log4j.appender.fichero.Threshold", "WARN");
		props.setProperty("log4j.appender.fichero.layout", "org.apache.log4j.PatternLayout");
		props.setProperty("log4j.appender.fichero.layout.ConversionPattern", "%d [%-5p] (%F:%L).%M - %m%n");

		return props;
	}

	public static void inicializar() {

		try {

			// Configuro el logger a partir del lisatdo de variables por defecto. Es el equivalente al log4j.properties pero a piñon. 
			Properties lpPropiedadesLog = cargarVariables();
			PropertyConfigurator.configure(lpPropiedadesLog);

			rootLogger.warn("Componente de trazas inicializado correctamente con el valor + " + Trazas.getLogger().getEffectiveLevel());
		}

		catch (Exception ex) {
			System.err.println("Excepcion al inicializar el componente de trazas a partir de una properties");
			ex.printStackTrace();
		}
	}

	public static void habilitarDebug() {

		try {

			// Configuro el logger a partir del lisatdo de variables por defecto. Es el equivalente al log4j.properties pero a piñon. 
			Properties lpPropiedadesLog = cargarVariables();
			
			// Habilito el DEBUG por si no estaba puesto. 
			// Log global
			lpPropiedadesLog.setProperty("log4j.rootLogger", "DEBUG, consola, fichero");
			nivelLogActual = "DEBUG";
			
			PropertyConfigurator.configure(lpPropiedadesLog);

			rootLogger.warn("Componente de trazas inicializado correctamente con el Debug habilitado...");
		}

		catch (Exception ex) {
			System.err.println("Excepcion al inicializar el componente de trazas a partir de una properties");
			ex.printStackTrace();
		}
	}

	public static void deshabilitarDebug() {

		try {

			// Configuro el logger a partir del lisatdo de variables por defecto. Es el equivalente al log4j.properties pero a piñon. 
			Properties lpPropiedadesLog = cargarVariables();
			
			// Habilito el DEBUG por si no estaba puesto. 
			// Log global
			lpPropiedadesLog.setProperty("log4j.rootLogger", "INFO, consola, fichero");
			nivelLogActual = "INFO";
			
			PropertyConfigurator.configure(lpPropiedadesLog);

			rootLogger.warn("Componente de trazas inicializado correctamente con el Debug deshabilitado...");
		}

		catch (Exception ex) {
			System.err.println("Excepcion al inicializar el componente de trazas a partir de una properties");
			ex.printStackTrace();
		}
	}
	
	public static void configurarNivelLog (String nivel)  {

		if (!nivel.equals("DEBUG") && !nivel.equals("INFO") && !nivel.equals("WARN") && !nivel.equals("ERROR") && !nivel.equals("FATAL"))   {
			System.err.println("Error en el tipo de nivel de log a configurar: " + nivel);
			rootLogger.error("Error en el tipo de nivel de log a configurar: " + nivel);
			return;
		}
		
		try {

			// Configuro el logger a partir del lisatdo de variables por defecto. Es el equivalente al log4j.properties pero a piñon. 
			Properties lpPropiedadesLog = cargarVariables();
			
			// Habilito el DEBUG por si no estaba puesto. 
			// Log global
			lpPropiedadesLog.setProperty("log4j.rootLogger", nivel + ", consola, fichero");
			nivelLogActual = nivel;
			
			PropertyConfigurator.configure(lpPropiedadesLog);

			rootLogger.warn("Componente de trazas inicializado correctamente con el nivel " + nivel + " habilitado...");
		}

		catch (Exception ex) {
			System.err.println("Excepcion al inicializar el componente de trazas a partir de una properties");
			ex.printStackTrace();
		}
	}

	public static Properties inicializar(Properties lpPropiedadesLog) {

		try {

			// Configuro el logger a partir del Properties que me envian. De
			// esta forma puedo hacerlo en caliente

			PropertyConfigurator.configure(lpPropiedadesLog);
			rootLogger.warn("Componente de trazas inicializado correctamente desde la clase Properties...");
		}

		catch (Exception ex) {
			System.err.println("Excepcion al inicializar el componente de trazas a partir de una properties");
			ex.printStackTrace();
			return null;
		}

		return lpPropiedadesLog;
	}

	public static Logger getLogger(String lpCategoria) {

		// Obtiene la instancia de Logger para esa categoria. Si no existia, la
		// crea y la devuelvo.

		return Logger.getLogger(lpCategoria);

	}

	public static Logger getLogger() {

		// Si no es necesario una categoria especifica, crearemos una por
		// defecto.

		return rootLogger;
	}

	public static Properties inicializar(String lpFicheroProps) {

		// Ahora configuro el logger

		Properties lvPropiedadesLog = null;

		try {

			// Cargo, desde el fichero de propiedades, una Properties con los
			// valores del fichero que me han pasado.
			// El fichero debe estar en el classpath de la aplicacion, para no
			// buscar en rutas concretas.

			lvPropiedadesLog = new Properties();
			InputStream lvEntradas = ClassLoader.getSystemResourceAsStream(lpFicheroProps);

			if (lvEntradas == null) {
				System.err.println("Error al buscar el fichero de configuracion " + lpFicheroProps
						+ " del classpath de la aplicacion");
				return null;
			}

			lvPropiedadesLog.load(lvEntradas);

			// Configuro todas las instancias del logger que despues ire
			// creando.

			PropertyConfigurator.configure(lvPropiedadesLog);

			// Logger configurado

			rootLogger.warn(
					"Componente de trazas inicializado correctamente desde el fichero " + lpFicheroProps + "...");

		}

		catch (IOException ex) {
			System.err.println("Error al cargar el fichero de configuracion " + lpFicheroProps
					+ " del classpath de la aplicacion");
			ex.printStackTrace();
			return null;
		}

		catch (Exception ex) {
			System.err.println("Excepcion al inicializar el componente de trazas");
			ex.printStackTrace();
			return null;
		}

		// Devuelvo el properties, por si se quiere modificar posteriormente y
		// refrescar en caliente la configuracion del logger.

		return lvPropiedadesLog;
	}
}
