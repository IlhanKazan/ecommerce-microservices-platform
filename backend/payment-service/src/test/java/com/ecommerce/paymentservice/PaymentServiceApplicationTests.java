package com.ecommerce.paymentservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Tam çalışan dev ortamı gerektirir — CI'da çalıştır, lokalda -DskipTests kullan")
@SpringBootTest
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
