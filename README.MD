# TFM_Safe_Bootstrapping_Raspberry_Server

## Introducción

Este proyecto contiene el código a ejecutar en el servidor para realizar el proceso de bootstrapping seguro de una Raspberry. Se ha elaborado dentro del Trabajo Fin de Master (TFM) dentro del Master en Ciberseguridad de la UCAM (2018), y es complemento del proyecto hermano 'TFM_Safe_Bootstrapping_Raspberry_Device'. 

El bootstrapping es el proceso por el que un dispositivo IoT se registra por primera vez, de manera segura, contra la plataforma IoT contra la que posteriormente se comunicará. Para realizar ese registro, debemos identificar de manera inequívoca el dispositio, bien con un certificado digital de cliente, o bien con algún secreto compartido entre dispositivo y plataforma. Normalmente se suelen utilizan dos técnicas: 

+ Personalizar el firmware del dispositivo de manera que todos los firmwares sean distintos
+ Agregarle, a posteriori a la fabricación, el secreto o certificado. 

Ambas técnicas tienen sus complicaciones, pues obliga a llevar una política segura de gestión de los secretos en el proceso de fabricación, y aumenta el coste del mismo. 

El objetivo del proyecto es, usando un firmware igual entre todos los dispositivos, y sin introducirle ningún secreto o certificado a posteriori en la fabricación, que el dispositivo sea capaz de autoregistrarse, y conseguir su propio certificado cliente, distinto para cada dispositivo, con el que conectar de manera segura contra la plataforma IoT. 

El proyecto se ha desarrollado para ejecutarse en un servidor de aplicaciones J2EE. Contiene el código que será invocado por del dispositivo Raspberry, mediante HTTPS, donde éste enviará su identificador único cifrado, y un Certificate Signed Request (CSR) que será firmado en el servidor por la CA correspondiente. Este certificado posteriormente será el usado por el dispositivo IoT para comunicar con la plataforma IoT, que autoregistrará el certificado al pertenecer a una CA registrada. 

## Instalación

El proyecto tiene formato de Web Application J2EE, por tanto, se debe clonar el repositio de Github en local, y posteriormente mediante Maven: 
```
mvn install
```
Esto generará en el directorio target la war correspondiente, que se deberá desplegar en el servidor de aplicaciones deseado. 

Además se deben incluir las dependencias en el classpath del servidor de aplicaciones, se pueden consultar dentro del propio fichero Pom.xml

Una vez instalado, y antes de ejecutarlo por primera vez, se necesita generar la clave privada y pública de la CA que usaremos para firmar el CSR. Para ello usaremos openssl:

 + Para generar la clave privada: 

``` 
openssl genrsa -out rootCA_TFM_Ciberseguridad 2048
```

 + Para generar la clave pública autofirmada: 

```
openssl req -key rootCA_TFM_Ciberseguridad.key -new -x509 - days 7300 -out rootCA_TFM_Ciberseguridad.pem
```
Estos dos ficheros debemos alojarlos dentro de una carpeta "ks" en el raiz de la instancia del servidor de aplicaciones J2EE. 
