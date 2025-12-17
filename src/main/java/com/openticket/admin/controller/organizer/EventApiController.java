package com.openticket.admin.controller.organizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
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
import com.openticket.admin.repository.EventDetailRepository;
import com.openticket.admin.repository.EventRepository;
import com.openticket.admin.repository.EventStatusRepository;
import com.openticket.admin.security.LoginCompanyProvider;
import com.openticket.admin.service.DashboardService;
import com.openticket.admin.service.SmbStorageService;
import com.openticket.admin.service.event.EventCreationService;
import com.openticket.admin.service.event.EventQueryService;
import com.openticket.admin.service.event.EventService;
import com.openticket.admin.service.event.EventTicketTypeService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/events")
public class EventApiController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields(
                "id",
                "companyUser",
                "status",
                "createdAt",

                "images",
                "images.*.id",
                "images.*.imageUrl",
                "images.*.createdAt",

                // 活動票種
                "eventTicketTypes",
                "eventTicketTypes.*.id",
                "eventTicketTypes.*.eventId",
                "eventTicketTypes.*.createdAt",
                "eventTicketTypes.*.ticketTemplateId",
                "eventTicketTypes.*.earlyBirdConfig",
                "eventTicketTypes.*.earlyBirdConfigId");
    }

    @Autowired
    private EventService eventService;

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
    private EventCreationService eventCreationService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SmbStorageService smbStorageService;

    @Autowired
    private LoginCompanyProvider loginCompanyProvider;

    @GetMapping
    public Page<EventListItemDTO> getPagedEvents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "createdAt") String sort) {
        Long companyId = loginCompanyProvider.getCompanyId();

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
            dto.setStatus(eventService.calculateDynamicStatus(event));
            dto.setViews(0);
            dto.setTicketsSold(0);
            dto.setImages(event.getImages());
            return dto;
        });
    }

    @GetMapping("/latest")
    public List<EventListItemDTO> getLatestEvents() {
        Long companyId = loginCompanyProvider.getCompanyId();
        return dashboardService.getLatestEvents(companyId);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createEvent(
            @ModelAttribute Event event,
            @RequestParam("cover") MultipartFile coverFile,
            HttpServletRequest request) {

        try {
            String ticketJson = request.getParameter("eventTicketsJson");
            String description = request.getParameter("description");

            Event saved = eventCreationService.createEventWithAll(
                    event, coverFile, ticketJson, description);

            return ResponseEntity.ok(saved);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("活動建立失敗：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        Long companyId = loginCompanyProvider.getCompanyId();
        Event event = eventService.findOwnedEvent(id, companyId);

        // 活動描述
        EventDetail detail = eventDetailRepository.findByEventId(event.getId());
        String description = detail != null ? detail.getContent() : "";
        List<EventTicketRequest> selectedTickets = eventTicketTypeService.findByEventId(event.getId());

        // 新增：計算哪些票種已經有訂單，不可刪除
        List<Long> cannotDeleteIds = event.getEventTicketTypes().stream()
                .filter(e -> e.getCheckoutOrders() != null && !e.getCheckoutOrders().isEmpty())
                .map(e -> e.getTicketTemplate().getId())
                .toList();

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

        result.put("cannotDeleteTicketIds", cannotDeleteIds);
        result.put("eventStatus", eventService.calculateDynamicStatus(event));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @ModelAttribute Event updated,
            @RequestParam(value = "cover", required = false) MultipartFile coverFile,
            HttpServletRequest request) {

        try {
            Long companyId = loginCompanyProvider.getCompanyId();
            Event event = eventService.findOwnedEvent(id, companyId);

            // 1. 若活動不可編輯
            String status = eventService.calculateDynamicStatus(event);

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

        Long companyId = loginCompanyProvider.getCompanyId();
        Event event = eventService.findOwnedEvent(id, companyId);

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

    @GetMapping("/my")
    public List<EventTitleDTO> getMyEventTitles(
            @RequestParam(required = false) String keyword) {

        Long companyId = loginCompanyProvider.getCompanyId(); // TODO JWT
        return eventQueryService.getEventTitles(companyId, keyword);
    }
}