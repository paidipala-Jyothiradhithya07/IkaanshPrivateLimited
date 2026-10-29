package com.ikaanshpl.finance.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikaanshpl.finance.controller.ApiException;
import com.ikaanshpl.finance.dto.Dtos.LoginRequest;
import com.ikaanshpl.finance.dto.Dtos.RegisterRequest;
import com.ikaanshpl.finance.dto.Dtos.TokenResponse;
import com.ikaanshpl.finance.dto.Dtos.UserResponse;
import com.ikaanshpl.finance.entity.Category;
import com.ikaanshpl.finance.entity.User;
import com.ikaanshpl.finance.repo.CategoryRepository;
import com.ikaanshpl.finance.repo.UserRepository;
import com.ikaanshpl.finance.security.JwtService;

import lombok.RequiredArgsConstructor;


@Service
public class AuthService {
	 private static final List<String> DEFAULT_CATEGORIES =
	            List.of("Salary", "Food", "Rent", "Transport", "Utilities", "Entertainment", "Other");

	    private final UserRepository users;
	    private final CategoryRepository categories;
	    private final PasswordEncoder encoder;
	    private final JwtService jwt;
	    private final RefreshTokenService refreshTokens;
	    
	    

	    public AuthService(UserRepository users, CategoryRepository categories, PasswordEncoder encoder, JwtService jwt,
				RefreshTokenService refreshTokens) {
			super();
			this.users = users;
			this.categories = categories;
			this.encoder = encoder;
			this.jwt = jwt;
			this.refreshTokens = refreshTokens;
		}

		@Transactional
	    public UserResponse register(RegisterRequest r) {
	        String email = r.email().trim().toLowerCase(Locale.ROOT);
	        if (users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
	        User u = new User();
	        u.setName(r.name().trim());
	        u.setEmail(email);
	        u.setPasswordHash(encoder.encode(r.password()));
	        users.save(u);
	        // Seed starter categories so CSV import works out of the box.
	        categories.saveAll(DEFAULT_CATEGORIES.stream().map(n -> new Category(u.getId(), n)).toList());
	        return UserResponse.from(u);
	    }

	    @Transactional
	    public TokenResponse login(LoginRequest r) {
	        User u = users.findByEmailIgnoreCase(r.email().trim())
	                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
	                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
	        return tokensFor(u, refreshTokens.startSession(u.getId()));
	    }

	    @Transactional(noRollbackFor = ApiException.class)
	    public TokenResponse refresh(String rawRefreshToken) {
	        RefreshTokenService.Session s = refreshTokens.rotate(rawRefreshToken);
	        User u = users.findById(s.userId()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
	        return tokensFor(u, s);
	    }

	    public void logout(String rawRefreshToken) {
	        refreshTokens.revokeSession(rawRefreshToken);
	    }

	    public void logoutAll(Long userId) {
	        refreshTokens.revokeAll(userId);
	    }

	    private TokenResponse tokensFor(User u, RefreshTokenService.Session s) {
	        return new TokenResponse(jwt.generateAccessToken(u, s.familyId()), s.rawToken(), "Bearer", jwt.getAccessTtlSeconds());
	    }
}
