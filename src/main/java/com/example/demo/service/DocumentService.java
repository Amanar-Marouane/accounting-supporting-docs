package com.example.demo.service;

import com.example.demo.contract.DocumentServiceContract;
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
import com.example.demo.util.AppLogger;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService implements DocumentServiceContract {

    private final DocumentRepository documentRepository;
    private final SocieteRepository societeRepository;

    private static final String UPLOAD_DIR = "uploads/documents/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");

    @Override
    @Transactional
    public DocumentResponseDTO uploadDocument(DocumentUploadDTO dto, MultipartFile file, User user) {
        // Validate file
        validateFile(file);

        // Check if numero piece already exists
        documentRepository.findByNumeroPiece(dto.getNumeroPiece()).ifPresent(doc -> {
            throw new BusinessException("DUPLICATE_DOCUMENT",
                    String.format("Un document avec le numéro de pièce '%s' existe déjà", dto.getNumeroPiece()));
        });

        // Get societe
        Societe societe = user.getSociete();
        if (societe == null) {
            throw new BusinessException("NO_SOCIETE",
                    "L'utilisateur n'est associé à aucune société");
        }

        // Save file
        String savedFilePath = saveFile(file, societe.getIce());

        // Create document
        Document document = Document.builder()
                .numeroPiece(dto.getNumeroPiece())
                .typeDocument(dto.getTypeDocument())
                .categorieComptable(dto.getCategorieComptable())
                .datePiece(dto.getDatePiece())
                .montant(dto.getMontant())
                .fournisseur(dto.getFournisseur())
                .cheminFichier(savedFilePath)
                .nomFichierOriginal(file.getOriginalFilename())
                .statut(Document.StatutDocument.EN_ATTENTE)
                .societe(societe)
                .uploadedBy(user)
                .exerciceComptable(dto.getExerciceComptable())
                .build();

        Document saved = documentRepository.save(document);

        return mapToDTO(saved);
    }

    @Override
    public List<DocumentResponseDTO> getDocumentsBySocieteAndExercice(Long societeId, Integer exercice) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new ResourceNotFoundException("Société", societeId.toString()));

        return documentRepository.findBySocieteAndExerciceComptable(societe, exercice)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponseDTO> getAllPendingDocuments() {
        return documentRepository.findByStatut(Document.StatutDocument.EN_ATTENTE)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponseDTO> getPendingDocumentsByExercice(Integer exercice) {
        return documentRepository.findByStatutAndExerciceComptable(Document.StatutDocument.EN_ATTENTE, exercice)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocumentResponseDTO validateDocument(Long documentId, DocumentValidationDTO validation, User comptable) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        if (document.getStatut() != Document.StatutDocument.EN_ATTENTE) {
            throw new BusinessException("ALREADY_PROCESSED",
                    String.format("Le document '%s' a déjà été traité avec le statut: %s",
                            document.getNumeroPiece(), document.getStatut()));
        }

        if (validation.getAction() == DocumentValidationDTO.Action.VALIDER) {
            document.setStatut(Document.StatutDocument.VALIDE);
            document.setCommentaireComptable(validation.getCommentaire());
        } else {
            if (validation.getCommentaire() == null || validation.getCommentaire().trim().isEmpty()) {
                throw new BusinessException("REJECTION_REASON_REQUIRED",
                        "Le motif de rejet est obligatoire pour rejeter un document");
            }
            document.setStatut(Document.StatutDocument.REJETE);
            document.setCommentaireComptable(validation.getCommentaire());
        }

        document.setDateValidation(LocalDateTime.now());
        document.setValidatedBy(comptable);

        Document updated = documentRepository.save(document);
        return mapToDTO(updated);
    }

    @Override
    public DocumentResponseDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id.toString()));
        return mapToDTO(document);
    }

    @Override
    public List<DocumentResponseDTO> getDocumentsBySociete(Long societeId) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new ResourceNotFoundException("Société", societeId.toString()));

        return documentRepository.findBySociete(societe)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] downloadDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        try {
            Path filePath = Paths.get(document.getCheminFichier());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_ERROR",
                    "Erreur lors de la lecture du fichier: " + e.getMessage());
        }
    }

    @Override
    @PreDestroy
    public void deleteAllDocuments() {
        AppLogger.header("Cleaning up documents");
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (Files.exists(uploadPath)) {
                Files.walk(uploadPath)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                AppLogger.info(String.format("Deleted file: %s", path.getFileName()));
                            } catch (IOException e) {
                                AppLogger.error(String.format("Failed to delete: %s", path.getFileName()));
                            }
                        });
            }
            AppLogger.success("All documents cleaned up successfully");
        } catch (IOException e) {
            AppLogger.error("Error during cleanup: " + e.getMessage());
        }
        AppLogger.footer("Cleaning up documents");
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Le fichier est vide");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE",
                    "La taille du fichier ne doit pas dépasser 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("INVALID_FILENAME", "Nom de fichier invalide");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("INVALID_FILE_FORMAT",
                    String.format("Format de fichier non autorisé. Formats acceptés: %s",
                            String.join(", ", ALLOWED_EXTENSIONS.stream()
                                    .map(String::toUpperCase)
                                    .collect(Collectors.toList()))));
        }
    }

    private String saveFile(MultipartFile file, String ice) {
        try {
            // Create directory structure: uploads/documents/{ICE}/
            Path uploadPath = Paths.get(UPLOAD_DIR, ice);
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();
        } catch (IOException e) {
            throw new BusinessException("FILE_SAVE_ERROR",
                    "Erreur lors de l'enregistrement du fichier: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot + 1);
    }

    private DocumentResponseDTO mapToDTO(Document document) {
        return DocumentResponseDTO.builder()
                .id(document.getId())
                .numeroPiece(document.getNumeroPiece())
                .typeDocument(document.getTypeDocument())
                .categorieComptable(document.getCategorieComptable())
                .datePiece(document.getDatePiece())
                .montant(document.getMontant())
                .fournisseur(document.getFournisseur())
                .nomFichierOriginal(document.getNomFichierOriginal())
                .statut(document.getStatut())
                .dateValidation(document.getDateValidation())
                .commentaireComptable(document.getCommentaireComptable())
                .societeRaisonSociale(document.getSociete().getRaisonSociale())
                .uploadedByName(document.getUploadedBy().getFullName())
                .validatedByName(document.getValidatedBy() != null ? document.getValidatedBy().getFullName() : null)
                .exerciceComptable(document.getExerciceComptable())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
