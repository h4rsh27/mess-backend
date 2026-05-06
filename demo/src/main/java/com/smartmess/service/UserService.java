package com.smartmess.service;

import com.smartmess.dto.AuthResponse;
import com.smartmess.dto.LoginRequest;
import com.smartmess.dto.RegisterRequest;
import com.smartmess.dto.UserDto;
import com.smartmess.entity.User;
import com.smartmess.enums.ApprovalStatus;
import com.smartmess.enums.Role;

import com.smartmess.exception.ResourceNotFoundException;
import com.smartmess.repository.UserRepository;
import com.smartmess.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        if (request.getRole() == Role.ROLE_STUDENT && request.getPlanType() == null) {
            throw new IllegalArgumentException("Student registrations must include a plan type.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .planType(request.getRole() == Role.ROLE_STUDENT ? request.getPlanType() : null)
                .subscriptionPreference(request.getRole() == Role.ROLE_STUDENT
                        ? (request.getSubscriptionPreference() != null ? request.getSubscriptionPreference() : com.smartmess.enums.SubscriptionPreference.VEG)
                        : null)
                .phone(request.getPhone())
                .approvalStatus(request.getRole() == Role.ROLE_STUDENT
                        ? ApprovalStatus.PENDING : ApprovalStatus.APPROVED)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {} with role {}", user.getEmail(), user.getRole());

        String token = generateToken(user);
        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        String token = generateToken(user);
        return buildAuthResponse(token, user);
    }

    public UserDto getMyProfile(UUID userId) {
        User user = findById(userId);
        return toUserDto(user);
    }

    @Transactional
    public void joinMess(UUID userId, UUID messId) {
        User user = findById(userId);
        if (user.getRole() != Role.ROLE_STUDENT) {
            throw new IllegalStateException("Only students can join a mess.");
        }
        user.setMessId(messId);
        user.setApprovalStatus(ApprovalStatus.PENDING);
        userRepository.save(user);
        log.info("Student {} requested to join mess {}", userId, messId);
    }

    @Transactional
    public void approveStudent(UUID ownerId, UUID studentId, boolean approve) {
        User student = findById(studentId);
        student.setApprovalStatus(approve ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        userRepository.save(student);
        log.info("Student {} approval set to {} by owner {}", studentId, approve, ownerId);
    }

    public List<UserDto> getStudentsByMess(UUID messId) {
        return userRepository.findByMessIdAndRole(messId, Role.ROLE_STUDENT)
                .stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
    }

    private String generateToken(User user) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role", user.getRole().name(),
                "messId", user.getMessId() != null ? user.getMessId().toString() : ""
        );
        return jwtService.generateToken(user.getEmail(), claims);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .planType(user.getPlanType())
                .subscriptionPreference(user.getSubscriptionPreference())
                .messId(user.getMessId())
                .approvalStatus(user.getApprovalStatus())
                .build();
    }

    public UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .planType(user.getPlanType())
                .subscriptionPreference(user.getSubscriptionPreference())
                .messId(user.getMessId())
                .approvalStatus(user.getApprovalStatus())
                .phone(user.getPhone())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
