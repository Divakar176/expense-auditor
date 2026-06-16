package com.company.service;

import java.io.File;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrService {

    @Value("${tesseract.path:C:\\Program Files\\Tesseract-OCR\\tesseract.exe}")
    private String tesseractPath;

    public String extractTextFromFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        if ((contentType != null && contentType.equalsIgnoreCase("application/pdf")) ||
            (fileName != null && fileName.toLowerCase().endsWith(".pdf"))) {
            return convertPdfToText(file);
        } else {
            return performOCR(file);
        }
    }

    private String convertPdfToText(MultipartFile file) throws Exception {
        File workspace = new File("ocr_workspace");
        if (!workspace.exists()) workspace.mkdirs();

        File inputFile = new File(workspace, "pdf_converted_page.png");

        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            if (document.getNumberOfPages() == 0)
                throw new IllegalArgumentException("The uploaded PDF has no pages.");

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            java.awt.image.BufferedImage rawImage = pdfRenderer.renderImageWithDPI(0, 300);

            java.awt.image.BufferedImage cleanImage = new java.awt.image.BufferedImage(
                rawImage.getWidth(), rawImage.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_RGB);

            java.awt.Graphics2D g = cleanImage.createGraphics();
            g.drawImage(rawImage, 0, 0, null);
            g.dispose();

            javax.imageio.ImageIO.write(cleanImage, "png", inputFile);
        }

        return executeTesseract(inputFile);
    }

    private String performOCR(MultipartFile file) throws Exception {
        File workspace = new File("ocr_workspace");
        if (!workspace.exists()) workspace.mkdirs();

        File inputFile = new File(workspace, "input_bill.png");

        try (InputStream is = file.getInputStream()) {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(is);
            if (original == null)
                throw new IllegalArgumentException("Invalid image file.");

            java.awt.image.BufferedImage standard = new java.awt.image.BufferedImage(
                original.getWidth(), original.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_RGB);

            java.awt.Graphics2D g = standard.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.dispose();

            javax.imageio.ImageIO.write(standard, "png", inputFile);
        }

        return executeTesseract(inputFile);
    }

    private String executeTesseract(File imageFile) throws Exception {
        File workspace = new File("ocr_workspace");
        File outputBase = new File(workspace, "output_result");
        File outputTxt = new File(workspace, "output_result.txt");

        if (outputTxt.exists()) outputTxt.delete();

        String[] command = {
            tesseractPath,
            imageFile.getAbsolutePath(),
            outputBase.getAbsolutePath(),
            "-l", "eng"
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(tesseractPath).getParentFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            if (outputTxt.exists()) {
                return java.nio.file.Files.readString(outputTxt.toPath());
            } else {
                throw new java.io.FileNotFoundException("Tesseract produced no output.");
            }
        } finally {
            if (imageFile.exists()) imageFile.delete();
            if (outputTxt.exists()) outputTxt.delete();
        }
    }
}