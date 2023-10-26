package co.com.bancolombia.model.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
//@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Validation {
    private Company company;
    private boolean existInCamaraComercio;
    private CreditRating creditRatingDataCredito;
    private List<Restriction> restrictions;
    private Report reportSuperIntendencia;
}
