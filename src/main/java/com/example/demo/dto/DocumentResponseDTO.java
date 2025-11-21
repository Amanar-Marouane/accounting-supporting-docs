package com.example.demo.dto;

import com.example.demo.entity.Document;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDTO {

    private Long id;
    private String numeroPiece;
    private Document.TypeDocument typeDocument;
    private String categorieComptable;
    private LocalDate datePiece;
    private BigDecimal montant;
    private String fournisseur;
    private String nomFichierOriginal;
    private Document.StatutDocument statut;
    private LocalDateTime dateValidation;
    private String commentaireComptable;
    private String societeRaisonSociale;
    private String uploadedByName;
    private String validatedByName;
    private Integer exerciceComptable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
