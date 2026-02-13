package com.example.giftbook.controller;

import com.example.giftbook.entity.Activity;
import com.example.giftbook.entity.GiftRecord;
import com.example.giftbook.repository.ActivityRepository;
import com.example.giftbook.repository.GiftRecordRepository;
import com.example.giftbook.util.RateLimitStore;
import com.example.giftbook.util.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ApiController {
    private final ActivityRepository activityRepository;
    private final GiftRecordRepository giftRecordRepository;
    private final TokenStore tokenStore;
    private final RateLimitStore rateLimitStore;

    public ApiController(ActivityRepository activityRepository,
                         GiftRecordRepository giftRecordRepository,
                         TokenStore tokenStore,
                         RateLimitStore rateLimitStore) {
        this.activityRepository = activityRepository;
        this.giftRecordRepository = giftRecordRepository;
        this.tokenStore = tokenStore;
        this.rateLimitStore = rateLimitStore;
    }

    @GetMapping("/activity")
    public Activity getActivity() {
        return activityRepository.findById(1L).orElseGet(() -> {
            Activity activity = new Activity();
            activity.setId(1L);
            activity.setTitle("某某婚礼/乔迁");
            activity.setDate("2026-01-01");
            activity.setLocation("某某村文化礼堂");
            return activityRepository.save(activity);
        });
    }

    @PostMapping("/records")
    public ResponseEntity<?> submitRecords(@RequestBody SubmitRequest req, HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!rateLimitStore.allow(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "提交过于频繁，请稍后再试"));
        }
        if (!StringUtils.hasText(req.getPayerName()) || req.getItems() == null || req.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "参数不完整"));
        }
        String submitId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        BigDecimal total = BigDecimal.ZERO;
        for (SubmitItem item : req.getItems()) {
            if (!StringUtils.hasText(item.getName()) || item.getAmount() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "姓名和金额必填"));
            }
            GiftRecord record = new GiftRecord();
            record.setSubmitId(submitId);
            record.setName(item.getName());
            record.setAmount(item.getAmount());
            record.setRelation(item.getRelation());
            record.setBlessing(item.getBlessing());
            record.setPayerName(req.getPayerName());
            record.setPayerPhone(req.getPayerPhone());
            record.setSubmitTime(now);
            record.setIp(ip);
            giftRecordRepository.save(record);
            total = total.add(item.getAmount());
        }
        return ResponseEntity.ok(Map.of("message", "success", "total", total));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if ("admin".equals(req.getUsername()) && "admin".equals(req.getPassword())) {
            String token = UUID.randomUUID().toString();
            tokenStore.add(token);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户名或密码错误"));
    }

    @GetMapping("/records")
    public ResponseEntity<?> allRecords(@RequestHeader(value = "Authorization", required = false) String auth,
                                        @RequestParam(required = false, defaultValue = "") String keyword) {
        if (!checkToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        List<GiftRecord> records = StringUtils.hasText(keyword)
                ? giftRecordRepository.findByNameContainingOrPayerNameContainingOrderBySubmitTimeDesc(keyword, keyword)
                : giftRecordRepository.findAllByOrderBySubmitTimeDesc();
        BigDecimal sum = records.stream().map(GiftRecord::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ResponseEntity.ok(Map.of("records", records, "sum", sum));
    }

    @PostMapping(value = "/activity/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateActivity(@RequestHeader(value = "Authorization", required = false) String auth,
                                            @RequestParam String title,
                                            @RequestParam String date,
                                            @RequestParam String location,
                                            @RequestParam(value = "wxFile", required = false) MultipartFile wxFile,
                                            @RequestParam(value = "aliFile", required = false) MultipartFile aliFile) throws IOException {
        if (!checkToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        Activity activity = activityRepository.findById(1L).orElse(new Activity());
        activity.setId(1L);
        activity.setTitle(title);
        activity.setDate(date);
        activity.setLocation(location);

        if (wxFile != null && !wxFile.isEmpty()) {
            activity.setWxQrcodePath(saveUpload(wxFile));
        }
        if (aliFile != null && !aliFile.isEmpty()) {
            activity.setAliQrcodePath(saveUpload(aliFile));
        }
        activityRepository.save(activity);
        return ResponseEntity.ok(activity);
    }

    @DeleteMapping("/records/clear")
    public ResponseEntity<?> clear(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (!checkToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        giftRecordRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @DeleteMapping("/records/{id}")
    public ResponseEntity<?> deleteOne(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @PathVariable Long id) {
        if (!checkToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        giftRecordRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestHeader(value = "Authorization", required = false) String auth) throws IOException {
        if (!checkToken(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        List<GiftRecord> list = giftRecordRepository.findAllByOrderBySubmitTimeDesc();
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("礼簿记录");
        Row head = sheet.createRow(0);
        String[] headers = {"提交时间", "代付人", "手机号", "姓名", "金额", "关系", "祝福语", "IP"};
        for (int i = 0; i < headers.length; i++) {
            head.createCell(i).setCellValue(headers[i]);
        }
        int rowNum = 1;
        for (GiftRecord r : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getSubmitTime() == null ? "" : r.getSubmitTime().toString());
            row.createCell(1).setCellValue(nullToEmpty(r.getPayerName()));
            row.createCell(2).setCellValue(nullToEmpty(r.getPayerPhone()));
            row.createCell(3).setCellValue(nullToEmpty(r.getName()));
            row.createCell(4).setCellValue(r.getAmount() == null ? 0D : r.getAmount().doubleValue());
            row.createCell(5).setCellValue(nullToEmpty(r.getRelation()));
            row.createCell(6).setCellValue(nullToEmpty(r.getBlessing()));
            row.createCell(7).setCellValue(nullToEmpty(r.getIp()));
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

        byte[] bytes;
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            workbook.write(bos);
            bytes = bos.toByteArray();
        }
        workbook.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gift_records.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    private String saveUpload(MultipartFile file) throws IOException {
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".")))
                .orElse(".jpg");
        String fileName = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()) + ext;
        Path uploadDir = Paths.get("src/main/resources/static/uploads");
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(fileName);
        file.transferTo(target);
        return "/uploads/" + fileName;
    }

    private boolean checkToken(String auth) {
        if (auth == null) return false;
        String token = auth.replace("Bearer ", "");
        return tokenStore.valid(token);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return StringUtils.hasText(xff) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class SubmitRequest {
        private String payerName;
        private String payerPhone;
        private List<SubmitItem> items;

        public String getPayerName() { return payerName; }
        public void setPayerName(String payerName) { this.payerName = payerName; }
        public String getPayerPhone() { return payerPhone; }
        public void setPayerPhone(String payerPhone) { this.payerPhone = payerPhone; }
        public List<SubmitItem> getItems() { return items; }
        public void setItems(List<SubmitItem> items) { this.items = items; }
    }

    public static class SubmitItem {
        private String name;
        private BigDecimal amount;
        private String relation;
        private String blessing;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getRelation() { return relation; }
        public void setRelation(String relation) { this.relation = relation; }
        public String getBlessing() { return blessing; }
        public void setBlessing(String blessing) { this.blessing = blessing; }
    }
}
