package com.zsoftware.choresrpg.cloud;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.settings.AppSettings;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// The signed-in user's group. A user is always in exactly one group, and the chores live in it
// (groups/{groupId}/chores, synced by ChoreSync). Signing in for the first time creates "name's group".
//
// Firestore:
//   users/{userId}      groupId, groupName   (which group the user is in)
//   groups/{groupId}    name, ownerId, members [user ids, in joining order], memberNames {id: name}, inviteCode
//   invites/{code}      groupId, groupName   (looked up when an invite link is opened or a code is typed in)
//
// Being removed: the creator takes the user out of the group's members; the next time the app looks up
// the user's group (or while it's open) it notices, remembers a notice to show, and gives the user a new
// default group, keeping the chores on the phone.
public class GroupManager {
    private static String LOGTAG = "GroupManager";

    // Invite links open the app directly (JoinGroupActivity); no website is involved.
    public static final String InviteLinkBase = "choresrpg://join/";

    public interface OnGroupReady
    {
        void GroupReady(String groupId);
    }

    public interface OnDone
    {
        void Done(boolean success);
    }

    public interface OnInviteCode
    {
        // Null when the code couldn't be made.
        void InviteCode(String code);
    }

    // Screens showing the group register while visible, to redraw when it changes (renamed, members).
    public interface OnGroupChanged
    {
        void GroupChanged();
    }
    private static final List<OnGroupChanged> Listeners = new ArrayList<>();
    public static void AddListener(OnGroupChanged listener) { Listeners.add(listener); }
    public static void RemoveListener(OnGroupChanged listener) { Listeners.remove(listener); }

    private static Context AppContext;
    private static Group CurrentGroup;
    private static ListenerRegistration GroupRegistration;
    // While the group is being looked up, more requests just wait for the same answer.
    private static boolean Resolving = false;
    private static final List<OnGroupReady> WaitingForGroup = new ArrayList<>();

    // The group the user is in; null while signed out or not looked up yet.
    public static Group GetCurrentGroup() { return CurrentGroup; }
    public static String GetCurrentGroupId() { return CurrentGroup == null ? null : CurrentGroup.Id; }

    private static FirebaseFirestore Db() { return FirebaseFirestore.getInstance(); }
    private static DocumentReference UserDoc(String userId) { return Db().collection("users").document(userId); }
    private static DocumentReference GroupDoc(String groupId) { return Db().collection("groups").document(groupId); }
    private static DocumentReference InviteDoc(String code) { return Db().collection("invites").document(code); }

    // Finds the signed-in user's group (creating the default group the first time, or after being
    // removed from one), starts watching it, then calls onReady on the main thread.
    public static void Resolve(Context context, OnGroupReady onReady)
    {
        AppContext = context.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        if (CurrentGroup != null && GroupRegistration != null)
        {
            onReady.GroupReady(CurrentGroup.Id);
            return;
        }
        WaitingForGroup.add(onReady);
        if (Resolving)
            return;
        Resolving = true;

        String userId = user.getUid();
        UserDoc(userId).get().addOnCompleteListener(userTask -> {
            if (!userTask.isSuccessful())
            {
                // Offline with nothing cached: try again the next time a screen opens.
                Log.e(LOGTAG, "Resolve: couldn't read the user", userTask.getException());
                FinishResolving(null);
                return;
            }
            DocumentSnapshot userDoc = userTask.getResult();
            String groupId = userDoc.getString("groupId");
            String groupName = userDoc.getString("groupName");
            if (groupId == null)
            {
                // First sign-in with groups: create "name's group". The chores on the phone go into it.
                CreateGroup(GetDefaultGroupName(), true).addOnCompleteListener(task -> FinishResolving(
                        task.isSuccessful() ? task.getResult() : null));
                return;
            }

            GroupDoc(groupId).get().addOnCompleteListener(groupTask -> {
                boolean member = groupTask.isSuccessful() && groupTask.getResult().exists()
                        && Group.FromDocument(groupTask.getResult()).IsMember(userId);
                if (member)
                {
                    SetCurrentGroup(Group.FromDocument(groupTask.getResult()));
                    FinishResolving(groupId);
                }
                else if (WasRemoved(groupTask))
                    OnRemovedFromGroup(groupName, newGroupId -> FinishResolving(newGroupId));
                else
                {
                    Log.e(LOGTAG, "Resolve: couldn't read the group", groupTask.getException());
                    FinishResolving(null);
                }
            });
        });
    }

