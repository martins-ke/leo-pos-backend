package com.leo.pos.auth;

import java.util.List;
import com.leo.pos.staff.User;
import com.leo.pos.shop.ShopDto;

public record AuthResponse(User user, String token) {}