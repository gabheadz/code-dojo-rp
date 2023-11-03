package co.com.bancolombia.usecase.companies;

import co.com.bancolombia.model.company.Company;
import co.com.bancolombia.model.company.CreditRating;
import co.com.bancolombia.model.company.Report;
import co.com.bancolombia.model.company.Validation;
import co.com.bancolombia.model.company.gateways.CompanyServicesGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
public class CompaniesUseCase {

    private final CompanyServicesGateway servicesGateway;

    /**
     * Logica de validacion definida en el Dojo
     * @param company la informacion de la compania a validar
     * @return un objeto Validation con todas las validaciones
     */
    public Mono<Validation> validateCompany(Company company) {
        return checkCompanyInCamaraComercio(company)
                // si la empresa no existe el proceso no debe continuar.
                .filter(Validation::isExistInCamaraComercio)
                .flatMap(this::searchRestrictionsInBanco)
                .flatMap(this::checkDataCreditoAndSuperInParallel);
    }

    private Mono<Validation> checkCompanyInCamaraComercio(Company company) {
        // se consume el gateway que consulta la camara de comercio
        // y si hay valor emitido, se arma el objeto Validation, si no hay
        // valor emitido se arma un Validation por defecto.
        return servicesGateway.validateInCamaraComercio(company)
                .map(doCompanyExist -> Validation.builder()
                        .company(company)
                        .existInCamaraComercio(doCompanyExist)
                        .build()
                )
                .defaultIfEmpty(Validation.builder()
                        .company(company)
                        .existInCamaraComercio(false)
                        .build());
    }

    private Mono<Validation> searchRestrictionsInBanco(Validation validation) {
        // se consume el gateway de consulta a las restricciones del banco.
        // si hay respuesta, se clona el objeto validation, y a la copia se le
        // asigna el resultado.
        return servicesGateway.getRestrictionsBancolombia(validation.getCompany())
                .map(result -> validation.toBuilder()
                        .restrictions(result)
                        .build()
                );
    }

    private Mono<Validation> checkDataCreditoAndSuperInParallel(Validation validation) {

        // Se consumira el gateway de consulta a la superintendencia.
        Mono<Report> reporteSuperPublisher = servicesGateway.getReportSuperIntendencia(validation.getCompany());

        // Se consumira el gateway de consulta a datacredito.
        Mono<CreditRating> reporteDataCreditoPublisher = Mono.fromCallable(() ->
                        servicesGateway.getStateDataCredito(validation.getCompany()))
                // por ser una operacion bloqueante se suscribe la operacion en el scheduler elastico
                .subscribeOn(Schedulers.boundedElastic());

        return Mono.zip(reporteSuperPublisher, reporteDataCreditoPublisher)
                .map(tuple -> {
                    var reporteSuper = tuple.getT1();
                    var creditRating = tuple.getT2();
                    return validation.toBuilder()
                            .creditRatingDataCredito(creditRating)
                            .reportSuperIntendencia(reporteSuper)
                            .build();
                });
    }

}
