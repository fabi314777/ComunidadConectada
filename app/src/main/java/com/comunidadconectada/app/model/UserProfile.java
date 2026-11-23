package com.comunidadconectada.app.model;

import com.comunidadconectada.app.Constants;
import com.comunidadconectada.app.util.Identity;

public class UserProfile {
    private final String uid, name, unit, email, role, communityId;

    public UserProfile(String uid, String name, String unit, String email, String role, String communityId) {
        this.uid = uid; this.name = name; this.unit = unit; this.email = email;
        this.role = role; this.communityId = communityId;
    }
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getCommunityId() { return communityId; }
    public boolean isAdmin() { return Constants.ROLE_ADMIN.equals(role); }
    public String identity() { return Identity.format(name, unit); }
    public UserProfile withName(String newName) {
        return new UserProfile(uid, newName, unit, email, role, communityId);
    }
}