    // The group is gone or refuses the user (no longer a member) - not just a connection problem.
    private static boolean WasRemoved(Task<DocumentSnapshot> groupTask)
    {
        if (groupTask.isSuccessful())
            return true;   // read fine, but missing or without the user
        Exception error = groupTask.getException();
        return error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
    }

    private static void FinishResolving(String groupId)
    {
        Resolving = false;
        List<OnGroupReady> waiting = new ArrayList<>(WaitingForGroup);
        WaitingForGroup.clear();
        if (groupId == null)
            return;
        WatchGroup(groupId);
        for (OnGroupReady callback : waiting)
            callback.GroupReady(groupId);
    }

    // Removed from the group (by its creator): remember a notice for the next screen, move into a new
    // default group, keep the chores on the phone (they go into the new group).
    private static void OnRemovedFromGroup(String groupName, OnGroupReady onReady)
    {
        AppSettings.Initialize(AppContext);
        AppSettings.Instance().SetRemovedFromGroup(groupName == null ? "" : groupName);
        StopWatching();
        CurrentGroup = null;
        SaveData.Instance().ClearPendingDeletes();
        CreateGroup(GetDefaultGroupName(), false).addOnCompleteListener(task -> {
            if (task.isSuccessful())
                onReady.GroupReady(task.getResult());
            else
                Log.e(LOGTAG, "OnRemovedFromGroup: couldn't create a new group", task.getException());
        });
    }

    // Keeps CurrentGroup up to date (rename, members). Notices being removed while the app is open.
    private static void WatchGroup(String groupId)
    {
        if (GroupRegistration != null && CurrentGroup != null && CurrentGroup.Id.equals(groupId))
            return;
        StopWatching();
        GroupRegistration = GroupDoc(groupId).addSnapshotListener((snapshot, error) -> {
            String userId = AccountManager.GetUserId();
            if (userId == null)
                return;
            boolean removed;
            if (error != null)
                removed = error instanceof FirebaseFirestoreException
                        && ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
            else
                removed = snapshot != null && !snapshot.getMetadata().hasPendingWrites()
                        && (!snapshot.exists() || !Group.FromDocument(snapshot).IsMember(userId));
            if (removed)
            {
                String name = CurrentGroup == null ? "" : CurrentGroup.Name;
                ChoreSync.Stop();
                OnRemovedFromGroup(name, newGroupId -> {
                    WatchGroup(newGroupId);
                    ChoreSync.Start(AppContext);
                    NotifyChanged();
                });
                return;
            }
            if (snapshot != null && snapshot.exists())
            {
                SetCurrentGroup(Group.FromDocument(snapshot));
                NotifyChanged();
            }
        });
    }

    private static void StopWatching()
    {
        if (GroupRegistration != null)
            GroupRegistration.remove();
        GroupRegistration = null;
    }

    private static void SetCurrentGroup(Group group)
    {
        CurrentGroup = group;
    }

    private static void NotifyChanged()
    {
        for (OnGroupChanged listener : new ArrayList<>(Listeners))
            listener.GroupChanged();
    }

    // Forgets the group (after signing out or deleting the account).
    public static void Clear()
    {
        StopWatching();
        CurrentGroup = null;
        Resolving = false;
        WaitingForGroup.clear();
    }

    // e.g. "Žygimantas's group": the first name of the Google account (or the e-mail name).
    public static String GetDefaultGroupName()
    {
        return AppContext.getString(R.string.group_default_name, GetUserName());
    }

