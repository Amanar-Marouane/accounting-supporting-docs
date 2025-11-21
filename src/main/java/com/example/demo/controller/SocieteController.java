package com.example.demo.controller;

import com.example.demo.contract.DocumentServiceContract;
import com.example.demo.dto.DocumentResponseDTO;
import com.example.demo.dto.DocumentUploadDTO;
import com.example.demo.entity.User;
import com.example.demo.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/societe")
@RequiredArgsConstructor
public class SocieteController {

    private final DocumentServiceContract documentService;

    @GetMapping("/info")
    public ResponseEntity<String> getSocieteInfo() {
        return ResponseEntity.ok("Societe Information");
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDTO> uploadDocument(
            @Valid @ModelAttribute DocumentUploadDTO dto,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        User user = customUserDetails.getUser();
        DocumentResponseDTO response = documentService.uploadDocument(dto, file, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/exercice/{exercice}")
    public ResponseEntity<List<DocumentResponseDTO>> getDocumentsByExercice(
            @PathVariable Integer exercice,
            @AuthenticationPrincipal UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        User user = customUserDetails.getUser();
        if (user.getSociete() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<DocumentResponseDTO> documents = documentService.getDocumentsBySocieteAndExercice(
                user.getSociete().getId(), exercice);

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentResponseDTO>> getAllMyDocuments(
            @AuthenticationPrincipal UserDetails userDetails) {

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        User user = customUserDetails.getUser();
        if (user.getSociete() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<DocumentResponseDTO> documents = documentService.getDocumentsBySociete(
                user.getSociete().getId());

        return ResponseEntity.ok(documents);
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
