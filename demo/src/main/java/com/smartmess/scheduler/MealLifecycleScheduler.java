package com.smartmess.scheduler;

import com.smartmess.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MealLifecycleScheduler {

    private final BookingService bookingService;

    @Scheduled(cron = "0 */15 * * * *")
    public void markNoShows() {
        int updated = bookingService.markNoShowsForExpiredMeals();
        if (updated > 0) {
            log.info("Marked {} bookings as no-show after meal windows closed.", updated);
        }
    }
}
