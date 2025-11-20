package com.openticket.admin.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openticket.admin.dto.EventTicketRequest;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventDetail;
import com.openticket.admin.entity.EventStatus;
import com.openticket.admin.entity.EventTitlePage;
import com.openticket.admin.entity.User;
import com.openticket.admin.repository.EventDetailRepository;
import com.openticket.admin.repository.EventStatusRepository;
import com.openticket.admin.repository.UserRepository;
import com.openticket.admin.service.EventService;
import com.openticket.admin.service.EventTicketTypeService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/events")
public class EventApiController {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventStatusRepository eventStatusRepository;

    @Autowired
    private EventTicketTypeService eventTicketTypeService;

    @Autowired
    private EventDetailRepository eventDetailRepository;

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    @PostMapping("/create")
    public ResponseEntity<?> createEvent(
            @ModelAttribute Event event,
            @RequestParam("cover") MultipartFile coverFile,
            HttpServletRequest request) {

        try {
            // 1. 設定預設公司與狀態
            User defaultUser = userRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("使用者 ID 2 不存在"));
            event.setCompanyUser(defaultUser);

            // 1. 先抓時間欄位
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sale = event.getTicketStart();
            LocalDateTime start = event.getEventStart();
            LocalDateTime end = event.getEventEnd();

            // 2. 判斷要用哪個狀態 ID
            Long statusIdToUse;

            // 若時間資料不完整，給個安全預設值（你也可以選 1：未開放）
            if (sale == null || start == null || end == null) {
                statusIdToUse = 1L; // 或你自己決定一個安全值
            } else if (now.isBefore(sale)) {
                statusIdToUse = 1L; // 未開放
            } else if (now.isBefore(start)) {
                statusIdToUse = 4L; // 開放購票
            } else if (now.isBefore(end)) {
                statusIdToUse = 2L; // 活動進行中
            } else {
                statusIdToUse = 3L; // 已結束
            }

            // 3. 查表、設定到 event
            EventStatus status = eventStatusRepository.findById(statusIdToUse)
                    .orElseThrow(() -> new RuntimeException("狀態 ID " + statusIdToUse + " 不存在"));
            event.setStatusId(status);

            // 2. 儲存圖片
            String filename = UUID.randomUUID() + "_" + coverFile.getOriginalFilename();
            Path savePath = Paths.get("uploads/covers", filename);
            Files.createDirectories(savePath.getParent());
            Files.copy(coverFile.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

            EventTitlePage cover = new EventTitlePage();
            cover.setImageUrl("/uploads/covers/" + filename);
            cover.setEvent(event);
            event.getImages().add(cover);

            // 3. 儲存活動
            Event saved = eventService.createEvent(event);

            // 4. 解析活動票種 JSON
            String json = request.getParameter("eventTicketsJson");

            if (json != null && !json.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();

                List<EventTicketRequest> tickets = mapper.readValue(json,
                        new TypeReference<List<EventTicketRequest>>() {
                        });

                eventTicketTypeService.createForEvent(saved, tickets);
            }

            String content = request.getParameter("description");

            // 如果有填內容，就建立一筆 event_detail
            if (content != null && !content.isBlank()) {
                EventDetail detail = new EventDetail();
                detail.setEvent(saved);
                detail.setContent(content);
                eventDetailRepository.save(detail);
            }
            // 5. 回傳成功
            return ResponseEntity.ok(saved);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("圖片儲存失敗：" + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("活動建立失敗：" + e.getMessage());
        }
    }

}
