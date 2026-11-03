# Origen del backend

El proyecto Maven se generó con Spring Initializr para Java 21 y Spring Boot 4.1.2. La versión del parent del POM corresponde al artefacto publicado en Maven Central. [Spring Initializr](https://start.spring.io/) · [Requisitos de Spring Boot 4.1.2](https://docs.spring.io/spring-boot/system-requirements.html)

Los scripts de Maven Wrapper 3.3.4 se distribuyen bajo la licencia Apache 2.0. La configuración fija Maven 3.9.16 y el SHA-256 del ZIP, verificado a partir de la suma SHA-512 publicada. [Licencia](licenses/Apache-2.0.txt) · [Aviso de Apache Maven Wrapper](licenses/NOTICE-maven-wrapper.txt) · [SHA-512 oficial](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip.sha512)

La migración SQL V1 procede del arranque anterior de PetMatchCR. Se conserva íntegra para consultar registros existentes y respetar el historial de Flyway.
