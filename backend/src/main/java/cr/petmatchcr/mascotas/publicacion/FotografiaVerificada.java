package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.service.ErrorMascota;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

record FotografiaVerificada(String mime, byte[] contenido) {
    static final int LIMITE = 5_000_000;

    static FotografiaVerificada leer(MultipartFile archivo) {
        if (archivo.getSize() > LIMITE) throw demasiadoGrande();
        byte[] bytes;
        try (var entrada = archivo.getInputStream()) {
            bytes = entrada.readNBytes(LIMITE + 1);
        } catch (IOException ex) {
            throw new ErrorMascota(400, "ARCHIVO_ILEGIBLE", "No pudimos leer el archivo.");
        }
        if (bytes.length > LIMITE) throw demasiadoGrande();
        // El nombre y el Content-Type vienen del navegador. El lector identifica y
        // decodifica la imagen; se submuestrea para acotar la memoria de salida.
        try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw formato();
            ImageReader lector = lectores.next();
            try {
                String mime = switch (lector.getFormatName().toUpperCase(java.util.Locale.ROOT)) {
                    case "PNG" -> "image/png";
                    case "JPEG" -> "image/jpeg";
                    default -> throw formato();
                };
                lector.setInput(entrada, true, true);
                lector.addIIOReadWarningListener((origen, aviso) -> { throw formato(); });
                var parametros = lector.getDefaultReadParam();
                int pasoX = Math.max(1, Math.ceilDiv(lector.getWidth(0), 1024));
                int pasoY = Math.max(1, Math.ceilDiv(lector.getHeight(0), 1024));
                parametros.setSourceSubsampling(pasoX, pasoY, 0, 0);
                lector.read(0, parametros);
                // Verifica también el final de la imagen y la integridad de cada bloque PNG.
                ImagenPublica.sinMetadatosPrivados(bytes, mime);
                return new FotografiaVerificada(mime, bytes);
            } finally {
                lector.dispose();
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw formato();
        }
    }

    private static ErrorMascota formato() {
        return new ErrorMascota(415, "FORMATO_FOTO_INVALIDO", "Elegí una fotografía JPEG o PNG válida.");
    }

    private static ErrorMascota demasiadoGrande() {
        return new ErrorMascota(413, "FOTO_DEMASIADO_GRANDE", "La fotografía supera 5 000 000 bytes.");
    }
}