    private static String GetUserName()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return "";
        String name = user.getDisplayName();
        if (name != null && !name.trim().isEmpty())
            return name.trim().split("\\s+")[0];
        String email = user.getEmail();
        if (email != null && email.contains("@"))
            return email.substring(0, email.indexOf('@'));
        return AppContext.getString(R.string.group_default_owner);
    }

    // Creates a group with the user as its only member and moves the user into it; returns its id.
    // 'firstGroup': also clears chores saved in the cloud before groups existed (users/{id}/chores) -
    // the chores on the phone go into the new group instead.
    private static Task<String> CreateGroup(String name, boolean firstGroup)
    {
        String userId = AccountManager.GetUserId();
        DocumentReference group = Db().collection("groups").document();
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("ownerId", userId);
        List<String> members = new ArrayList<>();
        members.add(userId);
        data.put("members", members);
        Map<String, String> names = new HashMap<>();
        names.put(userId, GetUserName());
        data.put("memberNames", names);
        data.put("createdAt", System.currentTimeMillis());

        WriteBatch batch = Db().batch();
        batch.set(group, data);
        batch.set(UserDoc(userId), UserData(group.getId(), name), SetOptions.merge());
        return batch.commit().continueWithTask(task -> {
            if (!task.isSuccessful())
                throw task.getException();
            SetCurrentGroup(new Group(group.getId(), name, userId, members, names, null));
            if (firstGroup)
                DeleteCollection(UserDoc(userId).collection("chores"));
            return Tasks.forResult(group.getId());
        });
    }

    private static Map<String, Object> UserData(String groupId, String groupName)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("groupName", groupName);
        return data;
    }

    // --- Actions from the group screens. Callbacks run on the main thread. ---

    public static void Rename(String name, OnDone onDone)
    {
        Group group = CurrentGroup;
        if (group == null)
        {
            onDone.Done(false);
            return;
        }
        WriteBatch batch = Db().batch();
        batch.update(GroupDoc(group.Id), "name", name);
        if (group.InviteCode != null)
            batch.update(InviteDoc(group.InviteCode), "groupName", name);
        batch.set(UserDoc(AccountManager.GetUserId()), UserData(group.Id, name), SetOptions.merge());
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                Log.e(LOGTAG, "Rename: failed", task.getException());
            onDone.Done(task.isSuccessful());
        });
    }

    // Creator only: takes someone out of the group. They're told the next time they open the app.
    public static void RemoveMember(String memberId, OnDone onDone)
    {
        Group group = CurrentGroup;
        if (group == null || !group.OwnerId.equals(AccountManager.GetUserId()) || memberId.equals(group.OwnerId))
        {
            onDone.Done(false);
            return;
        }
        GroupDoc(group.Id).update("members", FieldValue.arrayRemove(memberId),
                        "memberNames." + memberId, FieldValue.delete())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Log.e(LOGTAG, "RemoveMember: failed", task.getException());
                    onDone.Done(task.isSuccessful());
                });
    }

    // The link that opens the app's join screen for this invite code.
    public static String GetInviteLink(String code)
    {
        return InviteLinkBase + code;
    }

    // The invite code in what the user typed or pasted: the code itself, or a whole invite link
    // (choresrpg://join/CODE). Null when there's nothing usable.
    public static String ParseInviteCode(String text)
    {
        if (text == null)
            return null;
        String code = text.trim();
        int slash = code.lastIndexOf('/');
        if (slash >= 0)
            code = code.substring(slash + 1);
        code = code.replace(" ", "").replace("-", "").toLowerCase(Locale.ROOT);
        if (code.isEmpty() || !code.matches("[a-z0-9]+"))
            return null;
        return code;
    }

    // The group's invite code (made the first time it's needed).
    public static void GetInviteCode(OnInviteCode onCode)
    {
        Group group = CurrentGroup;
        if (group == null)
        {
            onCode.InviteCode(null);
            return;
        }
        if (group.InviteCode != null)
        {
            onCode.InviteCode(group.InviteCode);
            return;
        }
        String code = NewInviteCode();
        Map<String, Object> invite = new HashMap<>();
        invite.put("groupId", group.Id);
        invite.put("groupName", group.Name);
        WriteBatch batch = Db().batch();
        batch.set(InviteDoc(code), invite);
        batch.update(GroupDoc(group.Id), "inviteCode", code);
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                Log.e(LOGTAG, "GetInviteCode: failed", task.getException());
            onCode.InviteCode(task.isSuccessful() ? code : null);
        });
    }

    private static String NewInviteCode()
    {
        String letters = "abcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 12; i++)
            code.append(letters.charAt(random.nextInt(letters.length())));
        return code.toString();
    }

    public static class InviteInfo
    {
        public final String Code;
        public final String GroupId;
        public final String GroupName;

        InviteInfo(String code, String groupId, String groupName)
        {
            Code = code;
            GroupId = groupId;
            GroupName = groupName;
        }
    }

    public interface OnInvite
    {
        // Null when the link isn't valid (anymore).
        void Invite(InviteInfo invite);
    }

    public static void LookUpInvite(String code, OnInvite onInvite)
    {
        InviteDoc(code).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || !task.getResult().exists() || task.getResult().getString("groupId") == null)
            {
                Log.e(LOGTAG, "LookUpInvite: not found", task.getException());
                onInvite.Invite(null);
                return;
            }
            onInvite.Invite(new InviteInfo(code, task.getResult().getString("groupId"), task.getResult().getString("groupName")));
        });
    }

    // Joins the invite's group, leaving the current one. 'bringChores': the chores on the phone go into the
    // new group too; otherwise they're removed from the phone (they stay in the old group if anyone's left).
    public static void Join(InviteInfo invite, boolean bringChores, OnDone onDone)
    {
        String userId = AccountManager.GetUserId();
        Group oldGroup = CurrentGroup;
        PrepareToSwitch(bringChores);

        GroupDoc(invite.GroupId).update("members", FieldValue.arrayUnion(userId),
                        "memberNames." + userId, GetUserName(),
                        "joinCode", invite.Code)
                .continueWithTask(task -> {
                    if (!task.isSuccessful())
                        throw task.getException();
                    return LeaveGroup(oldGroup, userId);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful())
                        throw task.getException();
                    return UserDoc(userId).set(UserData(invite.GroupId, invite.GroupName), SetOptions.merge());
                })
                .addOnCompleteListener(task -> FinishSwitch(task, "Join", onDone));
    }

    // Leaves the group for a new default group. 'keepChores': the new group starts with a copy of the
    // chores on the phone; otherwise empty.
    public static void Leave(boolean keepChores, OnDone onDone)
    {
        SwitchToNewGroup(GetDefaultGroupName(), keepChores, onDone);
    }

    // Leaves the group for a new group with this name. 'bringChores': like Leave's keepChores.
    public static void CreateNewGroup(String name, boolean bringChores, OnDone onDone)
    {
        SwitchToNewGroup(name, bringChores, onDone);
    }

    private static void SwitchToNewGroup(String name, boolean keepChores, OnDone onDone)
    {
        String userId = AccountManager.GetUserId();
        Group oldGroup = CurrentGroup;
        PrepareToSwitch(keepChores);
        LeaveGroup(oldGroup, userId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful())
                        throw task.getException();
                    return CreateGroup(name, false);
                })
                .addOnCompleteListener(task -> FinishSwitch(task, "SwitchToNewGroup", onDone));
    }

    // Before moving to another group: stop syncing the old one, and decide what happens to the phone's chores.
    private static void PrepareToSwitch(boolean keepChores)
    {
        ChoreSync.Stop();
        StopWatching();
        if (keepChores)
            SaveData.Instance().ClearPendingDeletes();   // deletions belonged to the old group
        else
            SaveData.Instance().ClearLocalChores();
    }

    private static void FinishSwitch(Task<?> task, String what, OnDone onDone)
    {
        if (!task.isSuccessful())
            Log.e(LOGTAG, what + ": failed", task.getException());
        CurrentGroup = null;
        // Looks up the (new) group and syncs its chores; on failure, carries on with the old one.
        ChoreSync.Start(AppContext);
        onDone.Done(task.isSuccessful());
    }

    // Takes the user out of a group: the last member deletes it (with its chores and invite); otherwise the
    // creator role passes on to the member who joined earliest.
    private static Task<Void> LeaveGroup(Group group, String userId)
    {
        if (group == null)
            return Tasks.forResult(null);
        String newOwner = group.GetOwnerAfterLeaving(userId);
        if (newOwner == null)
        {
            return DeleteCollection(GroupDoc(group.Id).collection("chores")).continueWithTask(task -> {
                WriteBatch batch = Db().batch();
                if (group.InviteCode != null)
                    batch.delete(InviteDoc(group.InviteCode));
                batch.delete(GroupDoc(group.Id));
                return batch.commit();
            });
        }
        return GroupDoc(group.Id).update("members", FieldValue.arrayRemove(userId),
                "memberNames." + userId, FieldValue.delete(),
                "ownerId", newOwner);
    }

    // Account deletion: leaves (or deletes) the group and deletes the user's record. Stop ChoreSync first.
    public static Task<Void> DeleteUserData(String userId)
    {
        Group group = CurrentGroup;
        StopWatching();
        return LeaveGroup(group, userId)
                .continueWithTask(task -> DeleteCollection(UserDoc(userId).collection("chores")))
                .continueWithTask(task -> UserDoc(userId).delete())
                .addOnCompleteListener(task -> CurrentGroup = null);
    }

    // Deletes every document in a collection (in batches of up to 500; the app can't delete a collection).
    private static Task<Void> DeleteCollection(com.google.firebase.firestore.CollectionReference collection)
    {
        return collection.get().continueWithTask(task -> {
            if (!task.isSuccessful())
                return Tasks.forResult(null);   // e.g. nothing there / not allowed: nothing to delete
            List<Task<Void>> commits = new ArrayList<>();
            WriteBatch batch = Db().batch();
            int inBatch = 0;
            for (DocumentSnapshot document : task.getResult().getDocuments())
            {
                batch.delete(document.getReference());
                if (++inBatch == 500)
                {
                    commits.add(batch.commit());
                    batch = Db().batch();
                    inBatch = 0;
                }
            }
            if (inBatch > 0)
                commits.add(batch.commit());
            return Tasks.whenAll(commits);
        });
    }
}
