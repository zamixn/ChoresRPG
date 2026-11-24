package com.zsoftware.choresrpg.cloud;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.zsoftware.choresrpg.R;

// The user's account: "Sign in with Google" (Android's Credential Manager) linked to Firebase
// Authentication. Signing in is optional; the app works the same without an account.
public class AccountManager {
    private static String LOGTAG = "AccountManager";

    public enum SignInResult
    {
        SignedIn,
        Cancelled,
        NoGoogleAccount,
        Failed
    }

    public interface OnSignInFinished
    {
        void SignInFinished(SignInResult result);
    }

    public interface OnSignedOut
    {
        void SignedOut();
    }

    public static boolean IsSignedIn()
    {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    // Firebase's id of the signed-in user (the key of their data in Firestore); null when signed out.
    public static String GetUserId()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : user.getUid();
    }

    // What to show for the signed-in user: their e-mail address, or their name if there is none.
    public static String GetUserLabel()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return null;
        if (user.getEmail() != null && !user.getEmail().isEmpty())
            return user.getEmail();
        return user.getDisplayName();
    }

    // Shows Google's account picker, then signs in to Firebase with the chosen account.
    // The callback runs on the main thread.
    public static void SignIn(Activity activity, OnSignInFinished onFinished)
    {
        // The "Web client" id from google-services.json (generated as R.string.default_web_client_id).
        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        CredentialManager.create(activity).getCredentialAsync(activity, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        SignInToFirebase(response.getCredential(), onFinished);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException)
                            onFinished.SignInFinished(SignInResult.Cancelled);
                        else if (e instanceof NoCredentialException)
                            onFinished.SignInFinished(SignInResult.NoGoogleAccount);
                        else
                        {
                            Log.e(LOGTAG, "SignIn: Google sign-in failed", e);
                            onFinished.SignInFinished(SignInResult.Failed);
                        }
                    }
                });
    }

    private static void SignInToFirebase(Credential credential, OnSignInFinished onFinished)
    {
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType()))
        {
            Log.e(LOGTAG, "SignInToFirebase: unexpected credential type " + credential.getType());
            onFinished.SignInFinished(SignInResult.Failed);
            return;
        }

        String idToken = GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).addOnCompleteListener(task -> {
            if (task.isSuccessful())
                onFinished.SignInFinished(SignInResult.SignedIn);
            else
            {
                Log.e(LOGTAG, "SignInToFirebase: Firebase sign-in failed", task.getException());
                onFinished.SignInFinished(SignInResult.Failed);
            }
        });
    }

    // Signs out of Firebase and clears the remembered Google account choice, so the next sign-in
    // shows the account picker again. Chores stay on the phone.
    public static void SignOut(Context context, OnSignedOut onSignedOut)
    {
        FirebaseAuth.getInstance().signOut();
        CredentialManager.create(context).clearCredentialStateAsync(new ClearCredentialStateRequest(),
                new CancellationSignal(), ContextCompat.getMainExecutor(context),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        onSignedOut.SignedOut();
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        // Already signed out of Firebase; only the remembered account choice stays.
                        Log.e(LOGTAG, "SignOut: couldn't clear the saved Google account", e);
                        onSignedOut.SignedOut();
                    }
                });
    }
}
