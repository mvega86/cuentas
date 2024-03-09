package mvega.dev.cuentas.src.service.mapper.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdCasaDTO {
    private String calle;
    private String numero;
    private String escalera;
    private String piso;
    private String letra;
    private String ciudad;
    private String provincia;
}
