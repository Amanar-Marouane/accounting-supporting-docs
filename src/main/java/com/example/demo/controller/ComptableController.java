package com.example.demo.controller;

import com.example.demo.contract.DocumentServiceContract;
import com.example.demo.dto.DocumentResponseDTO;
import com.example.demo.dto.DocumentValidationDTO;
import com.example.demo.entity.User;
import com.example.demo.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comptable")
@RequiredArgsConstructor
public class ComptableController {

    private final DocumentServiceContract documentService;

    @GetMapping("/info")
    public ResponseEntity<String> getComptableInfo() {
        return ResponseEntity.ok("Comptable Information");
    }

    @GetMapping("/documents/pending")
    public ResponseEntity<List<DocumentResponseDTO>> getAllPendingDocuments() {
        List<DocumentResponseDTO> documents = documentService.getAllPendingDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/pending/exercice/{exercice}")
    public ResponseEntity<List<DocumentResponseDTO>> getPendingDocumentsByExercice(
            @PathVariable Integer exercice) {

        List<DocumentResponseDTO> documents = documentService.getPendingDocumentsByExercice(exercice);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/societe/{societeId}")
    public ResponseEntity<List<DocumentResponseDTO>> getDocumentsBySociete(
            @PathVariable Long societeId) {

        List<DocumentResponseDTO> documents = documentService.getDocumentsBySociete(societeId);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/documents/{id}/validate")
    public ResponseEntity<DocumentResponseDTO> validateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentValidationDTO validation,
            @AuthenticationPrincipal UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        User comptable = customUserDetails.getUser();

        DocumentResponseDTO response = documentService.validateDocument(id, validation, comptable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable Long id) {
        DocumentResponseDTO document = documentService.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        byte[] fileContent = documentService.downloadDocument(id);
        DocumentResponseDTO document = documentService.getDocumentById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", document.getNomFichierOriginal());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }
}
