package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uy.edu.um.proyectotic2026.entities.Provider;
import uy.edu.um.proyectotic2026.entities.ProviderType;

import java.util.List;

@Getter
@AllArgsConstructor

public class ProviderResponseDTO {

    private Long id;
    private String email;
    private String name;
    private String document;
    private String phone;
    private ProviderType type;
    private List<BusinessResponseDTO> businesses;

    public static ProviderResponseDTO from(Provider provider) {
        return new ProviderResponseDTO(
                provider.getId(),
                provider.getEmail(),
                provider.getName(),
                provider.getDocument(),
                provider.getPhone(),
                provider.getType(),
                provider.getBusinesses().stream()
                        .map(BusinessResponseDTO::from)
                        .toList()
        );
    }

}
