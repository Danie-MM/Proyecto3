package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.service.ErrorMascota;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.*;

class FotografiaVerificadaTest {
    static byte[] imagen(String formato) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB), formato, bytes);
        return bytes.toByteArray();
    }

    private static FotografiaVerificada leer(byte[] bytes) {
        return FotografiaVerificada.leer(new MockMultipartFile("archivo","archivo.txt","text/plain",bytes));
    }

    @ParameterizedTest
    @ValueSource(strings={"jpeg","png"})
    void reconoceElContenidoYDescartaElNombre(String tipo) throws Exception {
        byte[] imagen = imagen(tipo);
        assertThat(leer(imagen).mime()).isEqualTo("image/" + tipo);
        assertThatExceptionOfType(ErrorMascota.class)
                .isThrownBy(() -> leer(Arrays.copyOf(imagen, 20)))
                .extracting(ErrorMascota::getEstado).isEqualTo(415);
    }

    @ParameterizedTest
    @ValueSource(strings={"jpeg","png"})
    void laDescargaPublicaConservaLaImagenYEliminaLosMetadatos(String tipo) throws Exception {
        byte[] original = imagen(tipo);
        var privado = "GPS:ubicacion-ficticia".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var archivo = new ByteArrayOutputStream();
        var dato = new java.io.DataOutputStream(archivo);
        if (tipo.equals("jpeg")) {
            dato.write(original, 0, 2);
            dato.writeShort(0xffe1); // APP1 (EXIF/XMP).
            dato.writeShort(privado.length + 2);
            dato.write(privado);
            dato.write(original, 2, original.length - 2);
        } else {
            int finIHDR = 33;
            dato.write(original, 0, finIHDR);
            dato.writeInt(privado.length);
            byte[] etiqueta = "tEXt".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            var crc = new java.util.zip.CRC32();
            crc.update(etiqueta);
            crc.update(privado);
            dato.write(etiqueta);
            dato.write(privado);
            dato.writeInt((int)crc.getValue());
            dato.write(original, finIHDR, original.length-finIHDR);
        }
        byte[] verificada = leer(archivo.toByteArray()).contenido();
        byte[] publica = ImagenPublica.sinMetadatosPrivados(verificada,"image/"+tipo);
        var imagen = ImageIO.read(new java.io.ByteArrayInputStream(publica));
        assertThat(imagen.getWidth()).isEqualTo(4);
        assertThat(imagen.getHeight()).isEqualTo(3);
        assertThat(new String(publica,java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("ubicacion-ficticia");
        assertThat(verificada).containsSubsequence(privado);
    }


    @Test
    void elLimiteEsCincoMillonesDeBytesInclusivo() throws Exception {
        byte[] imagen = Arrays.copyOf(imagen("jpeg"), FotografiaVerificada.LIMITE);
        assertThat(leer(imagen).contenido()).hasSize(5_000_000);
        assertThatExceptionOfType(ErrorMascota.class).isThrownBy(() -> leer(new byte[5_000_001]))
                .extracting(ErrorMascota::getEstado).isEqualTo(413);
    }

    @Test
    void rechazaImagenDeOtroFormatoYTextoDisfrazado() throws Exception {
        var gif = imagen("gif");
        assertThatExceptionOfType(ErrorMascota.class).isThrownBy(() -> leer(gif))
                .extracting(ErrorMascota::getEstado).isEqualTo(415);
        assertThatExceptionOfType(ErrorMascota.class).isThrownBy(() -> leer(new byte[]{1,2,3,4}))
                .extracting(ErrorMascota::getEstado).isEqualTo(415);
    }
}
