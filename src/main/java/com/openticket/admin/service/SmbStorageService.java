package com.openticket.admin.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import com.hierynomus.smbj.auth.AuthenticationContext;

@Service
public class SmbStorageService {

    @Value("${smb.host}")
    private String host;

    @Value("${smb.share}")
    private String shareName;

    @Value("${smb.username}")
    private String username;

    @Value("${smb.password}")
    private String password;

    @Value("${smb.domain:}")
    private String domain;

    @Value("${smb.base-path}")
    private String basePath;

    /**
     * Upload cover image to SMB share.
     */
    public void uploadCover(String filename, InputStream data) throws IOException {
        try (SMBClient client = new SMBClient();
                Connection connection = client.connect(host);
                Session session = connection.authenticate(new AuthenticationContext(username, password.toCharArray(), domain));
                DiskShare share = (DiskShare) session.connectShare(shareName)) {

            String dirPath = normalize(basePath);
            if (!dirPath.isEmpty()) {
                ensureDirectories(share, dirPath);
            }

            String filePath = dirPath.isEmpty() ? filename : dirPath + "\\" + filename;

            try (File file = share.openFile(filePath,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null);
                    OutputStream out = file.getOutputStream()) {
                data.transferTo(out);
            }
        }
    }

    /**
     * Stream cover image from SMB share to response OutputStream.
     */
    public void streamCover(String filename, OutputStream responseOut) throws IOException {
        try (SMBClient client = new SMBClient();
                Connection connection = client.connect(host);
                Session session = connection.authenticate(new AuthenticationContext(username, password.toCharArray(), domain));
                DiskShare share = (DiskShare) session.connectShare(shareName)) {

            String dirPath = normalize(basePath);
            String filePath = dirPath.isEmpty() ? filename : dirPath + "\\" + filename;

            if (!share.fileExists(filePath)) {
                throw new IOException("File not found: " + filePath);
            }

            try (File file = share.openFile(filePath,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null);
                    InputStream in = file.getInputStream()) {
                in.transferTo(responseOut);
            }
        }
    }

    /**
     * 若檔案存在於 SMB，則刪除封面檔。
     */
    public void deleteCover(String filename) throws IOException {
        try (SMBClient client = new SMBClient();
                Connection connection = client.connect(host);
                Session session = connection.authenticate(new AuthenticationContext(username, password.toCharArray(), domain));
                DiskShare share = (DiskShare) session.connectShare(shareName)) {

            String dirPath = normalize(basePath);
            String filePath = dirPath.isEmpty() ? filename : dirPath + "\\" + filename;

            if (share.fileExists(filePath)) {
                share.rm(filePath);
            }
        }
    }

    private void ensureDirectories(DiskShare share, String path) {
        String[] parts = path.split("/");
        String current = "";
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            current = current.isEmpty() ? part : current + "\\" + part;
            if (!share.folderExists(current)) {
                share.mkdir(current);
            }
        }
    }

    private String normalize(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.replace("\\", "/");
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
