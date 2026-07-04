package com.kiwi.engine.config;

import com.kiwi.engine.entity.CashbackRule;
import com.kiwi.engine.entity.Transaction;
import com.kiwi.engine.entity.User;
import com.kiwi.engine.repository.CashbackRuleRepository;
import com.kiwi.engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*");
            }
        };
    }

    @Bean
    public CommandLineRunner seedData(UserRepository userRepo, CashbackRuleRepository ruleRepo) {
        return args -> {
            // Seed demo user if not exists
            if (userRepo.count() == 0) {
                userRepo.save(User.builder()
                        .name("Aditi Sharma")
                        .email("aditi@example.com")
                        .creditLimit(new BigDecimal("50000.00"))
                        .build());
                log.info("Seeded demo user");
            }

            // Seed cashback rules if not exists
            if (ruleRepo.count() == 0) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime far = LocalDateTime.now().plusYears(10);

                // UPI scan-and-pay: 5% across all categories, ₹500/month cap
                for (Transaction.Category cat : Transaction.Category.values()) {
                    ruleRepo.save(CashbackRule.builder()
                            .category(cat)
                            .paymentMode(Transaction.PaymentMode.UPI_SCAN)
                            .percentage(new BigDecimal("5.00"))
                            .monthlyCap(new BigDecimal("500.00"))
                            .validFrom(now)
                            .validTo(far)
                            .build());
                }

                // UPI online: 2%, ₹300/month cap
                for (Transaction.Category cat : Transaction.Category.values()) {
                    ruleRepo.save(CashbackRule.builder()
                            .category(cat)
                            .paymentMode(Transaction.PaymentMode.UPI_ONLINE)
                            .percentage(new BigDecimal("2.00"))
                            .monthlyCap(new BigDecimal("300.00"))
                            .validFrom(now)
                            .validTo(far)
                            .build());
                }

                // Online (card): 1.5%, ₹200/month cap
                for (Transaction.Category cat : Transaction.Category.values()) {
                    ruleRepo.save(CashbackRule.builder()
                            .category(cat)
                            .paymentMode(Transaction.PaymentMode.ONLINE)
                            .percentage(new BigDecimal("1.50"))
                            .monthlyCap(new BigDecimal("200.00"))
                            .validFrom(now)
                            .validTo(far)
                            .build());
                }

                log.info("Seeded {} cashback rules", ruleRepo.count());
            }
        };
    }
}
