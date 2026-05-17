# ReceiptFlow 

ReceiptFlow is a professional Android application designed to bridge the gap between accountants and their clients. It provides a structured, secure, and aesthetic way to manage financial receipts, organize them by upload date, and generate comprehensive monthly reports.

---

## Key Features

### Customer Portal
*   **Multi-Source Upload**: Capture receipts directly via the Camera or select existing images from the Gallery.
*   **Smart Processing**: Automatic image rotation correction and compression (85% quality) to ensure fast uploads and low data usage.
*   **Contextual Comments**: Add notes to each receipt so your accountant knows exactly what they are for.
*   **History Tracking**: Browse through your own uploaded receipts, filtered automatically by the current month.
*   **Full Control**: Ability to delete previous uploads with a simple long-press.

### Accountant Dashboard
*   **Client Management**: A streamlined Spinner interface to quickly switch between all assigned customers.
*   **Monthly Filtering**: Powerful Year/Month selectors to jump to any tax period instantly.
*   **Visual Review**: High-quality thumbnails in the list and a full-screen image viewer for detailed inspections.
*   **Feedback Loop**: Update the status of any receipt (e.g., *Approved*, *Image Unclear*, *Not Relevant*) with a long-press.
*   **PDF Export**: Generate professional A4 PDF reports of all receipts for a selected month, saved directly to the device's public Downloads folder.

### Manager (Admin) Dashboard
*   **User Oversight**: Tabbed interface to manage both Customer and Accountant databases.
*   **Dynamic Assignment**: Assign (or re-assign) customers to specific accountants using a simple dialog.
*   **Data Migration**: When a customer switches accountants, their entire receipt history is automatically transferred to the new accountant.
*   **System Cleanup**: Securely delete users with full background cleanup of their database records and physical files.

---

## APIs and Technologies

### Firebase Cloud Platform
*   **Firebase Authentication**: Provides secure, encrypted login and registration logic for all three user roles.
*   **Cloud Firestore**: A real-time NoSQL database used to store user profiles, receipt data, and accountant-customer relationships.
*   **Firebase Storage**: Hosts the physical image files with organized folder structures (`receipts/{uid}/{year}/{month}/`).
*   **Firebase BoM**: Ensures all cloud libraries are synchronized and version-compatible.

### Android Jetpack & Core APIs
*   **PdfDocument API**: Powering the `PdfGenerator` to programmatically draw high-resolution images and text onto official PDF documents.
*   **MediaStore API**: Handles the modern "Scoped Storage" logic to save reports into the public Downloads folder without requiring intrusive permissions.
*   **ActivityResult API**: Modern implementation for handling Camera and Gallery interactions securely.
*   **ExifInterface API**: Reads hidden metadata to ensure photos are always displayed in the correct orientation.
*   **View Binding**: Generates type-safe links between Kotlin code and XML layouts to prevent null-pointer crashes.
*   **Coroutines**: Manages background processing for database calls and image compression to keep the UI smooth.

### UI & Design
*   **Material Design 3**: Uses the latest Material components (Cards, FABs, TabLayouts) for a modern feel.
*   **Gruvbox Theme**: A custom-built Retro-Groove palette available in both Light (Cream/Charcoal) and Dark (Charcoal/Cream) modes, optimized for high readability.
*   **Glide API**: An advanced image loading and caching library used to display receipt thumbnails and full-screen previews instantly.


