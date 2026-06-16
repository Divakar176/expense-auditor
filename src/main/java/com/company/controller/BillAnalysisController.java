package com.company.controller;

import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.company.model.AnalysisResult;
import com.company.model.Bill;
import com.company.repository.BillRepository;
import com.company.service.BillAnalysisService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class BillAnalysisController {

    @Autowired
    private BillAnalysisService billAnalysisService;

    @Autowired
    private BillRepository billRepository;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/analyze")
    @ResponseBody
    public AnalysisResult analyzeBill(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new AnalysisResult("SUSPICIOUS", 0, "Please upload an actual file asset.", "MEDIUM");
        }

        // ── 1. CRYPTOGRAPHIC FILE HASH INTERCEPT PRE-CHECK ─────────────────
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
            try (java.io.InputStream is = file.getInputStream()) {
                while ((bytesCount = is.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            String currentFileHash = sb.toString();

            // Check if this exact file signature already exists in the database
            if (billRepository.existsByFileHash(currentFileHash)) {
                AnalysisResult duplicateResult = new AnalysisResult("TAMPERED", 100, 
                    "Duplicate Submission: This exact file image has already been audited.", "HIGH");
                duplicateResult.setVendorName("Duplicate Document");
                duplicateResult.setBillDate("N/A");
                duplicateResult.setAmount(0.0);
                return duplicateResult; // Break early to block duplicate processing
            }
        } catch (Exception e) {
            System.err.println("⚠️ Hash pre-check skipped: " + e.getMessage());
        }

        // ── 2. RUN FORENSIC ANALYSIS ───────────────────────────────────────
        AnalysisResult result = billAnalysisService.analyze(file);

        // ── 3. SEMANTIC DUPLICATE INVOICE NUMBER CHECK ─────────────────────
        String invoiceNo = result.getInvoiceNo() != null
            ? result.getInvoiceNo()
            : "INV-" + System.currentTimeMillis();

        if (billRepository.existsByInvoiceNo(invoiceNo)) {
            result.setStatus("TAMPERED");
            result.setReason("DUPLICATE INVOICE: This invoice number was already submitted before.");
            result.setConfidence(99);
            result.setRiskLevel("HIGH");
            return result; 
        }

        // ── 4. LOOSE FIELD DUPLICATE CHECK (VENDOR + AMOUNT + DATE) ────────
        if (result.getVendorName() != null && result.getAmount() != null
            && result.getBillDate() != null
            && !"Unknown Vendor".equals(result.getVendorName())
            && !"Unknown Date".equals(result.getBillDate())
            && result.getAmount() > 0) {

            List<Bill> duplicates = billRepository.findByVendorNameAndAmountAndBillDate(
                result.getVendorName(),
                result.getAmount(),
                result.getBillDate());

            if (!duplicates.isEmpty()) {
                result.setStatus("TAMPERED");
                result.setReason("DUPLICATE CLAIM: A bill from the same vendor (" +
                    result.getVendorName() + ") for the same amount on the same date already exists.");
                result.setConfidence(97);
                result.setRiskLevel("HIGH");
                return result; 
            }
        }

        // ── 5. SAVE NEW UNIQUE BILL TO DATABASE ────────────────────────────
        try {
            Bill bill = new Bill();
            bill.setVendorName(result.getVendorName() != null ? result.getVendorName() : "Unknown");
            bill.setAmount(result.getAmount()   != null ? result.getAmount()   : 0.0);
            bill.setBillDate(result.getBillDate() != null ? result.getBillDate() : "Unknown");
            bill.setVerdict(result.getVerdict() != null ? result.getVerdict()  : result.getStatus());
            bill.setRiskScore(result.getRiskScore());
            bill.setInvoiceNo(invoiceNo);
            bill.setFileHash(result.getFileHash()); // Stores file hash identity for future upload blockings
            
            billRepository.save(bill);
            System.out.println("✅ Bill saved: " + bill);
        } catch (Exception e) {
            System.err.println("❌ DB save failed: " + e.getMessage());
        }

        return result;
    }

    // ── History page ──────────────────────────────────────────────────────
    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("bills", billRepository.findAllByOrderByIdDesc());
        return "history";
    }

    // ── Dashboard page ────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Bill> bills = billRepository.findAllByOrderByIdDesc();
        long original     = bills.stream().filter(b -> "ORIGINAL".equals(b.getVerdict())).count();
        long tampered     = bills.stream().filter(b -> "TAMPERED".equals(b.getVerdict())).count();
        long suspicious   = bills.stream().filter(b -> "SUSPICIOUS".equals(b.getVerdict())).count();
        long aiGenerated  = bills.stream().filter(b -> "AI_GENERATED".equals(b.getVerdict())).count();
        long notABill     = bills.stream().filter(b -> "NOT_A_BILL".equals(b.getVerdict())).count();

        model.addAttribute("total",        bills.size());
        model.addAttribute("original",     original);
        model.addAttribute("tampered",     tampered);
        model.addAttribute("suspicious",   suspicious);
        model.addAttribute("aiGenerated",  aiGenerated);
        model.addAttribute("notABill",     notABill);
        model.addAttribute("bills",        bills);
        return "dashboard";
    }

    // ── PDF Export ────────────────────────────────────────────────────────
    @GetMapping("/export-pdf")
    public void exportPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=audit-report.pdf");

        List<Bill> bills = billRepository.findAllByOrderByIdDesc();

        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream content = new PDPageContentStream(document, page);

        // Title
        content.setFont(PDType1Font.HELVETICA_BOLD, 18);
        content.beginText();
        content.newLineAtOffset(50, 780);
        content.showText("Forensic Audit Report");
        content.endText();

        // Date
        content.setFont(PDType1Font.HELVETICA, 10);
        content.beginText();
        content.newLineAtOffset(50, 760);
        content.showText("Generated: " + java.time.LocalDate.now().toString());
        content.endText();

        // Summary counts
        long original   = bills.stream().filter(b -> "ORIGINAL".equals(b.getVerdict())).count();
        long tampered   = bills.stream().filter(b -> "TAMPERED".equals(b.getVerdict())).count();
        long suspicious = bills.stream().filter(b -> "SUSPICIOUS".equals(b.getVerdict())).count();

        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
        content.beginText();
        content.newLineAtOffset(50, 730);
        content.showText("Summary");
        content.endText();

        content.setFont(PDType1Font.HELVETICA, 11);
        content.beginText();
        content.newLineAtOffset(50, 712);
        content.showText("Total Audits: " + bills.size() +
            "   Original: " + original +
            "   Tampered: " + tampered +
            "   Suspicious: " + suspicious);
        content.endText();

        // Divider
        content.moveTo(50, 700);
        content.lineTo(545, 700);
        content.stroke();

        // Table header
        content.setFont(PDType1Font.HELVETICA_BOLD, 10);
        content.beginText();
        content.newLineAtOffset(50, 683);
        content.showText("#");
        content.newLineAtOffset(30, 0);
        content.showText("Vendor");
        content.newLineAtOffset(150, 0);
        content.showText("Amount");
        content.newLineAtOffset(80, 0);
        content.showText("Date");
        content.newLineAtOffset(90, 0);
        content.showText("Risk");
        content.newLineAtOffset(50, 0);
        content.showText("Verdict");
        content.endText();

        // Header divider
        content.moveTo(50, 675);
        content.lineTo(545, 675);
        content.stroke();

        // Table rows
        content.setFont(PDType1Font.HELVETICA, 10);
        float y = 658;
        for (Bill bill : bills) {
            if (y < 60) {
                content.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                content = new PDPageContentStream(document, newPage);
                content.setFont(PDType1Font.HELVETICA, 10);
                y = 780;
            }

            content.beginText();
            content.newLineAtOffset(50, y);
            content.showText(String.valueOf(bill.getId()));
            content.newLineAtOffset(30, 0);
            content.showText(bill.getVendorName() != null ?
                bill.getVendorName().substring(0, Math.min(bill.getVendorName().length(), 20)) : "");
            content.newLineAtOffset(150, 0);
            content.showText(bill.getAmount() != null ? String.valueOf(bill.getAmount()) : "0");
            content.newLineAtOffset(80, 0);
            content.showText(bill.getBillDate() != null ? bill.getBillDate() : "");
            content.newLineAtOffset(90, 0);
            content.showText(String.valueOf(bill.getRiskScore()));
            content.newLineAtOffset(50, 0);
            content.showText(bill.getVerdict() != null ? bill.getVerdict() : "");
            content.endText();

            content.moveTo(50, y - 5);
            content.lineTo(545, y - 5);
            content.setLineWidth(0.3f);
            content.stroke();

            y -= 20;
        }

        // Footer
        content.setFont(PDType1Font.HELVETICA, 9);
        content.beginText();
        content.newLineAtOffset(50, 30);
        content.showText("Automated Corporate Expense Auditor — Confidential");
        content.endText();

        content.close();
        document.save(response.getOutputStream());
        document.close();
    }
}