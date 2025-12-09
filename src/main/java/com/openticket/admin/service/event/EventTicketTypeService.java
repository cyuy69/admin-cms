package com.openticket.admin.service.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openticket.admin.dto.EventTicketRequest;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventTicketType;
import com.openticket.admin.entity.TicketDiscountConfig;
import com.openticket.admin.entity.TicketType;
import com.openticket.admin.repository.EventTicketTypeRepository;
import com.openticket.admin.repository.TicketDiscountConfigRepository;
import com.openticket.admin.repository.TicketTypeRepository;

@Service
@Transactional
public class EventTicketTypeService {

    @Autowired
    private EventTicketTypeRepository repo;

    @Autowired
    private TicketTypeRepository ticketTypeRepo;

    @Autowired
    private TicketDiscountConfigRepository discountRepo;

    @Autowired
    private EventService eventService;

    public void createForEvent(Event event, List<EventTicketRequest> ticketList) {

        for (EventTicketRequest req : ticketList) {

            TicketType template = ticketTypeRepo.findById(req.getTicketTemplateId())
                    .orElseThrow(() -> new RuntimeException("找不到模板票種 ID：" + req.getTicketTemplateId()));

            EventTicketType ett = new EventTicketType();
            ett.setEvent(event);
            ett.setTicketTemplate(template);

            // 活動票價：活動覆蓋 > 模板 > null
            ett.setCustomPrice(
                    req.getCustomPrice() != null
                            ? req.getCustomPrice()
                            : template.getPrice());

            // 活動限量：活動覆蓋 > 模板 > null
            Integer finalLimit = req.getCustomLimit() != null
                    ? req.getCustomLimit()
                    : template.getLimitQuantity();

            ett.setCustomLimit(finalLimit);

            // isLimited 自動判斷
            ett.setIsLimited(finalLimit != null);

            // -------------------- 早鳥票設定 --------------------
            if (req.getIsEarlyBird() != null && req.getIsEarlyBird()) {

                ett.setIsEarlyBird(true);

                // 優惠折扣轉成 0.xx
                BigDecimal discountRate = req.getDiscountRate()
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                // 建立早鳥設定
                TicketDiscountConfig config = new TicketDiscountConfig();
                config.setDiscountPrice(discountRate);
                config.setDurationDays(req.getEarlyBirdDays());

                discountRepo.save(config);
                ett.setEarlyBirdConfig(config);
            } else {
                ett.setIsEarlyBird(false);
                ett.setEarlyBirdConfig(null);
            }
            repo.save(ett);
        }
    }

