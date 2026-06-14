package com.yuvaraj.fitsphere.security;

import com.yuvaraj.fitsphere.domain.Role;

/** Authenticated principal derived from the access token (mirrors req.user). */
public record AppUser(String id, Role role) {
}
