package com.openticket.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.openticket.admin.service.SmbStorageService;

@RestController
@RequestMapping("/api/files")
public class FileProxyController {

    @Autowired
    private SmbStorageService smbStorageService;

    @GetMapping("/covers/{filename}")
    public ResponseEntity<StreamingResponseBody> getCover(@PathVariable String filename) {
        StreamingResponseBody body = outputStream -> smbStorageService.streamCover(filename, outputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setCacheControl("max-age=3600, public");

        try {
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
