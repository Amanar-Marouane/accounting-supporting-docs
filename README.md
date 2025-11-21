# Accounting Supporting Documents Service

Service de gestion des pièces comptables pour le Cabinet Comptable Al Amane.

## Technologies

- Java 17
- Spring Boot 3.4.0
- Spring Security 6
- H2 Database
- JWT Authentication
- Docker

## Prérequis

- JDK 17+
- Maven 3.9+
- Docker (optionnel)

## Installation

### Avec Maven

1. Cloner le projet
```bash
git clone https://github.com/Amanar-Marouane/accounting-supporting-docs.git
cd accounting-supporting-docs
```

2. Créer le fichier de configuration
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

3. Compiler et lancer
```bash
mvn clean install
mvn spring-boot:run
```

### Avec Docker

1. Construire l'image
```bash
docker build -t accounting-support-service .
```

2. Lancer le conteneur
```bash
docker run -p 8080:8080 accounting-support-service
```

### Avec Docker Compose

```bash
docker-compose up -d
```

## Tests

### Lancer tous les tests
```bash
mvn test
```

### Lancer un test spécifique
```bash
mvn test -Dtest=DocumentServiceTest
```

### Générer le rapport de couverture
```bash
mvn clean test jacoco:report
```

## Seed Data

Pour charger des données de test:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Connexion
- `POST /api/auth/logout` - Déconnexion (authentifié)

### Société Routes
- `POST /api/societe/documents/upload` - Upload document
- `GET /api/societe/documents` - Liste des documents
- `GET /api/societe/documents/exercice/{exercice}` - Documents par exercice
- `GET /api/societe/documents/{id}` - Détails d'un document
- `GET /api/societe/documents/{id}/download` - Télécharger un document

### Comptable Routes
- `GET /api/comptable/documents/pending` - Documents en attente
- `GET /api/comptable/documents/pending/exercice/{exercice}` - Documents en attente par exercice
- `GET /api/comptable/documents/societe/{societeId}` - Documents d'une société
- `POST /api/comptable/documents/{id}/validate` - Valider/Rejeter un document
- `GET /api/comptable/documents/{id}` - Détails d'un document
- `GET /api/comptable/documents/{id}/download` - Télécharger un document

## Configuration

Les variables d'environnement principales:

- `SERVER_PORT` - Port du serveur (défaut: 8080)
- `SPRING_DATASOURCE_URL` - URL de la base de données
- `SECURITY_JWT_SECRET_KEY` - Clé secrète JWT
- `SECURITY_JWT_EXPIRATION_TIME` - Durée de validité du token (ms)

## Docker Hub

Pour pousser l'image sur Docker Hub:

```bash
docker tag accounting-support-service username/accounting-support-service:latest
docker push username/accounting-support-service:latest
```

## CI/CD

Le projet utilise GitHub Actions pour:
- Exécuter les tests automatiquement
- Construire l'application
- Créer et publier l'image Docker (sur push vers main)

## Licence

© 2024 Cabinet Comptable Al Amane
