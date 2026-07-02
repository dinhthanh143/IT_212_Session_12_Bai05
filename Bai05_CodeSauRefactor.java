// ============================================================
// FILE 03: MÃ NGUỒN SAU KHI REFACTOR
// Phiên bản đã refactor - Clean Code + Design Pattern + SOLID
// ============================================================

// ============================================================
// 1. ENTITY
// ============================================================
package com.abcbank.ekyc.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations", indexes = {
    @Index(name = "idx_citizen_id", columnList = "citizenId", unique = true),
    @Index(name = "idx_phone", columnList = "phone", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 15, unique = true)
    private String phone;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 12, unique = true)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// ============================================================
// 2. ENUM
// ============================================================
package com.abcbank.ekyc.entity;

public enum RegistrationStatus {
    PENDING_REGISTRATION, OCR_COMPLETED, FACE_VERIFIED,
    AWAITING_ACTIVATION, ACTIVE, REJECTED, EXPIRED
}

// ============================================================
// 3. DTO - REQUEST (Có Validation annotations)
// ============================================================
package com.abcbank.ekyc.dto;

import com.abcbank.ekyc.validator.ValidCitizenId;
import com.abcbank.ekyc.validator.ValidVietnamesePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2-100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @ValidVietnamesePhone
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được quá 100 ký tự")
    private String email;

    @NotBlank(message = "Số CCCD không được để trống")
    @ValidCitizenId
    private String citizenId;
}

// ============================================================
// 4. DTO - RESPONSE (Builder pattern)
// ============================================================
package com.abcbank.ekyc.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationResponse {
    private String registrationId;
    private String message;
    private String nextStep;
}

// ============================================================
// 5. DTO - ERROR RESPONSE CHUẨN (Global format)
// ============================================================
package com.abcbank.ekyc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private Map<String, String> details;
    private LocalDateTime timestamp;
    private String path;
}

// ============================================================
// 6. REPOSITORY (Spring Data JPA)
// ============================================================
package com.abcbank.ekyc.repository;

import com.abcbank.ekyc.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, String> {
    boolean existsByCitizenId(String citizenId);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    Optional<Registration> findByCitizenId(String citizenId);
}

// ============================================================
// 7. CUSTOM VALIDATOR - ANNOTATION
// ============================================================
package com.abcbank.ekyc.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CitizenIdValidator.class)
@Documented
public @interface ValidCitizenId {
    String message() default "So CCCD phai gom dung 12 chu so";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// ============================================================
// 8. CUSTOM VALIDATOR - IMPLEMENTATION
// ============================================================
package com.abcbank.ekyc.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CitizenIdValidator implements ConstraintValidator<ValidCitizenId, String> {
    private static final String CITIZEN_ID_REGEX = "^\\d{12}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value.matches(CITIZEN_ID_REGEX);
    }
}

// ============================================================
// 9. CUSTOM VALIDATOR - SĐT VIỆT NAM
// ============================================================
package com.abcbank.ekyc.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VietnamesePhoneValidator.class)
@Documented
public @interface ValidVietnamesePhone {
    String message() default "So dien thoai khong dung dinh dang VN (10 so, 03/05/07/08/09)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

package com.abcbank.ekyc.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VietnamesePhoneValidator implements ConstraintValidator<ValidVietnamesePhone, String> {
    private static final String PHONE_REGEX = "^(03|05|07|08|09)\\d{8}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value.matches(PHONE_REGEX);
    }
}

// ============================================================
// 10. EXCEPTION - TYPED EXCEPTIONS
// ============================================================
package com.abcbank.ekyc.exception;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {
    private final String field;
    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }
}

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// ============================================================
// 11. GLOBAL EXCEPTION HANDLER (@RestControllerAdvice)
// ============================================================
package com.abcbank.ekyc.exception;

import com.abcbank.ekyc.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, String> details = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Du lieu khong hop le")
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate: field={}", ex.getField());
        Map<String, String> details = new HashMap<>();
        details.put(ex.getField(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("DUPLICATE_RESOURCE")
                .message("Thong tin da ton tai")
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        log.error("Business error: code={}", ex.getErrorCode());
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("Da co loi xay ra, vui long thu lai sau")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// ============================================================
// 12. SERVICE INTERFACE
// ============================================================
package com.abcbank.ekyc.service;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;

public interface RegistrationService {
    RegistrationResponse register(RegistrationRequest request);
}

// ============================================================
// 13. SERVICE IMPLEMENTATION (Sạch - SOLID)
// ============================================================
package com.abcbank.ekyc.service.impl;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.entity.Registration;
import com.abcbank.ekyc.entity.RegistrationStatus;
import com.abcbank.ekyc.exception.DuplicateResourceException;
import com.abcbank.ekyc.repository.RegistrationRepository;
import com.abcbank.ekyc.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);
    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        log.info("Bat dau dang ky: phone={}, email={}",
            maskPhone(request.getPhone()), maskEmail(request.getEmail()));

        checkDuplicate(request);

        Registration registration = Registration.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .citizenId(request.getCitizenId())
                .status(RegistrationStatus.PENDING_REGISTRATION)
                .build();

        registration = registrationRepository.save(registration);
        log.info("Dang ky thanh cong: id={}", registration.getId());

        return RegistrationResponse.builder()
                .registrationId(registration.getId())
                .message("Dang ky thong tin thanh cong")
                .nextStep("OCR_VERIFICATION")
                .build();
    }

    private void checkDuplicate(RegistrationRequest request) {
        if (registrationRepository.existsByCitizenId(request.getCitizenId())) {
            throw new DuplicateResourceException("citizenId",
                "So CCCD da duoc dang ky trong he thong");
        }
        if (registrationRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("phone",
                "So dien thoai da duoc dang ky");
        }
        if (registrationRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("email",
                "Email da duoc dang ky");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        return email.substring(0, 2) + "***" + email.substring(email.indexOf("@"));
    }
}

// ============================================================
// 14. CONTROLLER (Sạch - Logging - @Valid)
// ============================================================
package com.abcbank.ekyc.controller;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ekyc")
@RequiredArgsConstructor
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request) {
        log.info("Nhan request dang ky eKYC");
        long startTime = System.currentTimeMillis();

        RegistrationResponse response = registrationService.register(request);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Xu ly thanh cong trong {}ms, id={}", duration, response.getRegistrationId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
