package co.com.bancolombia.model.company.gateways;

import co.com.bancolombia.model.company.Company;
import co.com.bancolombia.model.company.CreditRating;
import co.com.bancolombia.model.company.Report;
import co.com.bancolombia.model.company.Restriction;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Gateway con todas las operaciones en un solo lugar, solo para efectos del Dojo
 */
public interface CompanyServicesGateway {

    /**
     * validar si la compania existe en Camara de comercio
     * @param e
     * @return
     */
    Mono<Boolean> validateInCamaraComercio(Company e);

    /**
     * obtener un listado de restricciones que tiene la compania en el banco.
     * @param e
     * @return
     */
    Mono<List<Restriction>> getRestrictionsBancolombia(Company e);

    /**
     * Obtener el estado de la compania en Data Credito. Se nos ha dicho
     * que esta operacion es Bloquante I/O.
     * @param e
     * @return
     */
    CreditRating getStateDataCredito(Company e);

    /**
     * Operacion para obtener un reporte de dicha compania en la superintendencia
     *
     * @param e
     * @return
     */
    Mono<Report> getReportSuperIntendencia(Company e);


}
