# Loaney - Firebase Setup Guide

Follow this definitive guide to properly set up Firebase for the Loaney app. The app is completely coded and ready to sync with the cloud; you just need to activate the requested services in your Firebase Console.

## Step 1: Core Project Setup & Connection

1. **Create a Firebase Project:**
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Click **Add project** and name it `Loaney`.
   - Google Analytics is optional, but recommended for crash reporting later.

2. **Register Your Android App:**
   - On the Project Overview page, click the **Android icon** inside the circular "Add an app to get started" view.
   - Enter your package name: `com.sbs.loaney`.
   - **Important! Debug signing certificate SHA-1:** You MUST enter your machine's `SHA-1` fingerprint here so that **Google Sign-In** and **Phone Authentication** function properly. 
     *(To get your SHA-1 in Android Studio, open the right-side Gradle panel > app > Tasks > android > `signingReport` and double-click. Copy the `SHA-1` from the console).*
   - Register the app.

3. **Download Config File (The Glue):**
   - Click **Download google-services.json**.
   - Move this file directly into the `app/` folder of the `Loaney` project directory on your computer, replacing any old version that might be sitting there.

---

## Step 2: Enable Firebase Authentication

Because we enforce security rules on our database, users *must* be logged into the app before they can read or write any loans or profile data.

1. In the console sidebar, go to **Build > Authentication**.
2. Click **Get Started**.
3. Go to the **Sign-in method** tab.
4. Enable the following providers one by one:
   - **Email/Password** (Switch 'Enable' toggle ON).
   - **Google** (Switch 'Enable' toggle ON; you'll need to select a support email).
   - **Phone** (Switch 'Enable' toggle ON).

---

## Step 3: Enable Cloud Firestore (Your Database)

The app is mapped to use **Cloud Firestore** — Google's NoSQL database — as its primary storage engine for all User Profiles, Loans, Payments, Items, and Bank Accounts. This integrates flawlessly with your data structure requirements.

1. Go to **Build > Firestore Database** in the console sidebar.
2. Click **Create database**.
3. **Database Location**: Choose a region closest to your users (e.g., `nam5` for Central US, or `asia-south1` for Mumbai). Pick a *single region* to stay within the free tier. Click **Next**.
4. Choose **Start in Production mode**. Click **Create**.

### Applying Security Rules (CRITICAL FOR FREE TIER)
If your database isn't secured, bots can scrape it and exhaust your free read/write quota within hours.

1. Once the Firestore database is created, click the **Rules** tab at the top.
2. Delete the contents of the file and paste this exact rule set:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // This entirely locks down the database so users can ONLY read, write, 
    // and access their own personal profile, loans, and banking data!
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
3. Click **Publish**. Your data is now perfectly secure!

---

## Step 4: Test It Out!

The Firebase architecture is fully built! 

To verify everything is working:
1. Open Android Studio and select **Build > Clean Project**, followed by **Build > Rebuild Project**.
2. Run the `Loaney` app on an emulator or physical device.
3. Sign up via Email or Google inside the app.
4. After signing up, refresh your Firebase Console browser page. 
   - Check **Authentication > Users** to see your login.
   - Check **Firestore Database > Data** to see your `users` collection populated with your initial profile info!

---

## How the App Stores Loan Data for Free

You do **not** need to add any more code to the app to sync your loans! I have already programmed the app to do this automatically.

Here is exactly how the app optimizes your free tier usage for Loan data:

1. **Subcollections:** When a user creates a Loan, the app saves it into a special subcollection path in Firestore: 
   `users/{userId}/loans/{loanId}`
   This structure ensures that searching for a user's loans requires scanning very few documents, saving massive amounts of your daily free reads!
   
2. **Offline Caching:** The Firestore SDK in the app is configured to use local caching. This means if a user looks at their Dashboard, adds a Loan, and opens the app again later, it loads the Loan from their *local device memory* first. It only reaches out to the cloud if there is new data to sync! This guarantees you stay well within your daily 50,000 free Document Reads.

3. **Payments and Items:** Just like Loans, any `Payment` or `LoanItem` added to a specific loan is nested cleanly underneath that exact loan document:
   `users/{userId}/loans/{loanId}/payments/{paymentId}`
   This hierarchical design prevents you from having to do costly database-wide searches.

**In summary:** As long as you followed Step 3 to enable **Cloud Firestore** and applied the security rule, your app will instantly and securely backup all Loan Data to the cloud 100% for free!

*Enjoy your fully synchronized, cloud-ready application!*
