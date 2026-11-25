package com.zsoftware.choresrpg.cloud;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.zsoftware.choresrpg.Chore;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.settings.AppSettings;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// The signed-in user's groups. A user can be in several groups and is always in at least one: the first
// time (or after leaving / being removed from the last one) "name's group" is created. Every chore belongs
// to one group (Chore.GetGroupId) and is shared with its members; ChoreSync syncs every group's chores.
// The main group (a setting) is where new chores go by default.
//
// Firestore:
//   groups/{groupId}    name, ownerId, members [user ids, in joining order], memberNames {id: name},
//                       inviteCode, createdAt     (the user's groups: the ones listing them as a member)
//   invites/{code}      groupId, groupName   (looked up when an invite link is opened or a code is typed in)
//
// Being removed: the creator takes the user out of a group's members. The group then drops out of the
// user's group list; the app compares that list with the groups it last saw (SaveData.GetKnownGroups), so
// it notices even if it was closed at the time: it remembers a notice to show and removes that group's
// chores from the phone.
public class GroupManager {
    private static String LOGTAG = "GroupManager";

    // Invite links open the app directly (JoinGroupActivity); no website is involved.
    public static final String InviteLinkBase = "choresrpg://join/";

    public interface OnDone
    {
        void Done(boolean success);
    }

    public interface OnGroupCreated
    {
        // Null when the group couldn't be created.
        void GroupCreated(String groupId);
    }

    public interface OnInviteCode
    {
        // Null when the code couldn't be made.
        void InviteCode(String code);
    }

    // Screens showing groups register while visible, to redraw when they change (loaded, renamed, members,
    // joined, left, removed).
    public interface OnGroupChanged
    {
        void GroupChanged();
    }
    private static final List<OnGroupChanged> Listeners = new ArrayList<>();
    public static void AddListener(OnGroupChanged listener) { Listeners.add(listener); }
    public static void RemoveListener(OnGroupChanged listener) { Listeners.remove(listener); }

    private static Context AppContext;
    private static ListenerRegistration GroupsRegistration;
    // The user's groups, oldest first.
    private static final Map<String, Group> Groups = new LinkedHashMap<>();
    // True once the cloud (not just the phone's cache) has said which groups the user is in.
    private static boolean Loaded = false;
    private static boolean LoadFailed = false;
    private static boolean CreatingDefaultGroup = false;
    // Groups being left: dropping out of the list isn't "removed" for them.
    private static final Set<String> LeavingGroupIds = new HashSet<>();

    // The user's groups, oldest first; empty while signed out or not looked up yet.
    public static List<Group> GetGroups() { return new ArrayList<>(Groups.values()); }
    public static Group GetGroup(String groupId) { return groupId == null ? null : Groups.get(groupId); }
    public static boolean IsLoaded() { return Loaded; }
    // The groups couldn't be loaded (e.g. offline the first time); opening a screen tries again.
    public static boolean HasLoadFailed() { return LoadFailed && !Loaded; }

    // The group new chores go into: the chosen main group, or the oldest group if none is chosen (or it's
    // gone). Null while no groups are known.
    public static String GetMainGroupId()
    {
        if (Groups.isEmpty())
            return null;
        AppSettings.Initialize(AppContext);
        String chosen = AppSettings.Instance().GetMainGroupId();
        if (chosen != null && Groups.containsKey(chosen))
            return chosen;
        return Groups.keySet().iterator().next();
    }

    public static Group GetMainGroup() { return GetGroup(GetMainGroupId()); }

    public static void SetMainGroup(String groupId)
    {
        AppSettings.Initialize(AppContext);
        AppSettings.Instance().SetMainGroupId(groupId);
        NotifyChanged();
    }

    private static FirebaseFirestore Db() { return FirebaseFirestore.getInstance(); }
    private static DocumentReference UserDoc(String userId) { return Db().collection("users").document(userId); }
    private static DocumentReference GroupDoc(String groupId) { return Db().collection("groups").document(groupId); }
    private static DocumentReference InviteDoc(String code) { return Db().collection("invites").document(code); }

