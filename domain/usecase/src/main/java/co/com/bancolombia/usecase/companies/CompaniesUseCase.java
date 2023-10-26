package co.com.bancolombia.usecase.companies;

import co.com.bancolombia.model.company.Company;
import co.com.bancolombia.model.company.Validation;
import co.com.bancolombia.model.company.gateways.CompanyServicesGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CompaniesUseCase {

    private final CompanyServicesGateway servicesGateway;

    /**
     * TODO: implementar toda la logica de validacion definida en el Dojo
     * @param company la informacion de la compania a validar
     * @return un objeto Validation con todas las validaciones
     */
    public Mono<Validation> validateCompany(Company company) {
        return Mono.error(IllegalStateException::new);
    }


}
