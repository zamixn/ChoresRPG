package com.zsoftware.choresrpg.cloud;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A group of people sharing one chore list (Firestore: groups/{groupId}). Every signed-in user is in
// exactly one group; it starts as their own "name's group".
public class Group {
    public final String Id;
    public final String Name;
    // The creator: can remove members. Passes to the next member when they leave.
    public final String OwnerId;
    // User ids, in the order they joined.
    public final List<String> Members;
    // User id -> name shown in the member list.
    public final Map<String, String> MemberNames;
    // Code of the group's invite link (invites/{code}); null until someone first shares it.
    public final String InviteCode;

    public Group(String id, String name, String ownerId, List<String> members, Map<String, String> memberNames, String inviteCode)
    {
        Id = id;
        Name = name;
        OwnerId = ownerId;
        Members = members;
        MemberNames = memberNames;
        InviteCode = inviteCode;
    }

    public boolean IsMember(String userId)
    {
        return Members.contains(userId);
    }

    public String GetMemberName(String userId)
    {
        String name = MemberNames.get(userId);
        return name == null || name.isEmpty() ? "?" : name;
    }

    // Who becomes the creator when 'leavingUserId' leaves: the creator stays, unless it's them, then the
    // member who joined earliest. Null when nobody is left.
    public String GetOwnerAfterLeaving(String leavingUserId)
    {
        if (!OwnerId.equals(leavingUserId))
            return OwnerId;
        for (String member : Members)
        {
            if (!member.equals(leavingUserId))
                return member;
        }
        return null;
    }

    public static Group FromDocument(DocumentSnapshot document)
    {
        List<String> members = new ArrayList<>();
        Object membersValue = document.get("members");
        if (membersValue instanceof List)
        {
            for (Object member : (List<?>) membersValue)
                members.add(String.valueOf(member));
        }
        Map<String, String> names = new HashMap<>();
        Object namesValue = document.get("memberNames");
        if (namesValue instanceof Map)
        {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) namesValue).entrySet())
                names.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        String name = document.getString("name");
        String owner = document.getString("ownerId");
        return new Group(document.getId(), name == null ? "" : name, owner == null ? "" : owner,
                members, names, document.getString("inviteCode"));
    }
}
