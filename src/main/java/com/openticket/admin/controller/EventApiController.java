package com.openticket.admin.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openticket.admin.dto.EventListItemDTO;
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
    public List<EventListItemDTO> getAllEvents() {
        Long companyId = 2L; // 先寫死，未來從 JWT 拿
        return eventService.getEventListItems(companyId);
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

            EventListItemDTO dto = new EventListItemDTO();
            dto.setId(saved.getId());
            dto.setTitle(saved.getTitle());
            dto.setEventStart(saved.getEventStartFormatted());
            dto.setEventEnd(saved.getEventEndFormatted());
            dto.setTicketStart(saved.getTicketStartFormatted());
            dto.setStatus(saved.getDynamicStatus());
            dto.setViews(0);
            dto.setTicketsSold(0);

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        Event event = eventService.findById(id);
        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("找不到活動 ID：" + id);
        }

        // 1. 活動描述
        EventDetail detail = eventDetailRepository.findByEventId(event.getId());
        String description = detail != null ? detail.getContent() : "";
        List<EventTicketRequest> selectedTickets = eventTicketTypeService.findByEventId(event.getId());

        // 3. 組合回傳 JSON（你可以用 Map 或自訂 Response DTO）
        Map<String, Object> result = new HashMap<>();
        result.put("id", event.getId());
        result.put("title", event.getTitle());
        result.put("address", event.getAddress());
        result.put("eventStart", event.getEventStart());
        result.put("eventEnd", event.getEventEnd());
        result.put("ticketStart", event.getTicketStart());
        result.put("description", description);
        result.put("selectedTickets", selectedTickets);
        result.put("images", event.getImages());
        result.put("createdAt", event.getCreatedAtIso());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @ModelAttribute Event updated,
            HttpServletRequest request) {

        try {
            Event event = eventService.findById(id);

            // 1. 不可編輯：已結束或已取消
            String status = event.getDynamicStatus();
            if ("已取消".equals(status) || "已結束".equals(status)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("此活動狀態為「" + status + "」，不可編輯");
            }

            // 2. 修改基本資訊
            event.setTitle(updated.getTitle());
            event.setAddress(updated.getAddress());
            event.setEventStart(updated.getEventStart());
            event.setEventEnd(updated.getEventEnd());
            event.setTicketStart(updated.getTicketStart());

            // 3. 修改 event_detail（活動描述）
            String content = request.getParameter("description");
            eventService.updateDetail(event, content);

            // 4. 修改活動票種（方案 B：完全重建）
            String json = request.getParameter("eventTicketsJson");
            if (json != null && !json.isBlank()) {

                ObjectMapper mapper = new ObjectMapper();
                List<EventTicketRequest> list = mapper.readValue(json, new TypeReference<List<EventTicketRequest>>() {
                });

                eventTicketTypeService.rebuildEventTickets(event, list);
            }

            eventService.save(event);

            return ResponseEntity.ok("活動已成功更新");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新活動失敗：" + e.getMessage());
        }
    }

}
