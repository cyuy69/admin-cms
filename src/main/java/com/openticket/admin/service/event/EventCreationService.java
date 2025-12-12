package com.openticket.admin.service.event;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.openticket.admin.repository.EventRepository;
import com.openticket.admin.repository.EventStatusRepository;
import com.openticket.admin.repository.UserRepository;
import com.openticket.admin.security.LoginCompanyProvider;
import com.openticket.admin.service.SmbStorageService;

@Service
@Transactional
public class EventCreationService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStatusRepository eventStatusRepository;

    @Autowired
    private EventDetailRepository eventDetailRepository;

    @Autowired
    private EventTicketTypeService eventTicketTypeService;

    @Autowired
    private SmbStorageService smbStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginCompanyProvider loginCompanyProvider;

    /*
     * 建立活動(Event + 圖片 + 票種 + 描述）
     */
    public Event createEventWithAll(
            Event event,
            MultipartFile coverFile,
            String ticketJson,
            String description) throws IOException {

        // 1. 設定公司
        Long companyId = loginCompanyProvider.getCompanyId();

        User company = userRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("User not found: " + companyId));

        event.setCompanyUser(company);

        // 2. 設定動態狀態
        Long statusId = resolveEventStatus(event);
        EventStatus status = eventStatusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("Status not found: " + statusId));
        event.setStatus(status);

        // 3. 處理封面圖片
        if (coverFile != null && !coverFile.isEmpty()) {
            String filename = UUID.randomUUID() + "_" + coverFile.getOriginalFilename();
            smbStorageService.uploadCover(filename, coverFile.getInputStream());

            EventTitlePage page = new EventTitlePage();
            page.setEvent(event);
            page.setImageUrl("/api/files/covers/" + filename);

            event.getImages().add(page);
        }

        // 4. 儲存活動本體
        Event saved = eventRepository.save(event);

        // 5. 票種 JSON → List<EventTicketRequest>
        if (ticketJson != null && !ticketJson.isBlank()) {
            ObjectMapper mapper = new ObjectMapper();
            List<EventTicketRequest> tickets = mapper.readValue(ticketJson,
                    new TypeReference<List<EventTicketRequest>>() {
                    });

            eventTicketTypeService.createForEvent(saved, tickets);
        }

        // 6. 活動描述
        if (description != null && !description.isBlank()) {
            EventDetail detail = new EventDetail();
            detail.setEvent(saved);
            detail.setContent(description);
            eventDetailRepository.save(detail);
        }

        return saved;
    }

    /**
     * 動態計算活動狀態(createdAt 時)
     */
    private Long resolveEventStatus(Event event) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sale = event.getTicketStart();
        LocalDateTime start = event.getEventStart();
        LocalDateTime end = event.getEventEnd();

        if (sale == null || start == null || end == null)
            return 1L;
        if (now.isBefore(sale))
            return 1L; // 未開放
        if (now.isBefore(start))
            return 4L; // 開放購票
        if (now.isBefore(end))
            return 2L; // 活動進行中
        return 3L; // 已結束
    }
}
