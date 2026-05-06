package com.smartmess.service;

import com.smartmess.dto.MessDto;

import com.smartmess.dto.UserDto;
import com.smartmess.entity.Mess;
import com.smartmess.repository.MessRepository;
import com.smartmess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final MessRepository messRepository;
    private final UserRepository userRepository;
    private final MessService messService;
    private final UserService userService;

    public List<MessDto> getAllMesses() {
        return messRepository.findAll().stream()
                .map(mess -> messService.getMessById(mess.getId()))
                .collect(Collectors.toList());
    }

    public List<UserDto> getAllOwners() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("ROLE_OWNER"))
                .map(userService::toUserDto)
                .collect(Collectors.toList());
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userService::toUserDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessDto approveMess(UUID messId, boolean approve) {
        Mess mess = messService.findById(messId);
        mess.setApproved(approve);
        messRepository.save(mess);
        log.info("Mess {} approval set to {} by super admin", messId, approve);
        return messService.getMessById(messId);
    }

    public long getTotalMesses() {
        return messRepository.count();
    }

    public long getTotalUsers() {
        return userRepository.count();
    }

    @Transactional
    public UserDto setUserActive(UUID userId, boolean active) {
        var user = userService.findById(userId);
        user.setActive(active);
        userRepository.save(user);
        log.info("User {} active status changed to {}", userId, active);
        return userService.toUserDto(user);
    }
}
