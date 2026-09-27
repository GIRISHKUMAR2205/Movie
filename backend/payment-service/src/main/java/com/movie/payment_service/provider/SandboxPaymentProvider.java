package com.movie.payment_service.provider;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.movie.payment_service.config.PaymentProviderProperties;
import com.movie.payment_service.entity.PaymentMethod;

@Component
public class SandboxPaymentProvider implements PaymentProvider {
    private final PaymentProviderProperties properties;

    public SandboxPaymentProvider(PaymentProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public ProviderPayment create(UUID paymentId, BigDecimal amount, String currency, PaymentMethod paymentMethod) {
        String reference = "sandbox_pay_" + paymentId;
        String baseUrl = properties.checkoutBaseUrl().replaceAll("/+$", "");
        return new ProviderPayment(reference, baseUrl + "/" + reference);
    }

    @Override
    public String refund(UUID refundId, String paymentProviderReference, BigDecimal amount, String currency) {
        return "sandbox_refund_" + refundId;
    }
}
