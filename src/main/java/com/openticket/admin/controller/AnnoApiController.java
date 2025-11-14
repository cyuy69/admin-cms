package com.openticket.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openticket.admin.entity.Announcement;
import com.openticket.admin.service.AnnouncementService;

@RestController
@RequestMapping("/api/announcements")
public class AnnoApiController {

    @Autowired
    private AnnouncementService service;

    @GetMapping
    public List<Announcement> getAll() {
        return service.getAll();
    }

    @PostMapping
    public Announcement create(@RequestBody Announcement ann) {
        return service.create(ann);
    }
}
