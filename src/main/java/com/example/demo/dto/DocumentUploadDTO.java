package com.example.demo.dto;

import com.example.demo.entity.Document;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadDTO {

    @NotBlank(message = "Le numéro de pièce est obligatoire")
    private String numeroPiece;

    @NotNull(message = "Le type de document est obligatoire")
    private Document.TypeDocument typeDocument;

    @NotBlank(message = "La catégorie comptable est obligatoire")
    private String categorieComptable;

    @NotNull(message = "La date de la pièce est obligatoire")
    @PastOrPresent(message = "La date ne peut pas être dans le futur")
    private LocalDate datePiece;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @NotBlank(message = "Le fournisseur est obligatoire")
    private String fournisseur;

    @NotNull(message = "L'exercice comptable est obligatoire")
    @Min(value = 2000, message = "L'exercice comptable doit être >= 2000")
    @Max(value = 2100, message = "L'exercice comptable doit être <= 2100")
    private Integer exerciceComptable;
}
