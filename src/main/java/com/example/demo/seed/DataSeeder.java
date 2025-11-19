package com.example.demo.seed;

import com.example.demo.entity.Societe;
import com.example.demo.entity.User;
import com.example.demo.repository.SocieteRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final SocieteRepository societeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            AppLogger.header("Data Seeding");

            // Seed societes
            seedSocietes();

            // Seed users
            seedUsers();

            AppLogger.footer("Data Seeding");
        };
    }

    private void seedSocietes() {
        AppLogger.info("Seeding societes...");

        seedSociete("001234567890001", "Tech Solutions SARL",
                "123 Boulevard Mohammed V, Casablanca", "0522-123456",
                "contact@techsolutions.ma");

        seedSociete("001234567890002", "Innovation Industries SA",
                "456 Avenue Hassan II, Rabat", "0537-654321",
                "info@innovation.ma");

        seedSociete("001234567890003", "Digital Services SARL AU",
                "789 Rue Abdelmoumen, Marrakech", "0524-987654",
                "contact@digitalservices.ma");

        AppLogger.success("Societes seeding completed");
        AppLogger.line();
    }

    private void seedSociete(String ice, String raisonSociale, String adresse,
            String telephone, String emailContact) {
        if (societeRepository.findByIce(ice).isEmpty()) {
            Societe societe = Societe.builder()
                    .ice(ice)
                    .raisonSociale(raisonSociale)
                    .adresse(adresse)
                    .telephone(telephone)
                    .emailContact(emailContact)
                    .build();
            societeRepository.save(societe);
            AppLogger.info(String.format("✓ Created societe: %s (ICE: %s)", raisonSociale, ice));
        } else {
            AppLogger.warn(String.format("⊗ Societe already exists: %s (ICE: %s)", raisonSociale, ice));
        }
    }

    private void seedUsers() {
        AppLogger.info("Seeding users...");

        // Seed comptable users (no societe association)
        seedComptableUser("marou@gmail.com", "Ahmed Benjelloun");
        seedComptableUser("yasr@gmail.com", "Fatima El Amrani");

        // Seed societe users (with societe association)
        seedSocieteUser("admin@techsolutions.ma", "Karim Alami", "001234567890001");
        seedSocieteUser("admin@innovation.ma", "Nadia Mansouri", "001234567890002");
        seedSocieteUser("admin@digitalservices.ma", "Youssef Tazi", "001234567890003");

        AppLogger.success("Users seeding completed");
        AppLogger.line();
    }

    private void seedComptableUser(String email, String fullName) {
        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("password123"))
                    .fullName(fullName)
                    .role(User.Role.COMPTABLE)
                    .societe(null)
                    .build();
            userRepository.save(user);
            AppLogger.info(String.format("✓ Created comptable user: %s (%s)", fullName, email));
        } else {
            AppLogger.warn(String.format("⊗ User already exists: %s (%s)", fullName, email));
        }
    }

    private void seedSocieteUser(String email, String fullName, String ice) {
        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            Societe societe = societeRepository.findByIce(ice)
                    .orElseThrow(() -> new RuntimeException("Societe not found with ICE: " + ice));

            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("password123"))
                    .fullName(fullName)
                    .role(User.Role.SOCIETE)
                    .societe(societe)
                    .build();
            userRepository.save(user);
            AppLogger.info(String.format("✓ Created societe user: %s (%s) - associated with %s",
                    fullName, email, societe.getRaisonSociale()));
        } else {
            AppLogger.warn(String.format("⊗ User already exists: %s (%s)", fullName, email));
        }
    }
}
