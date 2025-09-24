package com.ai.hackathon.controller;

import com.ai.hackathon.model.AnalysisResult;
import com.ai.hackathon.service.CodeAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ReviewController {

    @Autowired
    private CodeAnalyzerService analyzerService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResult analyze(@RequestParam("file") MultipartFile file) throws Exception {
        String content = new String(file.getBytes());
        return analyzerService.analyzeOnly(file.getOriginalFilename(), content);
    }
}
