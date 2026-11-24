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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.zsoftware.choresrpg.R;

import java.util.ArrayList;
import java.util.List;

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

    public enum DeleteResult
    {
        Deleted,
        Cancelled,
        Failed
    }

    public interface OnDeleteFinished
    {
        void DeleteFinished(DeleteResult result);
    }

    // Result of asking Google for the user's account: a Firebase credential, or why there is none.
    private interface OnGoogleCredential
    {
        void GotCredential(AuthCredential credential);
        void NoCredential(SignInResult reason);
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
        GetGoogleCredential(activity, new OnGoogleCredential() {
            @Override
            public void GotCredential(AuthCredential credential) {
                FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        onFinished.SignInFinished(SignInResult.SignedIn);
                    else
                    {
                        Log.e(LOGTAG, "SignIn: Firebase sign-in failed", task.getException());
                        onFinished.SignInFinished(SignInResult.Failed);
                    }
                });
            }

            @Override
            public void NoCredential(SignInResult reason) {
                onFinished.SignInFinished(reason);
            }
        });
    }

    // Shows Google's account picker and turns the chosen account into a Firebase credential.
    private static void GetGoogleCredential(Activity activity, OnGoogleCredential onCredential)
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
                        AuthCredential credential = ToFirebaseCredential(response.getCredential());
                        if (credential != null)
                            onCredential.GotCredential(credential);
                        else
                            onCredential.NoCredential(SignInResult.Failed);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException)
                            onCredential.NoCredential(SignInResult.Cancelled);
                        else if (e instanceof NoCredentialException)
                            onCredential.NoCredential(SignInResult.NoGoogleAccount);
                        else
                        {
                            Log.e(LOGTAG, "GetGoogleCredential: Google sign-in failed", e);
                            onCredential.NoCredential(SignInResult.Failed);
                        }
                    }
                });
    }

    private static AuthCredential ToFirebaseCredential(Credential credential)
    {
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType()))
        {
            Log.e(LOGTAG, "ToFirebaseCredential: unexpected credential type " + credential.getType());
            return null;
        }
        String idToken = GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
        return GoogleAuthProvider.getCredential(idToken, null);
    }

    // Signs out of Firebase and clears the remembered Google account choice, so the next sign-in
    // shows the account picker again. Doesn't touch the chores on the phone.
    public static void SignOut(Context context, OnSignedOut onSignedOut)
    {
        FirebaseAuth.getInstance().signOut();
        ClearRememberedAccount(context, onSignedOut::SignedOut);
    }

    private static void ClearRememberedAccount(Context context, Runnable onDone)
    {
        CredentialManager.create(context).clearCredentialStateAsync(new ClearCredentialStateRequest(),
                new CancellationSignal(), ContextCompat.getMainExecutor(context),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        onDone.run();
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        // Only the remembered account choice stays; the next sign-in may preselect it.
                        Log.e(LOGTAG, "ClearRememberedAccount: couldn't clear the saved Google account", e);
                        onDone.run();
                    }
                });
    }

    // Firebase only deletes an account that signed in within the last few minutes.
    private static final long RecentSignInMillis = 4 * 60 * 1000;

    // Deletes the signed-in account: first every chore saved in the cloud, then the Firebase account,
    // then signs out. Doesn't touch the chores on the phone. If the last sign-in isn't recent, Google's
    // account picker is shown once more to confirm who is deleting. Stop ChoreSync before calling.
    public static void DeleteAccount(Activity activity, OnDeleteFinished onFinished)
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
        {
            onFinished.DeleteFinished(DeleteResult.Failed);
            return;
        }

        boolean signedInRecently = user.getMetadata() != null
                && System.currentTimeMillis() - user.getMetadata().getLastSignInTimestamp() < RecentSignInMillis;
        if (signedInRecently)
        {
            DeleteDataAndUser(activity, user, onFinished);
            return;
        }

        GetGoogleCredential(activity, new OnGoogleCredential() {
            @Override
            public void GotCredential(AuthCredential credential) {
                // Fails if a different Google account was picked.
                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        DeleteDataAndUser(activity, user, onFinished);
                    else
                    {
                        Log.e(LOGTAG, "DeleteAccount: couldn't confirm the account", task.getException());
                        onFinished.DeleteFinished(DeleteResult.Failed);
                    }
                });
            }

            @Override
            public void NoCredential(SignInResult reason) {
                onFinished.DeleteFinished(reason == SignInResult.Cancelled ? DeleteResult.Cancelled : DeleteResult.Failed);
            }
        });
    }

    private static void DeleteDataAndUser(Activity activity, FirebaseUser user, OnDeleteFinished onFinished)
    {
        DeleteCloudChores(user.getUid()).addOnCompleteListener(dataTask -> {
            if (!dataTask.isSuccessful())
            {
                Log.e(LOGTAG, "DeleteDataAndUser: couldn't delete the cloud chores", dataTask.getException());
                onFinished.DeleteFinished(DeleteResult.Failed);
                return;
            }
            user.delete().addOnCompleteListener(userTask -> {
                if (!userTask.isSuccessful())
                {
                    Log.e(LOGTAG, "DeleteDataAndUser: couldn't delete the account", userTask.getException());
                    onFinished.DeleteFinished(DeleteResult.Failed);
                    return;
                }
                FirebaseAuth.getInstance().signOut();
                ClearRememberedAccount(activity, () -> onFinished.DeleteFinished(DeleteResult.Deleted));
            });
        });
    }

    // Deletes every document under users/{userId}/chores (Firestore can't delete a whole collection from
    // the app, so documents go in batches of up to 500).
    private static Task<Void> DeleteCloudChores(String userId)
    {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        return firestore.collection("users").document(userId).collection("chores").get()
                .continueWithTask(task -> {
                    List<Task<Void>> commits = new ArrayList<>();
                    WriteBatch batch = firestore.batch();
                    int inBatch = 0;
                    for (DocumentSnapshot document : task.getResult().getDocuments())
                    {
                        batch.delete(document.getReference());
                        if (++inBatch == 500)
                        {
                            commits.add(batch.commit());
                            batch = firestore.batch();
                            inBatch = 0;
                        }
                    }
                    if (inBatch > 0)
                        commits.add(batch.commit());
                    return Tasks.whenAll(commits);
                });
    }
}
