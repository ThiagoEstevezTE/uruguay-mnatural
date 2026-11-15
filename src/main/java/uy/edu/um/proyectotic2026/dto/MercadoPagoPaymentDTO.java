package uy.edu.um.proyectotic2026.dto;

import lombok.Getter;
import lombok.Setter;

// Lo que manda el formulario de Mercado Pago despues de que el turista carga la tarjeta.
// El numero de tarjeta nunca llega a nuestro servidor: solo este token.
@Getter
@Setter
public class MercadoPagoPaymentDTO {

    private String token;
    private String paymentMethodId;
    private String paymentTypeId;     // credit_card o debit_card
    private String cardLastDigits;    // solo para mostrar en la reserva
    private String issuerId;
    private Integer installments;
    private String payerEmail;
    private String identificationType;
    private String identificationNumber;
}