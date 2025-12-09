package com.openticket.admin.controller.organizer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.openticket.admin.dto.EventTitleDTO;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventDetail;
import com.openticket.admin.entity.EventStatus;
import com.openticket.admin.entity.EventTitlePage;
import com.openticket.admin.entity.User;
import com.openticket.admin.repository.EventDetailRepository;
import com.openticket.admin.repository.EventStatusRepository;
import com.openticket.admin.repository.UserRepository;
import com.openticket.admin.repository.EventRepository;
import com.openticket.admin.service.DashboardService;
import com.openticket.admin.service.EventQueryService;
import com.openticket.admin.service.EventService;
import com.openticket.admin.service.EventTicketTypeService;
import com.openticket.admin.service.SmbStorageService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/events")
public class EventApiController {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStatusRepository eventStatusRepository;

    @Autowired
    private EventTicketTypeService eventTicketTypeService;

    @Autowired
    private EventDetailRepository eventDetailRepository;

    @Autowired
    private EventQueryService eventQueryService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SmbStorageService smbStorageService;

    @GetMapping
    public Page<EventListItemDTO> getPagedEvents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "createdAt") String sort) {
        Long companyId = 2L; // 之後從 JWT 拿

        Pageable pageable = PageRequest.of(
                page - 1, // Spring page 從 0 開始
                size,
                Sort.by(Sort.Direction.DESC, sort) // 動態排序
        );

        Page<Event> eventPage;

        if (keyword == null || keyword.isBlank()) {
            eventPage = eventRepository.findByCompanyUserId(companyId, pageable);
        } else {
            eventPage = eventRepository.searchByCompanyUserId(
                    companyId,
                    "%" + keyword + "%",
                    pageable);
        }

        // 轉成 DTO（Page<Entity> -> Page<DTO>）
        return eventPage.map(event -> {
            EventListItemDTO dto = new EventListItemDTO();
            dto.setId(event.getId());
            dto.setTitle(event.getTitle());
            dto.setEventStart(event.getEventStartFormatted());
            dto.setEventEnd(event.getEventEndFormatted());
            dto.setTicketStart(event.getTicketStartFormatted());
            dto.setCreatedAt(event.getCreatedAtIso());
            dto.setStatus(event.getDynamicStatus());
            dto.setViews(0);
            dto.setTicketsSold(0);
            dto.setImages(event.getImages());
            return dto;
        });
    }

    @GetMapping("/latest")
    public List<EventListItemDTO> getLatestEvents() {
        Long companyId = 2L; // 先寫死
        return dashboardService.getLatestEvents(companyId);
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
            event.setStatus(status);

            // 2. 儲存圖片到 SMB
            String filename = UUID.randomUUID() + "_" + coverFile.getOriginalFilename();
            smbStorageService.uploadCover(filename, coverFile.getInputStream());

            EventTitlePage cover = new EventTitlePage();
            cover.setImageUrl("/api/files/covers/" + filename);
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

        // 活動描述
        EventDetail detail = eventDetailRepository.findByEventId(event.getId());
        String description = detail != null ? detail.getContent() : "";
        List<EventTicketRequest> selectedTickets = eventTicketTypeService.findByEventId(event.getId());

        // 組合回傳 JSON
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
            @RequestParam(value = "cover", required = false) MultipartFile coverFile,
            HttpServletRequest request) {

        try {
            Event event = eventService.findById(id);

            // 1. 若活動不可編輯
            String status = event.getDynamicStatus();
            if ("已取消".equals(status) || "已結束".equals(status)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("此活動狀態為「" + status + "」，不可編輯");
            }

            // 2. 更新基本欄位
            event.setTitle(updated.getTitle());
            event.setAddress(updated.getAddress());
            event.setEventStart(updated.getEventStart());
            event.setEventEnd(updated.getEventEnd());
            event.setTicketStart(updated.getTicketStart());

            // 3. 更新 event_detail
            String content = request.getParameter("description");
            eventService.updateDetail(event, content);

            // 4. 更新票種
            String json = request.getParameter("eventTicketsJson");
            if (json != null && !json.isBlank()) {
                ObjectMapper mapper = new ObjectMapper();
                List<EventTicketRequest> list = mapper.readValue(json, new TypeReference<List<EventTicketRequest>>() {
                });
                eventTicketTypeService.rebuildEventTickets(event, list);
            }

            // 5. 若有新封面 → 更新封面（覆蓋到 SMB）
            if (coverFile != null && !coverFile.isEmpty()) {

                // 先記錄舊檔名，等會刪除 SMB 檔案
                List<String> oldFilenames = event.getImages().stream()
                        .map(EventTitlePage::getImageUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .map(url -> url.substring(url.lastIndexOf('/') + 1))
                        .toList();

                // 儲存新圖片到 SMB
                String filename = UUID.randomUUID() + "_" + coverFile.getOriginalFilename();
                smbStorageService.uploadCover(filename, coverFile.getInputStream());

                // 建立新的 EventTitlePage
                EventTitlePage page = new EventTitlePage();
                page.setImageUrl("/api/files/covers/" + filename);
                page.setEvent(event);

                // 刪除舊資料庫紀錄（orphanRemoval = true 會自動刪 DB）
                event.getImages().clear();
                event.getImages().add(page);

                // 刪除 SMB 上的舊檔（忽略刪除錯誤）
                for (String old : oldFilenames) {
                    try {
                        smbStorageService.deleteCover(old);
                    } catch (IOException ignore) {
                        // 刪除失敗就略過，避免阻塞更新流程
                    }
                }
            }

            eventService.save(event);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("title", event.getTitle());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新活動失敗：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelEvent(@PathVariable Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到活動"));

        // 只能「未開放」才能取消
        if (event.getStatus().getId() != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("只有「未開放」的活動可以取消");
        }

        // 取 id=5 的狀態（已取消）
        EventStatus canceled = eventStatusRepository.findById(5L)
                .orElseThrow(() -> new RuntimeException("找不到取消狀態"));

        event.setStatus(canceled);
        eventRepository.save(event);

        return ResponseEntity.ok("活動已取消");
    }

    @GetMapping("/all")
    public List<Map<String, Object>> listEvents() {
        return eventRepository.findAll().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getId());
                    map.put("title", e.getTitle());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/my")
    public List<EventTitleDTO> getMyEventTitles(
            @RequestParam(required = false) String keyword) {

        Long companyId = 2L; // TODO JWT
        return eventQueryService.getEventTitles(companyId, keyword);
    }

}