# FitMate — Smart Fitness & AI Nutrition App

A JavaFX 21 desktop application for personalized health tracking, AI-powered weekly diet plan generation, and fitness coaching via the Zenith AI Coach.

## Tech Stack
- **Java 21** + **JavaFX 21**
- **Firebase Admin SDK** — Firestore database
- **Firebase Authentication** — REST API (sign-up / sign-in)
- **OpenAI GPT-4 Turbo** — Diet plan generation & AI Coach
- **Maven** — Build & dependency management

## Team
| Name | ID | Module |
|------|----|--------|
| Md Emon Hossain | 20245103161 | Project Architecture & Diet Plan |
| Md. Mahedi Hasan | 20245103132 | Authentication (Login & Signup) |
| Halima Afroz Barna | 20245103138 | Profile Module |
| Tasnin Adiba Orny | 20245103002 | Health Metrics Module |
| Kazi Alinur Islam Shuvo | 20245103124 | AI Coach Module |
| Tanvir Rahman | 20234103355 | Dashboard Module |

## Project Structure
```
src/main/java/com/fitmate/
├── MainApp.java              # JavaFX entry point
├── controller/               # UI controllers (one per module)
├── service/                  # Business logic & API integrations
├── dao/                      # Firestore data access objects
├── model/                    # Plain Java data models
└── util/                     # Utilities (SceneManager, AppColors)

src/main/resources/
├── fxml/                     # FXML view layouts
└── css/styles.css            # Global stylesheet
```

## Setup & Run

### Prerequisites
- JDK 21
- Maven 3.8+

### Configuration
1. Add `serviceAccountKey.json` to the project root (download from Firebase Console → Service Accounts)
2. Create a `.env` file in the project root:
```
FIREBASE_API_KEY=your_firebase_web_api_key
FIREBASE_PROJECT_ID=your_project_id
FIREBASE_CREDENTIALS=serviceAccountKey.json
OPENAI_API_KEY=your_openai_api_key
```

> **Note:** `serviceAccountKey.json` and `.env` are excluded from version control for security.

### Run
```bash
mvn javafx:run
```
