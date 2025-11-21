package com.example.demo.service;

import com.example.demo.dto.DocumentResponseDTO;
import com.example.demo.dto.DocumentUploadDTO;
import com.example.demo.dto.DocumentValidationDTO;
import com.example.demo.entity.Document;
import com.example.demo.entity.Societe;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.DocumentRepository;
import com.example.demo.repository.SocieteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SocieteRepository societeRepository;

    @InjectMocks
    private DocumentService documentService;

    @TempDir
    Path tempDir;

    private User societeUser;
    private User comptableUser;
    private Societe societe;
    private Document document;
    private DocumentUploadDTO uploadDTO;
    private MultipartFile validFile;

    @BeforeEach
    void setUp() {
        // Setup Societe
        societe = Societe.builder()
                .id(1L)
                .raisonSociale("Test SARL")
                .ice("001234567890001")
                .adresse("123 Test Street")
                .telephone("0522-123456")
                .emailContact("test@test.ma")
                .build();

        // Setup Users
        societeUser = User.builder()
                .id(1L)
                .email("societe@test.ma")
                .password("password")
                .fullName("Societe User")
                .role(User.Role.SOCIETE)
                .societe(societe)
                .build();

        comptableUser = User.builder()
                .id(2L)
                .email("comptable@test.ma")
                .password("password")
                .fullName("Comptable User")
                .role(User.Role.COMPTABLE)
                .build();

        // Setup Document
        document = Document.builder()
                .id(1L)
                .numeroPiece("FAC-2024-001")
                .typeDocument(Document.TypeDocument.FACTURE_ACHAT)
                .categorieComptable("Achats")
                .datePiece(LocalDate.now())
                .montant(new BigDecimal("1000.00"))
                .fournisseur("Fournisseur Test")
                .cheminFichier("uploads/documents/001234567890001/test.pdf")
                .nomFichierOriginal("facture.pdf")
                .statut(Document.StatutDocument.EN_ATTENTE)
                .societe(societe)
                .uploadedBy(societeUser)
                .exerciceComptable(2024)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup Upload DTO
        uploadDTO = DocumentUploadDTO.builder()
                .numeroPiece("FAC-2024-001")
                .typeDocument(Document.TypeDocument.FACTURE_ACHAT)
                .categorieComptable("Achats")
                .datePiece(LocalDate.now())
                .montant(new BigDecimal("1000.00"))
                .fournisseur("Fournisseur Test")
                .exerciceComptable(2024)
                .build();

        // Setup valid file
        validFile = new MockMultipartFile(
                "file",
                "facture.pdf",
                "application/pdf",
                "test content".getBytes());
    }

    @Test
    void uploadDocument_WithValidData_ShouldSucceed() {
        // Given
        when(documentRepository.findByNumeroPiece(uploadDTO.getNumeroPiece())).thenReturn(Optional.empty());
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        // When
        DocumentResponseDTO result = documentService.uploadDocument(uploadDTO, validFile, societeUser);

        // Then
        assertNotNull(result);
        assertEquals("FAC-2024-001", result.getNumeroPiece());
        assertEquals("Test SARL", result.getSocieteRaisonSociale());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void uploadDocument_WithDuplicateNumeroPiece_ShouldThrowException() {
        // Given
        when(documentRepository.findByNumeroPiece(uploadDTO.getNumeroPiece())).thenReturn(Optional.of(document));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(uploadDTO, validFile, societeUser));

        assertEquals("DUPLICATE_DOCUMENT", exception.getCode());
        assertTrue(exception.getMessage().contains("FAC-2024-001"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_WithUserWithoutSociete_ShouldThrowException() {
        // Given
        User userWithoutSociete = User.builder()
                .id(3L)
                .email("test@test.ma")
                .fullName("Test User")
                .role(User.Role.COMPTABLE)
                .societe(null)
                .build();

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(uploadDTO, validFile, userWithoutSociete));

        assertEquals("NO_SOCIETE", exception.getCode());
    }

    @Test
    void uploadDocument_WithEmptyFile_ShouldThrowException() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(uploadDTO, emptyFile, societeUser));

        assertEquals("EMPTY_FILE", exception.getCode());
    }

    @Test
    void uploadDocument_WithInvalidFileFormat_ShouldThrowException() {
        // Given
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.exe",
                "application/exe",
                "test content".getBytes());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(uploadDTO, invalidFile, societeUser));

        assertEquals("INVALID_FILE_FORMAT", exception.getCode());
    }

    @Test
    void uploadDocument_WithFileTooLarge_ShouldThrowException() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeContent);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.uploadDocument(uploadDTO, largeFile, societeUser));

        assertEquals("FILE_TOO_LARGE", exception.getCode());
    }

    @Test
    void getDocumentsBySocieteAndExercice_WithValidData_ShouldReturnDocuments() {
        // Given
        List<Document> documents = Arrays.asList(document);
        when(societeRepository.findById(1L)).thenReturn(Optional.of(societe));
        when(documentRepository.findBySocieteAndExerciceComptable(societe, 2024)).thenReturn(documents);

        // When
        List<DocumentResponseDTO> result = documentService.getDocumentsBySocieteAndExercice(1L, 2024);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("FAC-2024-001", result.get(0).getNumeroPiece());
    }

    @Test
    void getDocumentsBySocieteAndExercice_WithInvalidSocieteId_ShouldThrowException() {
        // Given
        when(societeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> documentService.getDocumentsBySocieteAndExercice(999L, 2024));
    }

    @Test
    void getAllPendingDocuments_ShouldReturnPendingDocuments() {
        // Given
        List<Document> pendingDocuments = Arrays.asList(document);
        when(documentRepository.findByStatut(Document.StatutDocument.EN_ATTENTE)).thenReturn(pendingDocuments);

        // When
        List<DocumentResponseDTO> result = documentService.getAllPendingDocuments();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Document.StatutDocument.EN_ATTENTE, document.getStatut());
    }

    @Test
    void getPendingDocumentsByExercice_ShouldReturnFilteredDocuments() {
        // Given
        List<Document> documents = Arrays.asList(document);
        when(documentRepository.findByStatutAndExerciceComptable(Document.StatutDocument.EN_ATTENTE, 2024))
                .thenReturn(documents);

        // When
        List<DocumentResponseDTO> result = documentService.getPendingDocumentsByExercice(2024);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2024, result.get(0).getExerciceComptable());
    }

    @Test
    void validateDocument_WithValidAction_ShouldUpdateDocument() {
        // Given
        DocumentValidationDTO validation = DocumentValidationDTO.builder()
                .action(DocumentValidationDTO.Action.VALIDER)
                .commentaire("Document validé")
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        // When
        DocumentResponseDTO result = documentService.validateDocument(1L, validation, comptableUser);

        // Then
        assertNotNull(result);
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void validateDocument_WithRejectWithoutComment_ShouldThrowException() {
        // Given
        DocumentValidationDTO validation = DocumentValidationDTO.builder()
                .action(DocumentValidationDTO.Action.REJETER)
                .commentaire(null)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.validateDocument(1L, validation, comptableUser));

        assertEquals("REJECTION_REASON_REQUIRED", exception.getCode());
    }

    @Test
    void validateDocument_WithAlreadyProcessedDocument_ShouldThrowException() {
        // Given
        document.setStatut(Document.StatutDocument.VALIDE);
        DocumentValidationDTO validation = DocumentValidationDTO.builder()
                .action(DocumentValidationDTO.Action.VALIDER)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.validateDocument(1L, validation, comptableUser));

        assertEquals("ALREADY_PROCESSED", exception.getCode());
    }

    @Test
    void validateDocument_WithNonExistentDocument_ShouldThrowException() {
        // Given
        DocumentValidationDTO validation = DocumentValidationDTO.builder()
                .action(DocumentValidationDTO.Action.VALIDER)
                .build();

        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> documentService.validateDocument(999L, validation, comptableUser));
    }

    @Test
    void getDocumentById_WithValidId_ShouldReturnDocument() {
        // Given
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        // When
        DocumentResponseDTO result = documentService.getDocumentById(1L);

        // Then
        assertNotNull(result);
        assertEquals("FAC-2024-001", result.getNumeroPiece());
    }

    @Test
    void getDocumentById_WithInvalidId_ShouldThrowException() {
        // Given
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocumentById(999L));
    }

    @Test
    void getDocumentsBySociete_WithValidSocieteId_ShouldReturnDocuments() {
        // Given
        List<Document> documents = Arrays.asList(document);
        when(societeRepository.findById(1L)).thenReturn(Optional.of(societe));
        when(documentRepository.findBySociete(societe)).thenReturn(documents);

        // When
        List<DocumentResponseDTO> result = documentService.getDocumentsBySociete(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getDocumentsBySociete_WithInvalidSocieteId_ShouldThrowException() {
        // Given
        when(societeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocumentsBySociete(999L));
    }

    @Test
    void validateDocument_WithRejectAction_ShouldSetStatusToRejected() {
        // Given
        DocumentValidationDTO validation = DocumentValidationDTO.builder()
                .action(DocumentValidationDTO.Action.REJETER)
                .commentaire("Document incomplet")
                .build();

        Document savedDocument = Document.builder()
                .id(1L)
                .numeroPiece("FAC-2024-001")
                .statut(Document.StatutDocument.REJETE)
                .commentaireComptable("Document incomplet")
                .dateValidation(LocalDateTime.now())
                .validatedBy(comptableUser)
                .societe(societe)
                .uploadedBy(societeUser)
                .typeDocument(Document.TypeDocument.FACTURE_ACHAT)
                .categorieComptable("Achats")
                .datePiece(LocalDate.now())
                .montant(new BigDecimal("1000.00"))
                .fournisseur("Test")
                .cheminFichier("test.pdf")
                .nomFichierOriginal("test.pdf")
                .exerciceComptable(2024)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        // When
        DocumentResponseDTO result = documentService.validateDocument(1L, validation, comptableUser);

        // Then
        assertNotNull(result);
        verify(documentRepository).save(argThat(doc -> doc.getStatut() == Document.StatutDocument.REJETE &&
                "Document incomplet".equals(doc.getCommentaireComptable())));
    }
}
