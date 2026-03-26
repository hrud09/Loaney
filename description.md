# App Description: Loaney

## Overview
**Loaney** is an open-source Android application built to help users manage their personal finances, specifically focusing on tracking lent and borrowed money, recording payments, managing loan items, and tracking balances across various bank accounts. It provides a simple, fluid, and beautiful UI for maintaining a history of debts to ensure financial clarity among friends, family, and associates.

## Tech Stack
- **Platform**: Android
- **UI Toolkit**: Jetpack Compose (Modern, declarative UI)
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Dagger-Hilt
- **Authentication**: Firebase Authentication (Email/Password)
- **Database**: Firebase Cloud Firestore (with Offline Persistence)
- **Local Persistence / Preferences**: DataStore (for settings/preferences)

## Key Features
- **User Authentication**: Secure Sign-Up and Log-In.
- **Loan Tracking**: Add operations for lending or borrowing money/items.
- **Payment History**: Add partial or full payments against a specific loan.
- **Bank Account Management**: Track different bank accounts or wallets and their balances.
- **Real-time Sync**: Data is synced in real-time across devices using Firebase Firestore's Snapshot Listeners.
- **Offline Support**: Leverages Firestore's built-in offline caching so users can view and edit data without an internet connection.

---

## Data Structures & Firestore Schema

Data is stored in **Firebase Cloud Firestore**. The database utilizes root-level collections and subcollections for related data.

### 1. `users` (Root Collection)
Stores the profiles for authenticated users.
- **Document ID**: Firebase Authentication User UID.
- **Fields**:
  - `name` *(String)*: The user's displayed name.
  - `email` *(String)*: The user's email address.
  - `currency` *(String)*: The user's preferred local currency symbol (e.g., `৳`, `$`, `€`).
  - `createdAt` *(Number/Long)*: Unix timestamp of account creation.

### 2. `loans` (Root Collection)
Stores the primary lending and borrowing records.
- **Document ID**: Custom ID based on Unix timestamp (`String`).
- **Fields**:
  - `id` *(Number/Long)*: Unique identifier (same as Document ID).
  - `type` *(String/Enum)*: `LEND` or `BORROW`.
  - `personName` *(String)*: Name of the person the user is transacting with.
  - `phoneNumber` *(String)*: Contact number.
  - `email` *(String, optional)*: Contact email.
  - `address` *(String, optional)*: Contact address.
  - `amount` *(Number/Double)*: The principal amount of the loan.
  - `loanDate` *(Timestamp/Date)*: The date the loan was initiated.
  - `promisedReturnDate` *(Timestamp/Date)*: The expected date of return.
  - `purpose` *(String, optional)*: Reason for the loan.
  - `notes` *(String, optional)*: Additional context.
  - `interest` *(Number/Double, optional)*: Interest rate or flat interest applied.
  - `proofUri` *(String, optional)*: Local URI or Firebase Storage URL for document proof.
  - `profilePhotoUri` *(String, optional)*: Photo of the person.
  - `status` *(String/Enum)*: `ACTIVE`, `FULLY_PAID`, or `FORGIVEN`.
  - `relationshipType` *(String, optional)*: e.g., Friend, Family, Colleague.
  - `witness` *(String, optional)*: Name of a witness present during the transaction.
  - `isDeleted` *(Boolean)*: Soft-delete flag for the recycle bin.
  - `removedAt` *(Number/Long, optional)*: Timestamp when the item was soft-deleted.
  - `createdAt` *(Number/Long)*: Document creation timestamp.

#### ↳ `payments` (Subcollection under a `loan` document)
Stores payment installments made towards a specific loan.
- **Document ID**: Custom ID based on Unix timestamp (`String`).
- **Fields**:
  - `id` *(Number/Long)*: Unique payment identifier.
  - `loanId` *(Number/Long)*: Reference to the parent loan.
  - `amount` *(Number/Double)*: Amount paid in this installment.
  - `paymentDate` *(Timestamp/Date)*: When the payment was made.
  - `note` *(String, optional)*: Context for the payment.
  - `proofUri` *(String, optional)*: Receipt or proof of payment.
  - `createdAt` *(Number/Long)*: Document creation timestamp.

#### ↳ `loanItems` (Subcollection under a `loan` document)
Stores non-monetary items associated with a loan (e.g., lending a book or laptop).
- **Document ID**: Custom ID based on Unix timestamp (`String`).
- **Fields**:
  - `id` *(Number/Long)*: Unique item identifier.
  - `loanId` *(Number/Long)*: Reference to the parent loan.
  - `itemName` *(String)*: Name of the item.
  - `description` *(String, optional)*: Details about the condition or specifics.
  - `estimatedValue` *(Number/Double, optional)*: Approximate monetary value.
  - `isReturned` *(Boolean)*: Status flag.
  - `returnDate` *(Timestamp/Date, optional)*: When the item was returned.
  - `photoUri` *(String, optional)*: Picture of the item.
  - `createdAt` *(Number/Long)*: Document creation timestamp.

### 3. `bankAccounts` (Root Collection)
Stores the user's personal bank accounts or virtual wallets.
- **Document ID**: Custom ID based on Unix timestamp (`String`).
- **Fields**:
  - `id` *(Number/Long)*: Unique bank account identifier.
  - `bankName` *(String)*: Name of the institution (e.g., Bkash, Chase).
  - `accountName` *(String)*: Account holder's name.
  - `accountNumber` *(String, optional)*: Last 4 digits or full number.
  - `initialBalance` *(Number/Double)*: Starting balance.
  - `colorHex` *(String, optional)*: UI color code for the card.
  - `createdAt` *(Number/Long)*: Document creation timestamp.