    @Transactional
    public void rebuildEventTickets(Event event, List<EventTicketRequest> list) {

        // 取得活動狀態
        String status = eventService.calculateDynamicStatus(event);

        boolean isNotOpened = "未開放".equals(status);
        boolean isOnSale = "開放購票".equals(status);
        boolean isRunning = "活動進行中".equals(status);
        boolean isEnded = "已結束".equals(status) || "已取消".equals(status);

        // 若活動正在進行 / 已結束 / 已取消 → 禁止任何修改
        if (isRunning || isEnded) {
            throw new RuntimeException("活動狀態為「" + status + "」，不可修改票種");
        }

        // 取出現有票種
        List<EventTicketType> existing = repo.findByEventId(event.getId());

        // 使用 templateId 當 key
        Map<Long, EventTicketType> existingMap = existing.stream().collect(Collectors.toMap(
                e -> e.getTicketTemplate().getId(),
                e -> e));

        // 收集前端傳來所有 templateId
        Set<Long> incomingIds = list.stream()
                .map(EventTicketRequest::getTicketTemplateId)
                .collect(Collectors.toSet());

        // 處理每一個前端傳來的票種：新增 or 更新
        for (EventTicketRequest req : list) {

            TicketType template = ticketTypeRepo.findById(req.getTicketTemplateId())
                    .orElseThrow(() -> new RuntimeException("找不到模板票種 ID：" + req.getTicketTemplateId()));

            EventTicketType old = existingMap.get(req.getTicketTemplateId());

            // 票種存在的更新邏輯

            if (old != null) {

                // （禁止改價）若已開賣
                if (isOnSale) {
                    if (req.getCustomPrice() != null &&
                            old.getCustomPrice().compareTo(req.getCustomPrice()) != 0) {
                        throw new RuntimeException("活動已開賣，票價不可變動：" + template.getName());
                    }
                } else {
                    // 未開放 → 可改價
                    old.setCustomPrice(
                            req.getCustomPrice() != null ? req.getCustomPrice() : template.getPrice());
                }

                // 限量永遠可變
                Integer limit = req.getCustomLimit() != null
                        ? req.getCustomLimit()
                        : template.getLimitQuantity();

                old.setCustomLimit(limit);
                old.setIsLimited(limit != null);

                // 早鳥票只能在未開放時改（已開賣就不能改）
                if (isNotOpened) {
                    if (Boolean.TRUE.equals(req.getIsEarlyBird())) {

                        BigDecimal discountRate = req.getDiscountRate()
                                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                        TicketDiscountConfig config = new TicketDiscountConfig();
                        config.setDiscountPrice(discountRate);
                        config.setDurationDays(req.getEarlyBirdDays());
                        discountRepo.save(config);

                        old.setEarlyBirdConfig(config);
                        old.setIsEarlyBird(true);

                    } else {
                        old.setIsEarlyBird(false);
                        old.setEarlyBirdConfig(null);
                    }
                }
                continue;
            }

            // 新增票種

            EventTicketType ett = new EventTicketType();
            ett.setEvent(event);
            ett.setTicketTemplate(template);

            // 已開賣 → 必須使用模板原價
            if (isOnSale) {
                ett.setCustomPrice(template.getPrice());
            } else {
                ett.setCustomPrice(
                        req.getCustomPrice() != null ? req.getCustomPrice() : template.getPrice());
            }

            Integer limit = req.getCustomLimit() != null
                    ? req.getCustomLimit()
                    : template.getLimitQuantity();

            ett.setCustomLimit(limit);
            ett.setIsLimited(limit != null);

            // 新增早鳥（僅未開放可設定）
            if (isNotOpened && Boolean.TRUE.equals(req.getIsEarlyBird())) {
                BigDecimal discountRate = req.getDiscountRate()
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                TicketDiscountConfig config = new TicketDiscountConfig();
                config.setDiscountPrice(discountRate);
                config.setDurationDays(req.getEarlyBirdDays());
                discountRepo.save(config);

                ett.setEarlyBirdConfig(config);
                ett.setIsEarlyBird(true);
            } else {
                ett.setIsEarlyBird(false);
                ett.setEarlyBirdConfig(null);
            }

            repo.save(ett);
        }

        // 處理刪除:只有「未開放」才能刪掉票種
        if (isNotOpened) {
            for (EventTicketType e : existing) {
                Long templateId = e.getTicketTemplate().getId();

                if (!incomingIds.contains(templateId)) {

                    // 使用 repo 查 reservation_items 是否有訂單
                    boolean hasOrders = repo.hasOrders(e.getId());

                    if (hasOrders) {
                        throw new RuntimeException(
                                "票種「" + e.getTicketTemplate().getName() + "」已有訂單，不能刪除");
                    }

                    repo.delete(e);
                }
            }
        }

        // 若開放購票後，則完全禁止刪除
        if (isOnSale) {
            for (EventTicketType e : existing) {
                Long templateId = e.getTicketTemplate().getId();

                if (!incomingIds.contains(templateId)) {
                    throw new RuntimeException(
                            "活動已開放購票，不能刪除票種：「" + e.getTicketTemplate().getName() + "」");
                }
            }
        }

    }

    public List<EventTicketRequest> findByEventId(Long eventId) {
        List<EventTicketType> entities = repo.findByEventId(eventId);

        return entities.stream().map(e -> {
            EventTicketRequest dto = new EventTicketRequest();
            dto.setTicketTemplateId(e.getTicketTemplate().getId());
            dto.setCustomPrice(e.getCustomPrice());
            dto.setCustomLimit(e.getCustomLimit());
            dto.setDescription(e.getTicketTemplate().getDescription());

            // 早鳥啟用
            dto.setIsEarlyBird(e.getIsEarlyBird());

            // 若有早鳥設定
            if (e.getEarlyBirdConfig() != null) {
                dto.setEarlyBirdDays(e.getEarlyBirdConfig().getDurationDays());
                // discountRate 是前端百分比 → 後端 BigDecimal 小數要乘 100
                dto.setDiscountRate(
                        e.getEarlyBirdConfig().getDiscountPrice().multiply(new BigDecimal("100")));
            } else {
                dto.setEarlyBirdDays(null);
                dto.setDiscountRate(null);
            }
            return dto;
        }).toList();
    }

}