    // Starts watching the signed-in user's groups (once; later calls only restart chore syncing that
    // stopped). Called by ChoreSync.Start.
    static void Start(Context context)
    {
        AppContext = context.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        if (GroupsRegistration != null)
        {
            if (Loaded)
                ChoreSync.SyncGroups(GetSyncedGroupIds());
            return;
        }
        String userId = user.getUid();
        LoadFailed = false;
        // Metadata changes too: the first answer may come from the phone's cache, and only the cloud's
        // answer can tell that a group is gone.
        GroupsRegistration = Db().collection("groups").whereArrayContains("members", userId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (!userId.equals(AccountManager.GetUserId()))
                        return;
                    if (error != null)
                    {
                        // Firestore stops the listener after an error; the next screen that opens tries again.
                        Log.e(LOGTAG, "Start: couldn't load the groups", error);
                        StopWatching();
                        LoadFailed = true;
                        NotifyChanged();
                        return;
                    }
                    if (snapshot != null)
                        OnGroups(snapshot);
                });
    }

    private static void OnGroups(QuerySnapshot snapshot)
    {
        List<Group> groups = new ArrayList<>();
        Map<String, String> confirmed = new LinkedHashMap<>();
        for (DocumentSnapshot document : snapshot.getDocuments())
        {
            Group group = Group.FromDocument(document);
            groups.add(group);
            // Only groups the cloud has confirmed are remembered, so a join the cloud refuses isn't later
            // mistaken for being removed.
            if (!document.getMetadata().hasPendingWrites())
                confirmed.put(group.Id, group.Name);
        }
        Collections.sort(groups, (a, b) -> a.CreatedAt != b.CreatedAt
                ? Long.compare(a.CreatedAt, b.CreatedAt) : a.Name.compareToIgnoreCase(b.Name));
        Groups.clear();
        for (Group group : groups)
            Groups.put(group.Id, group);

        if (!snapshot.getMetadata().isFromCache())
            OnGroupsConfirmed(confirmed);
        if (Loaded)
            ChoreSync.SyncGroups(GetSyncedGroupIds());
        NotifyChanged();
    }

    // The cloud said which groups the user is in: notice removals, make sure there's a group, and put
    // chores without a (known) group into the main group.
    private static void OnGroupsConfirmed(Map<String, String> confirmed)
    {
        SaveData saveData = SaveData.Instance();
        if (saveData == null)
            return;
        for (Map.Entry<String, String> known : saveData.GetKnownGroups().entrySet())
        {
            String groupId = known.getKey();
            if (Groups.containsKey(groupId) || LeavingGroupIds.contains(groupId))
                continue;
            OnRemovedFromGroup(groupId, known.getValue());
        }
        LeavingGroupIds.retainAll(Groups.keySet());
        saveData.SetKnownGroups(confirmed);
        Loaded = true;

        if (Groups.isEmpty())
        {
            CreateDefaultGroup();
            return;
        }
        MoveChoresWithoutGroup();
    }

    // Removed from a group (by its creator): remember a notice for the next screen and take the group's
    // chores off the phone (they stay in the group).
    private static void OnRemovedFromGroup(String groupId, String groupName)
    {
        Log.i(LOGTAG, "OnRemovedFromGroup: " + groupId);
        AppSettings.Initialize(AppContext);
        AppSettings.Instance().AddRemovedFromGroup(groupName == null ? "" : groupName);
        ChoreSync.StopGroup(groupId);
        SaveData.Instance().RemoveGroupChores(groupId);
        ChoreSync.NotifyChoresChanged();
    }

    // Chores that were never synced (signed out, or from before groups) or whose group isn't one of the
    // user's (e.g. kept after signing out of another account) go into the main group.
    private static void MoveChoresWithoutGroup()
    {
        String mainGroupId = GetMainGroupId();
        if (mainGroupId == null)
            return;
        List<Chore> chores = SaveData.Instance().GetChoreList();
        boolean moved = false;
        for (Chore chore : chores)
        {
            String groupId = chore.GetGroupId();
            if (groupId == null || (!Groups.containsKey(groupId) && !LeavingGroupIds.contains(groupId)))
            {
                chore.SetGroupId(mainGroupId);
                moved = true;
            }
        }
        if (moved)
            SaveData.Instance().SaveChoreList(chores);
    }

    // The first group (or a new one after leaving / being removed from the last): "name's group". Also
    // clears chores saved in the cloud before groups existed (users/{id}/chores).
    private static void CreateDefaultGroup()
    {
        if (CreatingDefaultGroup)
            return;
        CreatingDefaultGroup = true;
        String userId = AccountManager.GetUserId();
        CreateGroup(GetDefaultGroupName()).addOnCompleteListener(task -> {
            CreatingDefaultGroup = false;
            if (!task.isSuccessful())
            {
                Log.e(LOGTAG, "CreateDefaultGroup: failed", task.getException());
                return;
            }
            if (userId != null)
                DeleteCollection(UserDoc(userId).collection("chores"));
        });
    }

    // The groups whose chores are synced: all of them except ones being left.
    private static Set<String> GetSyncedGroupIds()
    {
        Set<String> ids = new HashSet<>(Groups.keySet());
        ids.removeAll(LeavingGroupIds);
        return ids;
    }

    private static void StopWatching()
    {
        if (GroupsRegistration != null)
            GroupsRegistration.remove();
        GroupsRegistration = null;
    }

    private static void NotifyChanged()
    {
        for (OnGroupChanged listener : new ArrayList<>(Listeners))
            listener.GroupChanged();
    }

    // Forgets the groups (after signing out or deleting the account). Stop ChoreSync too.
    public static void Clear()
    {
        StopWatching();
        Groups.clear();
        Loaded = false;
        LoadFailed = false;
        CreatingDefaultGroup = false;
        LeavingGroupIds.clear();
        // Another account may sign in next: its groups aren't these, so their absence isn't a removal.
        if (SaveData.Instance() != null)
            SaveData.Instance().SetKnownGroups(new LinkedHashMap<>());
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

    // Creates a group with the user as its only member; returns its id. The group list shows it right away
    // (the write is in the phone's cache); the task finishes once the cloud has it.
    private static Task<String> CreateGroup(String name)
    {
        return CreateGroup(Db().collection("groups").document(), name);
    }

    // Same, for a group whose id is needed before the cloud has it.
    private static Task<String> CreateGroup(DocumentReference group, String name)
    {
        String userId = AccountManager.GetUserId();
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
        return group.set(data).continueWith(task -> {
            if (!task.isSuccessful())
                throw task.getException();
            return group.getId();
        });
    }

    // --- Actions from the group screens. Callbacks run on the main thread. ---

    public static void CreateNewGroup(String name, OnGroupCreated onCreated)
    {
        CreateGroup(name).addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                Log.e(LOGTAG, "CreateNewGroup: failed", task.getException());
            onCreated.GroupCreated(task.isSuccessful() ? task.getResult() : null);
        });
    }

    public static void Rename(String groupId, String name, OnDone onDone)
    {
        Group group = Groups.get(groupId);
        if (group == null)
        {
            onDone.Done(false);
            return;
        }
        WriteBatch batch = Db().batch();
        batch.update(GroupDoc(group.Id), "name", name);
        if (group.InviteCode != null)
            batch.update(InviteDoc(group.InviteCode), "groupName", name);
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                Log.e(LOGTAG, "Rename: failed", task.getException());
            onDone.Done(task.isSuccessful());
        });
    }

    // Creator only: takes someone out of the group. They're told the next time they open the app.
    public static void RemoveMember(String groupId, String memberId, OnDone onDone)
    {
        Group group = Groups.get(groupId);
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
    public static void GetInviteCode(String groupId, OnInviteCode onCode)
    {
        Group group = Groups.get(groupId);
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

    // Joins the invite's group (the user stays in their other groups). Its chores arrive through ChoreSync.
    public static void Join(InviteInfo invite, OnDone onDone)
    {
        String userId = AccountManager.GetUserId();
        GroupDoc(invite.GroupId).update("members", FieldValue.arrayUnion(userId),
                        "memberNames." + userId, GetUserName(),
                        "joinCode", invite.Code)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Log.e(LOGTAG, "Join: failed", task.getException());
                    onDone.Done(task.isSuccessful());
                });
    }

    // Where Leave(keepChores = true) copies the group's chores: the main group, or (when leaving the main
    // group) the oldest other group. Null when this is the user's only group: then a new "name's group" is
    // made for them.
    public static Group GetLeaveCopyTarget(String leavingGroupId)
    {
        String mainGroupId = GetMainGroupId();
        if (mainGroupId != null && !mainGroupId.equals(leavingGroupId))
            return Groups.get(mainGroupId);
        for (Group group : Groups.values())
        {
            if (!group.Id.equals(leavingGroupId))
                return group;
        }
        return null;
    }

    // Leaves a group: its chores come off the phone; 'keepChores' first copies them into another group
    // (GetLeaveCopyTarget). The last member leaving deletes the group; otherwise the creator role passes on
    // to the member who joined earliest.
    public static void Leave(String groupId, boolean keepChores, OnDone onDone)
    {
        Group group = Groups.get(groupId);
        String userId = AccountManager.GetUserId();
        if (group == null || userId == null)
        {
            onDone.Done(false);
            return;
        }
        LeavingGroupIds.add(groupId);
        ChoreSync.StopGroup(groupId);
        List<Chore> groupChores = SaveData.Instance().RemoveGroupChores(groupId);
        if (keepChores && !groupChores.isEmpty())
        {
            Group target = GetLeaveCopyTarget(groupId);
            if (target != null)
                CopyChores(groupChores, target.Id);
            else
            {
                // The only group: the copies go into a new "name's group" (its id is known before the cloud
                // has it, so this works offline too).
                CreatingDefaultGroup = true;
                DocumentReference newGroup = Db().collection("groups").document();
                CreateGroup(newGroup, GetDefaultGroupName()).addOnCompleteListener(task -> {
                    CreatingDefaultGroup = false;
                    if (!task.isSuccessful())
                        Log.e(LOGTAG, "Leave: couldn't create a new group", task.getException());
                });
                CopyChores(groupChores, newGroup.getId());
            }
        }
        ChoreSync.NotifyChoresChanged();

        LeaveGroupInCloud(group, userId).addOnCompleteListener(task -> {
            if (!task.isSuccessful())
            {
                // Still in the group: sync it again (its chores come back).
                Log.e(LOGTAG, "Leave: failed", task.getException());
                LeavingGroupIds.remove(groupId);
                if (Loaded)
                    ChoreSync.SyncGroups(GetSyncedGroupIds());
            }
            onDone.Done(task.isSuccessful());
        });
    }

    // Copies chores into a group, with new ids (the originals stay in their group for its other members).
    private static void CopyChores(List<Chore> chores, String groupId)
    {
        List<Chore> all = SaveData.Instance().GetChoreList();
        for (Chore chore : chores)
            all.add(chore.CopyToGroup(groupId));
        SaveData.Instance().SaveChoreList(all);
        ChoreSync.NotifyChoresChanged();
    }

    // Takes the user out of a group in the cloud: the last member deletes it (with its chores and invite).
    private static Task<Void> LeaveGroupInCloud(Group group, String userId)
    {
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

    // Account deletion: leaves (or deletes) every group and deletes the user's old record. Stop ChoreSync first.
    public static Task<Void> DeleteUserData(String userId)
    {
        List<Group> groups = GetGroups();
        StopWatching();
        List<Task<Void>> leaving = new ArrayList<>();
        for (Group group : groups)
            leaving.add(LeaveGroupInCloud(group, userId));
        return Tasks.whenAll(leaving)
                .continueWithTask(task -> DeleteCollection(UserDoc(userId).collection("chores")))
                .continueWithTask(task -> UserDoc(userId).delete())
                .addOnCompleteListener(task -> Clear());
    }

    // Deletes every document in a collection (in batches of up to 500; the app can't delete a collection).
    private static Task<Void> DeleteCollection(CollectionReference collection)
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
