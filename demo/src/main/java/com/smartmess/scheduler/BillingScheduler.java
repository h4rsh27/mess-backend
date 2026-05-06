package com.smartmess.scheduler;

import com.smartmess.repository.MessRepository;
import com.smartmess.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingScheduler {

    private final MessRepository messRepository;
    private final PaymentService paymentService;

    /**
     * Auto-generate monthly bills for all active messes on the 1st of every month at midnight.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void generateMonthlyBills() {
        log.info("Running monthly bill generation scheduler...");
        messRepository.findByIsActiveTrue().forEach(mess -> {
            try {
                paymentService.generateMonthlyBillsForMess(mess.getId());
            } catch (Exception e) {
                log.error("Failed to generate bills for mess {}: {}", mess.getId(), e.getMessage());
            }
        });
        log.info("Monthly bill generation completed.");
    }
}
