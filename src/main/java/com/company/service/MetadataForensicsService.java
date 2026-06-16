package com.company.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

@Service
public class MetadataForensicsService {

    public boolean hasEditingSoftwareMetadata(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (directory != null) {
                String software = directory.getString(ExifIFD0Directory.TAG_SOFTWARE);
                if (software != null) {
                    String lower = software.toLowerCase();
                    return lower.contains("photoshop") ||
                           lower.contains("canva")     ||
                           lower.contains("adobe")     ||
                           lower.contains("picsart")   ||
                           lower.contains("gimp");
                }
            }
        } catch (Throwable t) { }
        return false;
    }
}