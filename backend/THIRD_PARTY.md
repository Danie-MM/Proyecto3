# Referencia docente y licencias

Base consultada: [Cenfotec-Apps/java-spring, rama scv01](https://github.com/Cenfotec-Apps/java-spring/tree/scv01), revisión `9312413aaae9654b7f51a37eb1a191024b1aead5`.

Se conservan Java 21, Spring Boot 3.2.5, el plugin de dependencias 1.1.4 y Gradle Wrapper 8.7. Los archivos del Wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) provienen de esa base; conservan los avisos de sus autores. El JAR se comprobó con el checksum oficial de Gradle. Se añade el checksum de la distribución y se amplía su tiempo de descarga.

La licencia Apache 2.0 de la base se conserva en [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt). Las modificaciones para PetMatchCR incluyen configuración Gradle, paquete `cr.petmatchcr`, PostgreSQL/Flyway, prueba de entorno, Docker y pruebas. No se copiaron los módulos de negocio, credenciales ni el login JWT del ejemplo docente. El contrato de acceso del equipo permanece pendiente de implementación por el bloque A.

PostgreSQL es la decisión provisional del equipo: Natanael debe registrar la confirmación del docente antes de extender los módulos. Estas versiones reproducen el acuerdo académico de arranque; no constituyen una revisión de aptitud para producción.
