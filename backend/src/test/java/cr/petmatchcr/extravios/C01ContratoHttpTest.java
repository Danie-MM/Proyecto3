package cr.petmatchcr.extravios;

import cr.petmatchcr.extravios.controllers.ErroresExtravios;
import cr.petmatchcr.extravios.controllers.PreferenciaUbicacionController;
import cr.petmatchcr.extravios.dto.PreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.service.ErrorUbicacion;
import cr.petmatchcr.extravios.service.PreferenciaUbicacionService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revisa forma HTTP y alias; la autenticación y CSRF se prueban con la cadena completa de integración. */
class C01ContratoHttpTest {
    private PreferenciaUbicacionService servicio;
    private MockMvc mvc;

    @BeforeEach
    void prepararControlador() {
        servicio = mock(PreferenciaUbicacionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PreferenciaUbicacionController(servicio))
                .setControllerAdvice(new ErroresExtravios()).build();
    }

    @Test
    void eliminarSinVersionDevuelveElErrorComunYNoLlamaAlServicio() throws Exception {
        mvc.perform(delete("/api/v1/mi-ubicacion"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.erroresCampos").isArray())
                .andExpect(jsonPath("$.trazaId").isString());
        verifyNoInteractions(servicio);
    }

    @Test
    void ambasRutasDistinguenLaAusenciaSinCachearDatosPrivados() throws Exception {
        when(servicio.consultar()).thenReturn(PreferenciaUbicacionDTO.sinConfigurar(UUID.randomUUID()));
        for (String ruta : new String[] {"/api/v1/mi-ubicacion", "/api/extravios/ubicacion"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.configurada").value(false))
                    .andExpect(jsonPath("$.version").doesNotExist());
        }
    }

    @Test
    void versionObsoletaDevuelve409ConCodigoDistinguible() throws Exception {
        doThrow(new ErrorUbicacion(409, "VERSION_DESACTUALIZADA", "Recargá la ubicación."))
                .when(servicio).eliminar(0);
        mvc.perform(delete("/api/v1/mi-ubicacion").param("versionEsperada", "0"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_DESACTUALIZADA"));
    }
}
