package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.service.ErrorMascota;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.CRC32;

/** Conserva los datos visuales originales. EXIF/GPS, comentarios, miniaturas y
 * bytes anexados al archivo no forman parte de la descarga pública. */
final class ImagenPublica {
    private static final Set<String> PNG_VISUAL = Set.of("IHDR", "PLTE", "tRNS", "IDAT", "IEND",
            "sRGB", "gAMA", "cHRM", "sBIT", "bKGD");

    private ImagenPublica() {}

    static byte[] sinMetadatosPrivados(byte[] original, String mime) {
        return "image/png".equals(mime) ? png(original) : jpeg(original);
    }

    private static byte[] png(byte[] bytes) {
        var salida = new ByteArrayOutputStream(bytes.length);
        if (bytes.length < 8) throw ilegible();
        salida.write(bytes, 0, 8);
        var datos = ByteBuffer.wrap(bytes);
        int inicio = 8;
        while (inicio <= bytes.length - 12) {
            int longitud = datos.getInt(inicio);
            if (longitud < 0 || longitud > bytes.length - inicio - 12) throw ilegible();
            String tipo = new String(bytes, inicio + 4, 4, StandardCharsets.US_ASCII);
            var crc = new CRC32();
            crc.update(bytes, inicio + 4, longitud + 4);
            if ((int)crc.getValue() != datos.getInt(inicio + 8 + longitud)) throw ilegible();
            if (PNG_VISUAL.contains(tipo)) salida.write(bytes, inicio, longitud + 12);
            if ("IEND".equals(tipo)) {
                if (longitud != 0) throw ilegible();
                return salida.toByteArray();
            }
            inicio += longitud + 12;
        }
        throw ilegible();
    }

    private static byte[] jpeg(byte[] bytes) {
        if (bytes.length < 2 || valor(bytes[0]) != 255 || valor(bytes[1]) != 0xd8) throw ilegible();
        var salida = new ByteArrayOutputStream(bytes.length);
        salida.write(bytes, 0, 2);
        int pos = 2;
        while (pos < bytes.length) {
            int inicio = pos;
            if (valor(bytes[pos]) != 255) throw ilegible();
            while (pos < bytes.length && valor(bytes[pos]) == 255) pos++;
            if (pos == bytes.length) throw ilegible();
            int marcador = valor(bytes[pos++]);
            if (marcador == 0xd9) {
                salida.write(255);
                salida.write(marcador);
                return salida.toByteArray();
            }
            if (marcador == 1) {
                salida.write(bytes, inicio, pos - inicio);
                continue;
            }
            if (marcador == 0 || marcador >= 0xd0 && marcador <= 0xd8 || pos > bytes.length - 2)
                throw ilegible();
            int longitud = (valor(bytes[pos]) << 8) | valor(bytes[pos + 1]);
            if (longitud < 2 || longitud > bytes.length - pos) throw ilegible();
            // APP14 Adobe estándar describe la transformación de color de imágenes CMYK.
            boolean adobe = marcador == 0xee && longitud == 14 &&
                    new String(bytes, pos + 2, 5, StandardCharsets.US_ASCII).equals("Adobe");
            boolean aplicacion = marcador >= 0xe0 && marcador <= 0xef;
            if ((!aplicacion && marcador != 0xfe) || adobe)
                salida.write(bytes, inicio, pos + longitud - inicio);
            pos += longitud;
            if (marcador == 0xda) {
                int fin = finDeBarrido(bytes, pos);
                salida.write(bytes, pos, fin - pos);
                pos = fin;
            }
        }
        throw ilegible();
    }

    // En los datos comprimidos FF00 significa un byte literal. Los marcadores RST
    // pertenecen al barrido; el resto vuelve al procesamiento de segmentos JPEG.
    private static int finDeBarrido(byte[] bytes, int pos) {
        while (pos < bytes.length) {
            int inicio = pos;
            if (valor(bytes[pos++]) != 255) continue;
            while (pos < bytes.length && valor(bytes[pos]) == 255) pos++;
            if (pos == bytes.length) throw ilegible();
            int marcador = valor(bytes[pos++]);
            if (marcador != 0 && (marcador < 0xd0 || marcador > 0xd7)) return inicio;
        }
        throw ilegible();
    }

    private static int valor(byte b) { return b & 0xff; }
    private static ErrorMascota ilegible() {
        return new ErrorMascota(415, "FORMATO_FOTO_INVALIDO", "Elegí una fotografía JPEG o PNG completa.");
    }
}
