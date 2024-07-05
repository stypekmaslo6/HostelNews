package com.shh.shhbook.service;

import lombok.Getter;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Getter
public class FtpService {
    @Value("${ftp.server}")
    private String server;

    @Value("${ftp.port}")
    private int port;

    @Value("${ftp.user}")
    private String user;

    @Value("${ftp.password}")
    private String password;

    public List<String> uploadFilesToFTP(List<MultipartFile> files, Long postId) {
        List<String> filePaths = new ArrayList<>();
        FTPClient client = new FTPClient();
        try {
            client.connect(server, port);
            client.login(user, password);
            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.makeDirectory("uploads/" + postId);
            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                InputStream inputStream = file.getInputStream();
                System.out.println(postId);
                boolean done = client.storeFile("uploads/" + postId  + "/" + fileName, inputStream);
                if (done) {
                    filePaths.add("uploads/" + postId  + "/" + fileName);
                }
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.logout();
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filePaths;
    }
    public File downloadFileFromFTP(String remoteFilePath) {
        FTPClient client = new FTPClient();
        File localFile = null;
        try {
            client.connect(server, port);
            client.login(user, password);
            client.setFileType(FTP.BINARY_FILE_TYPE);

            String[] parts = remoteFilePath.split("/");
            String fileName = parts[parts.length - 1];
            localFile = File.createTempFile(fileName, null);

            OutputStream outputStream = new FileOutputStream(localFile);
            boolean success = client.retrieveFile(remoteFilePath, outputStream);
            outputStream.close();

            if (!success) {
                localFile.delete();
                localFile = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.logout();
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return localFile;
    }
}
