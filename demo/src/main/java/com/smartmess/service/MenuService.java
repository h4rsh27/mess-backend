package com.smartmess.service;

import com.smartmess.dto.CreateMenuItemRequest;
import com.smartmess.dto.MenuItemDto;
import com.smartmess.entity.MenuItem;
import com.smartmess.exception.ResourceNotFoundException;

import com.smartmess.repository.FeedbackRepository;
import com.smartmess.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final FeedbackRepository feedbackRepository;


    @Transactional
    public MenuItemDto createMenuItem(UUID messId, CreateMenuItemRequest request) {
        MenuItem item = MenuItem.builder()
                .messId(messId)
                .name(request.getName())
                .mealType(request.getMealType())
                .date(request.getDate())
                .dayOfWeek(request.getDayOfWeek())
                .preparedQuantity(request.getPreparedQuantity())
                .description(request.getDescription())
                .isAvailable(true)
                .build();
        return toDto(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemDto updateMenuItem(UUID itemId, UUID messId, CreateMenuItemRequest request) {
        MenuItem item = getItemOwnedByMess(itemId, messId);
        item.setName(request.getName());
        item.setMealType(request.getMealType());
        item.setDate(request.getDate());
        item.setDayOfWeek(request.getDayOfWeek());
        item.setPreparedQuantity(request.getPreparedQuantity());
        item.setDescription(request.getDescription());
        return toDto(menuItemRepository.save(item));
    }

    @Transactional
    public void deleteMenuItem(UUID itemId, UUID messId) {
        MenuItem item = getItemOwnedByMess(itemId, messId);
        menuItemRepository.delete(item);
    }

    public List<MenuItemDto> getTodayMenu(UUID messId) {
        return getTodayMenuEntities(messId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<MenuItem> getTodayMenuEntities(UUID messId) {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        return menuItemRepository.findTodayMenu(messId, today, dayOfWeek);
    }

    public List<MenuItemDto> getWeeklyMenu(UUID messId) {
        return menuItemRepository.findByMessId(messId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public MenuItem findById(UUID itemId) {
        return menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId.toString()));
    }

    private MenuItem getItemOwnedByMess(UUID itemId, UUID messId) {
        MenuItem item = findById(itemId);
        if (!item.getMessId().equals(messId)) {
            throw new SecurityException("Access denied: item does not belong to your mess");
        }
        return item;
    }

    public MenuItemDto toDto(MenuItem item) {
        Double avgRating = feedbackRepository.avgRatingByMenuItemId(item.getId());
        long totalFeedbacks = feedbackRepository.countByMenuItemId(item.getId());
        return MenuItemDto.builder()
                .id(item.getId())
                .messId(item.getMessId())
                .name(item.getName())
                .mealType(item.getMealType())
                .date(item.getDate())
                .dayOfWeek(item.getDayOfWeek())
                .isAvailable(item.isAvailable())
                .preparedQuantity(item.getPreparedQuantity())
                .description(item.getDescription())
                .avgRating(avgRating != null ? avgRating : 0.0)
                .totalFeedbacks((int) totalFeedbacks)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
