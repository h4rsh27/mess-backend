package com.smartmess.service;

import com.smartmess.dto.CreateFeedbackRequest;
import com.smartmess.dto.FeedbackDto;
import com.smartmess.entity.Attendance;
import com.smartmess.entity.Feedback;
import com.smartmess.entity.User;
import com.smartmess.repository.AttendanceRepository;
import com.smartmess.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserService userService;
    private final MenuService menuService;
    private final MessService messService;

    @Transactional
    public FeedbackDto createFeedback(UUID userId, UUID messId, CreateFeedbackRequest request) {
        UUID menuItemId = request.getMenuItemId();
        UUID attendanceId = request.getAttendanceId();

        if (attendanceId != null) {
            Attendance attendance = attendanceRepository.findById(attendanceId)
                    .orElseThrow(() -> new IllegalArgumentException("Attendance record not found for feedback."));
            if (!attendance.getUserId().equals(userId)) {
                throw new SecurityException("You can only submit feedback for your own consumed meals.");
            }
            if (feedbackRepository.findByAttendanceId(attendanceId).isPresent()) {
                throw new IllegalStateException("Feedback has already been submitted for this meal.");
            }
            menuItemId = attendance.getMenuItemId();
            messId = attendance.getMessId();
        }

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .messId(messId)
                .menuItemId(menuItemId)
                .attendanceId(attendanceId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        feedback = feedbackRepository.save(feedback);

        Double avg = feedbackRepository.avgRatingByMessId(messId);
        if (avg != null) {
            messService.updateRating(messId, avg);
        }

        log.info("Feedback captured for user={} attendance={} item={}", userId, attendanceId, menuItemId);
        return toDto(feedback);
    }

    public List<FeedbackDto> getMessFeedback(UUID messId) {
        return feedbackRepository.findByMessIdOrderByCreatedAtDesc(messId)
                .stream().map(this::toDto).toList();
    }

    public List<FeedbackDto> getMyFeedback(UUID userId) {
        return feedbackRepository.findByUserId(userId)
                .stream().map(this::toDto).toList();
    }

    private FeedbackDto toDto(Feedback feedback) {
        User user = userService.findById(feedback.getUserId());
        String itemName = feedback.getMenuItemId() != null
                ? menuService.findById(feedback.getMenuItemId()).getName() : null;
        return FeedbackDto.builder()
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .userName(user.getName())
                .menuItemId(feedback.getMenuItemId())
                .menuItemName(itemName)
                .messId(feedback.getMessId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
