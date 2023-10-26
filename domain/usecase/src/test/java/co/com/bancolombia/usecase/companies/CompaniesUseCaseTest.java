package co.com.bancolombia.usecase.companies;

import co.com.bancolombia.model.company.*;
import co.com.bancolombia.model.company.exceptions.CompanyGenericException;
import co.com.bancolombia.model.company.exceptions.CompanyNotFoundException;
import co.com.bancolombia.model.company.gateways.CompanyServicesGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompaniesUseCaseTest {

    @InjectMocks
    CompaniesUseCase useCase;

    @Mock
    CompanyServicesGateway companyServicesGatewayMock;

    /**
     * instrumentacion de los mocks para los diferentes escenarios a probar
     */
    @BeforeEach
    void prepareMocks() {

        // Cuando se consulte la empresa 'Panaderia Acme' en camara de comercio el publisher no
        // va a emitir ningun valor. Con cualquier otro nombre de empresa, el publisher
        // emitira un TRUE
        when(companyServicesGatewayMock.validateInCamaraComercio(any(Company.class)))
                .thenAnswer(invocation -> {
                    Company company = invocation.getArgument(0);
                    if ("Panaderia Acme".equalsIgnoreCase(company.getName())) {
                        return Mono.empty();
                    }
                    return Mono.just(true);
                });

        // Cuando se consulte la empresa 'Minimercado Especial' en bancolombia el publisher va a emitir
        // un error. Con cualquier otro nombre el publisher emitira un objeto Restriction.
        lenient().when(companyServicesGatewayMock.getRestrictionsBancolombia(any(Company.class)))
                .thenAnswer(invocation -> {
                    Company company = invocation.getArgument(0);
                    if ("Minimercado Especial".equalsIgnoreCase(company.getName())) {
                        return Mono.error(new RuntimeException("opsss"));
                    } else {
                        return Mono.just(Collections.singletonList(Restriction.builder()
                                .name("blah")
                                .build()));
                    }
                });

        // Cuando se consulte la empresa "Verduras Frescas" en datacredito se va a generar una excepcion.
        // Con cualquier otro nombre devolvera CreditRating.LOW_RISK.
        lenient().when(companyServicesGatewayMock.getStateDataCredito(any(Company.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(200);
                    Company company = invocation.getArgument(0);
                    if ("Verduras Frescas".equalsIgnoreCase(company.getName())) {
                        return new RuntimeException("Error no esperado");
                    } else {
                        return CreditRating.LOW_RISK;
                    }
                });

        // En este caso el mock de consulta super intendencia siempre emite un Reporte
        lenient().when(companyServicesGatewayMock.getReportSuperIntendencia(any(Company.class)))
                .thenReturn(Mono.just(Report.builder().name("foobar").build()));
    }

    @Test
    void shouldPerformAllValidationsNoErrors() {

        Company company = Company.builder().name("Ferreteria Especial").build();

        // El publisher que vamos a probar
        Mono<Validation> validationPublisher = useCase.validateCompany(company);

        StepVerifier.create(validationPublisher)
                .expectSubscription()
                .expectNextMatches(validation -> {
                    // Aqui las validaciones de la respuesta
                    assertNotNull(validation.getCompany());
                    assertTrue(validation.isExistInCamaraComercio());
                    assertEquals(CreditRating.LOW_RISK, validation.getCreditRatingDataCredito());
                    assertNotNull(validation.getReportSuperIntendencia());
                    assertNotNull(validation.getRestrictions());
                    return true;
                })
                .verifyComplete();

        // verificamos que los metodos del gateway hayan sido invocados solo una vez
        verify(companyServicesGatewayMock, times(1)).validateInCamaraComercio(any(Company.class));
        verify(companyServicesGatewayMock, times(1)).getRestrictionsBancolombia(any(Company.class));
        verify(companyServicesGatewayMock, times(1)).getStateDataCredito(any(Company.class));
        verify(companyServicesGatewayMock, times(1)).getReportSuperIntendencia(any(Company.class));
    }

    @Test
    void shouldHandleInnerExceptionWithDefaultValue() {

        Company company = Company.builder().name("Verduras Frescas").build();

        // El publisher que vamos a probar
        Mono<Validation> validationPublisher = useCase.validateCompany(company);

        StepVerifier.create(validationPublisher)
                .expectSubscription()
                .expectNextMatches(validation -> {
                    assertNotNull(validation.getCompany());
                    assertTrue(validation.isExistInCamaraComercio());
                    assertEquals(CreditRating.MID_RISK, validation.getCreditRatingDataCredito());
                    assertNotNull(validation.getReportSuperIntendencia());
                    assertNotNull(validation.getRestrictions());
                    return true;
                })
                .verifyComplete();

        // Verificamos que estos metodos hayan sido invocados
        verify(companyServicesGatewayMock, times(1)).validateInCamaraComercio(any(Company.class));
        verify(companyServicesGatewayMock, times(1)).getRestrictionsBancolombia(any(Company.class));
        verify(companyServicesGatewayMock, times(1)).getStateDataCredito(any(Company.class));
        verify(companyServicesGatewayMock, times(1)).getReportSuperIntendencia(any(Company.class));
    }

    @Test
    void shouldHandleCompanyNotFound() {

        Company company = Company.builder().name("Panaderia Acme").build();

        // El publisher que vamos a probar
        Mono<Validation> validationPublisher = useCase.validateCompany(company);

        StepVerifier.create(validationPublisher)
                .expectSubscription()
                .verifyError(CompanyNotFoundException.class);

        // Verificamos que el sgte metodo haya sido invocado
        verify(companyServicesGatewayMock, times(1)).validateInCamaraComercio(any(Company.class));
        // De resto, verificamos que ninguno de estos debio ser invocado
        verify(companyServicesGatewayMock, times(0)).getRestrictionsBancolombia(any(Company.class));
        verify(companyServicesGatewayMock, times(0)).getStateDataCredito(any(Company.class));
        verify(companyServicesGatewayMock, times(0)).getReportSuperIntendencia(any(Company.class));
    }

    @Test
    void shouldHandleUnwantedExceptionAndMappingIt() {

        Company company = Company.builder().name("Minimercado Especial").build();

        // El publisher que vamos a probar
        Mono<Validation> validationPublisher = useCase.validateCompany(company);

        StepVerifier.create(validationPublisher)
                .expectSubscription()
                .verifyError(CompanyGenericException.class);

        // Verificamos que estos metodos hayan sido invocados
        verify(companyServicesGatewayMock, times(1)).validateInCamaraComercio(any(Company.class));
        verify(companyServicesGatewayMock, times(1)).getRestrictionsBancolombia(any(Company.class));

        // De resto, verificamos que ninguno de estos debio ser invocado
        verify(companyServicesGatewayMock, times(0)).getStateDataCredito(any(Company.class));
        verify(companyServicesGatewayMock, times(0)).getReportSuperIntendencia(any(Company.class));
    }


}
