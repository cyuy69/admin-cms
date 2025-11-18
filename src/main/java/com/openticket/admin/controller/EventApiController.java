package com.openticket.admin.controller;

import com.openticket.admin.entity.CompanyProfile;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventStatus;
import com.openticket.admin.entity.EventTitlePage;
import com.openticket.admin.repository.CompanyProfileRepository;
import com.openticket.admin.repository.EventStatusRepository;
import com.openticket.admin.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventApiController {

    @Autowired
    private EventService eventService;

    // ✅ 加在這裡
    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private EventStatusRepository eventStatusRepository;

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    @PostMapping("/create")
    public ResponseEntity<?> createEvent(
            @ModelAttribute Event event,
            @RequestParam("cover") MultipartFile coverFile) {

        try {
            // ✅ 設定預設公司與狀態
            CompanyProfile defaultCompany = companyProfileRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("公司 ID 1 不存在"));
            EventStatus defaultStatus = eventStatusRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("狀態 ID 2 不存在"));

            event.setCompany(defaultCompany);
            event.setStatusId(defaultStatus);

            // 儲存圖片
            String filename = UUID.randomUUID() + "_" + coverFile.getOriginalFilename();
            Path savePath = Paths.get("uploads/covers", filename);
            Files.createDirectories(savePath.getParent());
            Files.copy(coverFile.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

            // 建立封面
            EventTitlePage cover = new EventTitlePage();
            cover.setImageUrl("/uploads/covers/" + filename);
            cover.setEvent(event);
            event.getImages().add(cover);

            // 儲存活動
            Event saved = eventService.createEvent(event);
            return ResponseEntity.ok(saved);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("圖片儲存失敗：" + e.getMessage());
        }
    }
}
