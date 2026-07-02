// ============================================================
// FILE 02: MÃ NGUỒN TRƯỚC KHI REFACTOR
// Phiên bản cũ - Bài 03 - Có nhiều Code Smell
// ============================================================

// ============================================================
// Entity
// ============================================================
package com.abcbank.ekyc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String citizenId;

    @Column(nullable = false)
    private String status;

    // --- Constructors ---
    public Registration() {}

    // --- Getters & Setters (không dùng Lombok) ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// ============================================================
// DTO (Gộp chung Request/Response)
// ============================================================
package com.abcbank.ekyc.dto;

public class RegistrationRequest {

    private String fullName;
    private String phone;
    private String email;
    private String citizenId;

    // --- Constructors ---
    public RegistrationRequest() {}

    // --- Getters & Setters ---
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
}

public class RegistrationResponse {

    private String registrationId;
    private String message;

    public RegistrationResponse() {}

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

// ============================================================
// Repository
// ============================================================
package com.abcbank.ekyc.repository;

import com.abcbank.ekyc.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, String> {

    Registration findByCitizenId(String citizenId);

    Registration findByPhone(String phone);

    Registration findByEmail(String email);
}

// ============================================================
// Service
// ============================================================
package com.abcbank.ekyc.service;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;

public interface RegistrationService {
    RegistrationResponse register(RegistrationRequest request);
}

// ============================================================
// Service Implementation (CÓ NHIỀU VẤN ĐỀ)
// ============================================================
package com.abcbank.ekyc.service.impl;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.entity.Registration;
import com.abcbank.ekyc.repository.RegistrationRepository;
import com.abcbank.ekyc.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    // VẤN ĐỀ 1: Field Injection - không nên dùng @Autowired trực tiếp
    @Autowired
    private RegistrationRepository registrationRepository;

    @Override
    public RegistrationResponse register(RegistrationRequest request) {
        // VẤN ĐỀ 2: Validation trong Service - vi phạm Single Responsibility
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new RuntimeException("Ten khong duoc de trong");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new RuntimeException("SDT khong duoc de trong");
        }
        if (!request.getPhone().matches("^\\d{10}$")) {
            throw new RuntimeException("SDT khong hop le");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email khong duoc de trong");
        }
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Email khong hop le");
        }
        if (request.getCitizenId() == null || request.getCitizenId().trim().isEmpty()) {
            throw new RuntimeException("CCCD khong duoc de trong");
        }
        if (!request.getCitizenId().matches("^\\d{12}$")) {
            throw new RuntimeException("CCCD phai co 12 chu so");
        }

        // VẤN ĐỀ 3: Dùng RuntimeException - không có typed exception
        Registration existing = registrationRepository.findByCitizenId(request.getCitizenId());
        if (existing != null) {
            throw new RuntimeException("CCCD da ton tai");
        }

        existing = registrationRepository.findByPhone(request.getPhone());
        if (existing != null) {
            throw new RuntimeException("SDT da ton tai");
        }

        existing = registrationRepository.findByEmail(request.getEmail());
        if (existing != null) {
            throw new RuntimeException("Email da ton tai");
        }

        // VẤN ĐỀ 4: Không có Transactional - có thể gây inconsistent data
        Registration registration = new Registration();
        registration.setFullName(request.getFullName());
        registration.setPhone(request.getPhone());
        registration.setEmail(request.getEmail());
        registration.setCitizenId(request.getCitizenId());
        registration.setStatus("PENDING");

        registrationRepository.save(registration);

        // VẤN ĐỀ 5: Không có logging
        RegistrationResponse response = new RegistrationResponse();
        response.setRegistrationId(registration.getId());
        response.setMessage("Thanh cong");

        return response;
    }
}

// ============================================================
// Controller (CÓ NHIỀU VẤN ĐỀ)
// ============================================================
package com.abcbank.ekyc.controller;

import com.abcbank.ekyc.dto.RegistrationRequest;
import com.abcbank.ekyc.dto.RegistrationResponse;
import com.abcbank.ekyc.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ekyc")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    // VẤN ĐỀ 6: Thiếu @Valid
    // VẤN ĐỀ 7: Xử lý exception thủ công trong Controller
    // VẤN ĐỀ 8: Thiếu @RequestBody (vẫn chạy nhưng không rõ ràng)
    // VẤN ĐỀ 9: ResponseEntity không generic type
    @PostMapping("/register")
    public ResponseEntity register(@RequestBody RegistrationRequest request) {
        try {
            RegistrationResponse response = registrationService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // VẤN ĐỀ 10: Trả về lỗi dạng text, không phải JSON chuẩn
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Loi he thong");
        }
    }
}

// ============================================================
// TỔNG HỢP VẤN ĐỀ (CODE SMELLS & TECHNICAL DEBT)
// ============================================================
/*
1. Code Smell - Validation trong Service:
   - Service làm quá nhiều việc (kiểm tra dữ liệu + xử lý nghiệp vụ)
   - Vi phạm Single Responsibility Principle (SOLID)

2. Code Smell - Field Injection (@Autowired):
   - Không thể test dễ dàng với Mockito
   - Không rõ dependencies (không nhìn thấy qua constructor)

3. Code Smell - RuntimeException:
   - Không có typed exception dẫn đến khó xử lý riêng từng loại lỗi
   - Không có error code để frontend map

4. Code Smell - ResponseEntity không generic:
   - ResponseEntity<?> hoặc raw type gây mất type safety

5. Technical Debt - Không Logging:
   - Không thể trace request khi có lỗi
   - Không audit trail

6. Technical Debt - Không Transactional:
   - Có thể lưu dữ liệu không nhất quán nếu có lỗi

7. Technical Debt - Xử lý exception ở Controller:
   - Vi phạm DRY - mỗi controller sẽ xử lý lỗi khác nhau
   - Error response format không thống nhất

8. Vi phạm SOLID:
   - S: Service chứa cả validation + business + logging
   - O: Khó mở rộng vì logic validation cứng trong service
   - D: Phụ thuộc trực tiếp vào implementation qua @Autowired field

9. Security Issue:
   - Log số CCCD, SĐT đầy đủ (nếu có log) - lộ thông tin nhạy cảm
   - Error message có thể lộ thông tin hệ thống
*/
