package com.movie.payment_service.provider;

import java.math.BigDecimal;
import java.util.UUID;

import com.movie.payment_service.entity.PaymentMethod;

public interface PaymentProvider {
    ProviderPayment create(UUID paymentId, BigDecimal amount, String currency, PaymentMethod paymentMethod);
    String refund(UUID refundId, String paymentProviderReference, BigDecimal amount, String currency);
}
